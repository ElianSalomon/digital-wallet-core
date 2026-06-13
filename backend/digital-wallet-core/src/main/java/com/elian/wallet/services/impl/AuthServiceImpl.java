package com.elian.wallet.services.impl;

import com.elian.wallet.dto.AuthResponse;
import com.elian.wallet.dto.LoginRequest;
import com.elian.wallet.dto.RegisterRequest;
import com.elian.wallet.entity.PerfilCrediticio;
import com.elian.wallet.entity.Usuario;
import com.elian.wallet.exception.BusinessException;
import com.elian.wallet.repository.CatEstadoCrediticioRepository;
import com.elian.wallet.repository.PerfilCrediticioRepository;
import com.elian.wallet.repository.UsuarioRepository;
import com.elian.wallet.security.JwtService;
import com.elian.wallet.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UsuarioRepository usuarioRepository;
    private final PerfilCrediticioRepository perfilCrediticioRepository;
    private final CatEstadoCrediticioRepository estadoCrediticioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new BusinessException("El email ya esta registrado");
        }
        if (usuarioRepository.existsByDocumentoIdentidad(request.documentoIdentidad())) {
            throw new BusinessException("El documento de identidad ya esta registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNombre(request.nombre());
        usuario.setApellido(request.apellido());
        usuario.setEmail(request.email().toLowerCase());
        usuario.setTelefono(request.telefono());
        usuario.setDocumentoIdentidad(request.documentoIdentidad());
        usuario.setFechaNacimiento(request.fechaNacimiento());
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol("USER");
        usuario.setActivo(true);
        usuario.setCreadoEn(OffsetDateTime.now());
        usuario.setActualizadoEn(OffsetDateTime.now());
        usuarioRepository.save(usuario);

        var estado = estadoCrediticioRepository.findByCodigo("SIN_HISTORIAL")
                .orElseThrow(() -> new BusinessException("Catalogo SIN_HISTORIAL no encontrado"));
        PerfilCrediticio perfil = new PerfilCrediticio();
        perfil.setId(UUID.randomUUID());
        perfil.setUsuario(usuario);
        perfil.setEstadoCrediticio(estado);
        perfil.setScoreCrediticio(0);
        perfil.setIngresoMensual(BigDecimal.ZERO);
        perfil.setLimiteCredito(BigDecimal.ZERO);
        perfil.setDeudaActual(BigDecimal.ZERO);
        perfil.setEvaluadoEn(OffsetDateTime.now());
        perfilCrediticioRepository.save(perfil);

        return buildResponse(usuario);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.email().toLowerCase(), request.password()));
        Usuario usuario = usuarioRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new BusinessException("Credenciales invalidas"));
        return buildResponse(usuario);
    }

    private AuthResponse buildResponse(Usuario usuario) {
        return new AuthResponse(jwtService.generateToken(usuario), "Bearer", usuario.getId(), usuario.getEmail(), usuario.getRol());
    }
}
