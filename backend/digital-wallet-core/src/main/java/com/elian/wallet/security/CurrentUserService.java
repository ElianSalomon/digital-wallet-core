package com.elian.wallet.security;

import com.elian.wallet.entity.Usuario;
import com.elian.wallet.exception.NotFoundException;
import com.elian.wallet.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UsuarioRepository usuarioRepository;

    public Usuario get() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Usuario autenticado no encontrado"));
    }
}
