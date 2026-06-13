package com.elian.wallet.services.impl;

import com.elian.wallet.dto.CreditApprovalRequest;
import com.elian.wallet.dto.CreditRequest;
import com.elian.wallet.dto.CreditResponse;
import com.elian.wallet.dto.CuotaResponse;
import com.elian.wallet.entity.Credito;
import com.elian.wallet.entity.CuotaCredito;
import com.elian.wallet.exception.BusinessException;
import com.elian.wallet.exception.NotFoundException;
import com.elian.wallet.mapper.CreditMapper;
import com.elian.wallet.repository.CatEstadoCreditoRepository;
import com.elian.wallet.repository.CatEstadoCuotaRepository;
import com.elian.wallet.repository.CreditoRepository;
import com.elian.wallet.repository.CuotaCreditoRepository;
import com.elian.wallet.repository.WalletRepository;
import com.elian.wallet.security.CurrentUserService;
import com.elian.wallet.services.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {
    private final CreditoRepository creditoRepository;
    private final CuotaCreditoRepository cuotaCreditoRepository;
    private final WalletRepository walletRepository;
    private final CatEstadoCreditoRepository estadoCreditoRepository;
    private final CatEstadoCuotaRepository estadoCuotaRepository;
    private final CurrentUserService currentUserService;
    private final CreditMapper creditMapper;

    @Override
    @Transactional
    public CreditResponse requestCredit(CreditRequest request) {
        var user = currentUserService.get();
        var estado = estadoCreditoRepository.findByCodigo("SOLICITADO")
                .orElseThrow(() -> new BusinessException("Estado SOLICITADO no encontrado"));

        var wallet = request.walletDesembolsoId() == null
                ? null
                : walletRepository.findByIdAndActivaTrue(request.walletDesembolsoId())
                .orElseThrow(() -> new NotFoundException("Wallet de desembolso no encontrada"));

        if (wallet != null && !wallet.getUsuario().getId().equals(user.getId())) {
            throw new NotFoundException("Wallet de desembolso no encontrada");
        }

        Credito credito = new Credito();
        credito.setId(UUID.randomUUID());
        credito.setUsuario(user);
        credito.setWalletDesembolso(wallet);
        credito.setEstadoCredito(estado);
        credito.setMontoSolicitado(request.montoSolicitado());
        credito.setTasaInteresAnual(request.tasaInteresAnual());
        credito.setPlazoMeses(request.plazoMeses());
        credito.setFechaSolicitud(OffsetDateTime.now());

        return creditMapper.toResponse(creditoRepository.save(credito));
    }

    @Override
    public List<CreditResponse> findMyCredits() {
        return creditoRepository.findByUsuarioIdOrderByFechaSolicitudDesc(currentUserService.get().getId())
                .stream()
                .map(creditMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CreditResponse approve(UUID creditoId, CreditApprovalRequest request) {
        Credito credito = creditoRepository.findById(creditoId)
                .orElseThrow(() -> new NotFoundException("Credito no encontrado"));
        if (request.montoAprobado().compareTo(credito.getMontoSolicitado()) > 0) {
            throw new BusinessException("El monto aprobado no puede ser mayor al monto solicitado");
        }

        var estadoAprobado = estadoCreditoRepository.findByCodigo("APROBADO")
                .orElseThrow(() -> new BusinessException("Estado APROBADO no encontrado"));
        credito.setEstadoCredito(estadoAprobado);
        credito.setMontoAprobado(request.montoAprobado());
        credito.setFechaAprobacion(OffsetDateTime.now());
        Credito saved = creditoRepository.save(credito);
        generatePaymentPlan(saved);

        return creditMapper.toResponse(saved);
    }

    @Override
    public List<CuotaResponse> findPaymentPlan(UUID creditoId) {
        Credito credito = creditoRepository.findById(creditoId)
                .orElseThrow(() -> new NotFoundException("Credito no encontrado"));
        var user = currentUserService.get();
        if (!credito.getUsuario().getId().equals(user.getId()) && !"ADMIN".equals(user.getRol())) {
            throw new NotFoundException("Credito no encontrado");
        }
        return cuotaCreditoRepository.findByCreditoIdOrderByNumeroCuota(creditoId)
                .stream()
                .map(creditMapper::toCuotaResponse)
                .toList();
    }

    private void generatePaymentPlan(Credito credito) {
        if (cuotaCreditoRepository.existsByCreditoId(credito.getId())) {
            return;
        }
        var estadoPendiente = estadoCuotaRepository.findByCodigo("PENDIENTE")
                .orElseThrow(() -> new BusinessException("Estado PENDIENTE no encontrado"));

        BigDecimal tasaMensual = credito.getTasaInteresAnual()
                .divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        BigDecimal cuotaTotal;
        if (tasaMensual.compareTo(BigDecimal.ZERO) == 0) {
            cuotaTotal = credito.getMontoAprobado().divide(BigDecimal.valueOf(credito.getPlazoMeses()), 4, RoundingMode.HALF_UP);
        } else {
            double rate = tasaMensual.doubleValue();
            double factor = Math.pow(1 + rate, credito.getPlazoMeses());
            cuotaTotal = credito.getMontoAprobado()
                    .multiply(BigDecimal.valueOf((rate * factor) / (factor - 1)))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        BigDecimal saldo = credito.getMontoAprobado();
        for (int i = 1; i <= credito.getPlazoMeses(); i++) {
            BigDecimal interes = saldo.multiply(tasaMensual).setScale(4, RoundingMode.HALF_UP);
            BigDecimal capital = cuotaTotal.subtract(interes).setScale(4, RoundingMode.HALF_UP);
            if (i == credito.getPlazoMeses()) {
                capital = saldo;
                cuotaTotal = capital.add(interes).setScale(4, RoundingMode.HALF_UP);
            }

            CuotaCredito cuota = new CuotaCredito();
            cuota.setId(UUID.randomUUID());
            cuota.setCredito(credito);
            cuota.setEstadoCuota(estadoPendiente);
            cuota.setNumeroCuota(i);
            cuota.setFechaVencimiento(credito.getFechaAprobacion().toLocalDate().plusMonths(i));
            cuota.setMontoCapital(capital);
            cuota.setMontoInteres(interes);
            cuota.setMontoTotal(cuotaTotal);
            cuota.setMontoPagado(BigDecimal.ZERO);
            cuotaCreditoRepository.save(cuota);
            saldo = saldo.subtract(capital).setScale(4, RoundingMode.HALF_UP);
        }
    }
}
