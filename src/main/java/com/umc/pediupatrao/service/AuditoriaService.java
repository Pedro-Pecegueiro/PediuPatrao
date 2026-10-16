package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class AuditoriaService {

    private static final Set<String> CAMPOS_SENSIVEIS = Set.of("password", "senha", "token", "credential", "secret");
    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public Auditoria registrar(String operacao, String entidade, String entidadeId,
                               Map<String, AlteracaoCampo> campos, String justificativa) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Auditoria auditoria = new Auditoria();
        auditoria.setUsuario(authentication == null ? "SISTEMA" : authentication.getName());
        auditoria.setPerfil(perfil(authentication));
        auditoria.setDataHora(Instant.now());
        auditoria.setOperacao(operacao);
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setCampos(filtrarCamposAuditaveis(campos));
        auditoria.setJustificativa(justificativa == null || justificativa.isBlank() ? null : justificativa.trim());
        return auditoriaRepository.save(auditoria);
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public List<Auditoria> listar() {
        return auditoriaRepository.findAllByOrderByDataHoraDesc();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public Auditoria buscar(String id) {
        return auditoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Registro de auditoria não encontrado."));
    }

    private Map<String, AlteracaoCampo> filtrarCamposAuditaveis(Map<String, AlteracaoCampo> campos) {
        Map<String, AlteracaoCampo> camposAuditaveis = new LinkedHashMap<>();
        if (campos == null) {
            return camposAuditaveis;
        }
        campos.forEach((campo, alteracao) -> {
            String normalizado = campo.toLowerCase(Locale.ROOT);
            if (CAMPOS_SENSIVEIS.stream().noneMatch(normalizado::contains)) {
                camposAuditaveis.put(campo, alteracao);
            }
        });
        return camposAuditaveis;
    }

    private PerfilUsuario perfil(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(autoridade -> autoridade.startsWith("ROLE_"))
                .map(autoridade -> autoridade.substring("ROLE_".length()))
                .map(this::perfilOuNulo)
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
    }

    private PerfilUsuario perfilOuNulo(String nome) {
        try {
            return PerfilUsuario.valueOf(nome);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
