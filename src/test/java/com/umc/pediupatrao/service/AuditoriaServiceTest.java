package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Auditoria;
import com.umc.pediupatrao.entity.PerfilUsuario;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock AuditoriaRepository auditoriaRepository;
    @InjectMocks AuditoriaService auditoriaService;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void persisteRegistroComIdentificacaoEValoresAlterados() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("atendente", "n/a",
                        java.util.List.of(new SimpleGrantedAuthority("ROLE_ATENDENTE"))));
        when(auditoriaRepository.save(any(Auditoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditoriaService.registrar("ALTERACAO", "CLIENTE", "cliente-1",
                Map.of("telefone", new AlteracaoCampo("1111", "2222")), null);

        ArgumentCaptor<Auditoria> registro = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(registro.capture());
        assertThat(registro.getValue().getUsuario()).isEqualTo("atendente");
        assertThat(registro.getValue().getPerfil()).isEqualTo(PerfilUsuario.ATENDENTE);
        assertThat(registro.getValue().getDataHora()).isNotNull();
        assertThat(registro.getValue().getOperacao()).isEqualTo("ALTERACAO");
        assertThat(registro.getValue().getEntidade()).isEqualTo("CLIENTE");
        assertThat(registro.getValue().getEntidadeId()).isEqualTo("cliente-1");
        assertThat(registro.getValue().getCampos().get("telefone").getValorAnterior()).isEqualTo("1111");
        assertThat(registro.getValue().getCampos().get("telefone").getValorNovo()).isEqualTo("2222");
    }
}
