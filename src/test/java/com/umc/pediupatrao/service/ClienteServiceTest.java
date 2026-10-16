package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock ClienteRepository clienteRepository;
    @Mock AuditoriaService auditoriaService;
    @InjectMocks ClienteService clienteService;
    @Captor ArgumentCaptor<Map<String, AlteracaoCampo>> camposAuditoria;

    @Test
    void registraAlteracaoDoEnderecoLegado() {
        Cliente existente = cliente("Rua antiga");
        Cliente recebido = cliente("Avenida nova");
        recebido.setId("cliente-1");
        when(clienteRepository.findById("cliente-1")).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clienteService.salvar(recebido);

        verify(auditoriaService).registrar(eq("ALTERACAO"), eq("CLIENTE"), eq("cliente-1"),
                camposAuditoria.capture(), eq(null));
        assertThat(camposAuditoria.getValue()).containsOnlyKeys("endereco");
        assertThat(camposAuditoria.getValue().get("endereco").getValorAnterior()).isEqualTo("Rua antiga");
        assertThat(camposAuditoria.getValue().get("endereco").getValorNovo()).isEqualTo("Avenida nova");
    }

    private Cliente cliente(String endereco) {
        Cliente cliente = new Cliente();
        cliente.setId("cliente-1");
        cliente.setNome("Maria");
        cliente.setTelefone("11999999999");
        cliente.setCep("01000000");
        cliente.setLogradouro("Rua Central");
        cliente.setNumero("10");
        cliente.setComplemento("Casa");
        cliente.setBairro("Centro");
        cliente.setCidade("São Paulo");
        cliente.setEstado("SP");
        cliente.setEndereco(endereco);
        return cliente;
    }
}
