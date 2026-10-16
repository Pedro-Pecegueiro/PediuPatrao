package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.dto.UsuarioUpdateRequest;
import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Usuario criar(UsuarioRequest request) {
        String username = normalizarUsername(request.username());
        if (usuarioRepository.existsByUsername(username)) {
            throw new RegraNegocioException("Já existe um usuário com esse nome.");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPassword(passwordEncoder.encode(request.password()));
        usuario.setRole(request.role());
        usuario.setAtivo(request.ativo() == null || request.ativo());
        return usuarioRepository.save(usuario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Usuario atualizar(String id, UsuarioUpdateRequest request) {
        Usuario existente = buscarPorId(id);
        String username = normalizarUsername(request.username());
        usuarioRepository.findByUsername(username)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> { throw new RegraNegocioException("Já existe um usuário com esse nome."); });

        validarUltimoAdmin(existente, request.role(), request.ativo() == null || request.ativo());
        existente.setUsername(username);
        existente.setRole(request.role());
        existente.setAtivo(request.ativo() == null || request.ativo());
        if (request.password() != null && !request.password().isBlank()) {
            existente.setPassword(passwordEncoder.encode(request.password()));
        }
        return usuarioRepository.save(existente);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(String id) {
        Usuario existente = buscarPorId(id);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && existente.getUsername().equalsIgnoreCase(authentication.getName())) {
            throw new RegraNegocioException("Não é permitido excluir o próprio usuário autenticado.");
        }
        validarUltimoAdmin(existente, existente.getRole(), false);
        usuarioRepository.delete(existente);
    }

    private void validarUltimoAdmin(Usuario existente, PerfilUsuario novoPerfil, boolean ativo) {
        if (existente.getRole() == PerfilUsuario.ADMIN && existente.isAtivo()
                && (novoPerfil != PerfilUsuario.ADMIN || !ativo)
                && usuarioRepository.countByRoleAndAtivoTrue(PerfilUsuario.ADMIN) <= 1) {
            throw new RegraNegocioException("O último administrador ativo não pode perder o perfil ou ser desativado.");
        }
    }

    private String normalizarUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
