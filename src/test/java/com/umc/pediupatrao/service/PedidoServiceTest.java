package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock PedidoRepository pedidoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock AuditoriaService auditoriaService;
    @InjectMocks PedidoService pedidoService;
    @Captor org.mockito.ArgumentCaptor<Map<String, AlteracaoCampo>> camposAuditoria;

    @BeforeEach
    void autenticar() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("atendente", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_ATENDENTE"))));
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void calculaValoresNoServidorERegistraEntrada() {
        Cliente cliente = new Cliente();
        cliente.setId("c1");
        cliente.setNome("Maria");
        Produto produto = new Produto();
        produto.setId("p1");
        produto.setNome("Pizza");
        produto.setPreco(new BigDecimal("30.00"));
        produto.setAtivo(true);

        when(clienteRepository.findById("c1")).thenReturn(Optional.of(cliente));
        when(produtoRepository.findById("p1")).thenReturn(Optional.of(produto));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido salvo = invocation.getArgument(0);
            salvo.setId("pedido-1");
            return salvo;
        });

        Pedido pedido = pedidoService.criarPedido(
                new PedidoRequest("c1", List.of(new ItemPedidoRequest("p1", 2))));

        assertThat(pedido.getSubtotal()).isEqualByComparingTo("60.00");
        assertThat(pedido.getValorTotal()).isEqualByComparingTo("60.00");
        assertThat(pedido.getStatus()).isEqualTo(StatusPedido.RECEBIDO);
        assertThat(pedido.getResponsavelRegistro()).isEqualTo("atendente");
        assertThat(pedido.getDataEntrada()).isNotNull();
        verify(auditoriaService).registrar(eq("INCLUSAO"), eq("PEDIDO"), eq("pedido-1"),
                camposAuditoria.capture(), org.mockito.ArgumentMatchers.isNull());
        assertThat(camposAuditoria.getValue()).containsKeys("responsavelRegistro", "dataEntrada", "valorDesconto");
        assertThat(camposAuditoria.getValue().get("responsavelRegistro").getValorNovo()).isEqualTo("atendente");
        assertThat(camposAuditoria.getValue().get("dataEntrada").getValorNovo()).isNotBlank();
        assertThat(camposAuditoria.getValue().get("valorDesconto").getValorNovo()).isEqualTo("0.00");
    }

    @Test
    void impedePularEtapasDoFluxo() {
        Pedido pedido = pedidoBase(StatusPedido.RECEBIDO);
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.atualizarStatus("1", StatusPedido.FINALIZADO))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Transição inválida");
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void registraResponsavelEHorarioNaSaida() {
        Pedido pedido = pedidoBase(StatusPedido.PRONTO);
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido atualizado = pedidoService.atualizarStatus("1", StatusPedido.SAIU_PARA_ENTREGA);

        assertThat(atualizado.getDataSaida()).isNotNull();
        assertThat(atualizado.getResponsavelSaida()).isEqualTo("atendente");
    }

    @Test
    void limitaDescontoAVintePorCento() {
        Pedido pedido = pedidoBase(StatusPedido.RECEBIDO);
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.aplicarDesconto("1", new BigDecimal("20.01")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("20%");
    }

    @Test
    void registraValorAnteriorENovoDoDescontoNaAuditoria() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("gerente", "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_GERENTE"))));
        Pedido pedido = pedidoBase(StatusPedido.RECEBIDO);
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        pedidoService.aplicarDesconto("1", new BigDecimal("10"));

        verify(auditoriaService).registrar(eq("APLICACAO_DESCONTO"), eq("PEDIDO"), eq("1"),
                camposAuditoria.capture(), org.mockito.ArgumentMatchers.isNull());
        assertThat(camposAuditoria.getValue().get("valorDesconto").getValorAnterior()).isEqualTo("0");
        assertThat(camposAuditoria.getValue().get("valorDesconto").getValorNovo()).isEqualTo("10.00");
    }

    @Test
    void bloqueiaCancelamentoDepoisDaSaida() {
        Pedido pedido = pedidoBase(StatusPedido.SAIU_PARA_ENTREGA);
        when(pedidoRepository.findById("1")).thenReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoService.cancelar("1", "Cliente desistiu"))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pode ser cancelado");
    }

    private Pedido pedidoBase(StatusPedido status) {
        Pedido pedido = new Pedido();
        pedido.setId("1");
        pedido.setStatus(status);
        pedido.setSubtotal(new BigDecimal("100.00"));
        pedido.setValorTotal(new BigDecimal("100.00"));
        pedido.setDescontoPercentual(BigDecimal.ZERO);
        pedido.setValorDesconto(BigDecimal.ZERO);
        return pedido;
    }
}
