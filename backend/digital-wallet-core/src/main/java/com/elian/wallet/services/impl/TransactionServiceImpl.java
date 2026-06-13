package com.elian.wallet.services.impl;

import com.elian.wallet.dto.TransactionRequest;
import com.elian.wallet.dto.TransactionResponse;
import com.elian.wallet.entity.MovimientoWallet;
import com.elian.wallet.entity.Transaccion;
import com.elian.wallet.entity.Usuario;
import com.elian.wallet.entity.Wallet;
import com.elian.wallet.exception.BusinessException;
import com.elian.wallet.exception.NotFoundException;
import com.elian.wallet.mapper.TransactionMapper;
import com.elian.wallet.repository.CatEstadoTransaccionRepository;
import com.elian.wallet.repository.CatTipoTransaccionRepository;
import com.elian.wallet.repository.MovimientoWalletRepository;
import com.elian.wallet.repository.TransaccionRepository;
import com.elian.wallet.repository.WalletRepository;
import com.elian.wallet.security.CurrentUserService;
import com.elian.wallet.services.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final WalletRepository walletRepository;
    private final TransaccionRepository transaccionRepository;
    private final MovimientoWalletRepository movimientoWalletRepository;
    private final CatTipoTransaccionRepository tipoTransaccionRepository;
    private final CatEstadoTransaccionRepository estadoTransaccionRepository;
    private final CurrentUserService currentUserService;
    private final TransactionMapper transactionMapper;

    @Override
    @Transactional
    public TransactionResponse deposit(TransactionRequest request) {
        if (request.walletDestinoId() == null || request.walletOrigenId() != null) {
            throw new BusinessException("El deposito requiere solo wallet destino");
        }
        return process("DEPOSITO", null, request.walletDestinoId(), request);
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(TransactionRequest request) {
        if (request.walletOrigenId() == null || request.walletDestinoId() != null) {
            throw new BusinessException("El retiro requiere solo wallet origen");
        }
        return process("RETIRO", request.walletOrigenId(), null, request);
    }

    @Override
    @Transactional
    public TransactionResponse transfer(TransactionRequest request) {
        if (request.walletOrigenId() == null || request.walletDestinoId() == null) {
            throw new BusinessException("La transferencia requiere wallet origen y wallet destino");
        }
        if (request.walletOrigenId().equals(request.walletDestinoId())) {
            throw new BusinessException("La wallet origen y destino no pueden ser iguales");
        }
        return process("TRANSFERENCIA", request.walletOrigenId(), request.walletDestinoId(), request);
    }

    private TransactionResponse process(String tipoCodigo, UUID origenId, UUID destinoId, TransactionRequest request) {
        var existing = transaccionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return transactionMapper.toResponse(existing.get());
        }

        Usuario user = currentUserService.get();
        Wallet origen = null;
        Wallet destino = null;

        List<UUID> ids = Arrays.asList(origenId, destinoId).stream()
                .filter(id -> id != null)
                .sorted(Comparator.naturalOrder())
                .toList();
        for (UUID id : ids) {
            Wallet locked = walletRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new NotFoundException("Wallet no encontrada"));
            if (origenId != null && locked.getId().equals(origenId)) {
                origen = locked;
            }
            if (destinoId != null && locked.getId().equals(destinoId)) {
                destino = locked;
            }
        }

        if (origen != null) {
            validateOwnedAndActive(origen, user);
            if (origen.getSaldo().compareTo(request.monto()) < 0) {
                throw new BusinessException("Fondos insuficientes");
            }
        }
        if (destino != null) {
            validateActive(destino);
        }
        if (origen != null && destino != null && !origen.getMoneda().equals(destino.getMoneda())) {
            throw new BusinessException("Las wallets deben manejar la misma moneda");
        }

        var tipo = tipoTransaccionRepository.findByCodigo(tipoCodigo)
                .orElseThrow(() -> new BusinessException("Tipo de transaccion no encontrado"));
        var estado = estadoTransaccionRepository.findByCodigo("COMPLETADA")
                .orElseThrow(() -> new BusinessException("Estado de transaccion no encontrado"));

        Transaccion transaccion = new Transaccion();
        transaccion.setId(UUID.randomUUID());
        transaccion.setIdempotencyKey(request.idempotencyKey());
        transaccion.setTipoTransaccion(tipo);
        transaccion.setEstadoTransaccion(estado);
        transaccion.setWalletOrigen(origen);
        transaccion.setWalletDestino(destino);
        transaccion.setMonto(request.monto());
        transaccion.setMoneda(origen != null ? origen.getMoneda() : destino.getMoneda());
        transaccion.setDescripcion(request.descripcion());
        transaccion.setCreadaPor(user);
        transaccion.setCreadaEn(OffsetDateTime.now());
        transaccionRepository.save(transaccion);

        if (origen != null) {
            BigDecimal anterior = origen.getSaldo();
            origen.setSaldo(origen.getSaldo().subtract(request.monto()));
            origen.setActualizadaEn(OffsetDateTime.now());
            walletRepository.save(origen);
            saveMovement(transaccion, origen, "D", request.monto(), anterior, origen.getSaldo());
        }
        if (destino != null) {
            BigDecimal anterior = destino.getSaldo();
            destino.setSaldo(destino.getSaldo().add(request.monto()));
            destino.setActualizadaEn(OffsetDateTime.now());
            walletRepository.save(destino);
            saveMovement(transaccion, destino, "C", request.monto(), anterior, destino.getSaldo());
        }

        return transactionMapper.toResponse(transaccion);
    }

    private void saveMovement(Transaccion transaccion, Wallet wallet, String naturaleza, BigDecimal monto, BigDecimal anterior, BigDecimal posterior) {
        MovimientoWallet movimiento = new MovimientoWallet();
        movimiento.setId(UUID.randomUUID());
        movimiento.setTransaccion(transaccion);
        movimiento.setWallet(wallet);
        movimiento.setNaturaleza(naturaleza);
        movimiento.setMonto(monto);
        movimiento.setSaldoAnterior(anterior);
        movimiento.setSaldoPosterior(posterior);
        movimiento.setCreadoEn(OffsetDateTime.now());
        movimientoWalletRepository.save(movimiento);
    }

    private void validateOwnedAndActive(Wallet wallet, Usuario user) {
        validateActive(wallet);
        if (!wallet.getUsuario().getId().equals(user.getId())) {
            throw new NotFoundException("Wallet no encontrada");
        }
    }

    private void validateActive(Wallet wallet) {
        if (!Boolean.TRUE.equals(wallet.getActiva())) {
            throw new BusinessException("Wallet inactiva");
        }
    }
}
