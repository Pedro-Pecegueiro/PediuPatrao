package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardResponse;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class DashboardServiceTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final DashboardService dashboardService = new DashboardService(mongoTemplate);
    private List<Pedido> pedidos;

    @BeforeEach
    void configurarPedidos() {
        pedidos = List.of(
                pedido("1", "2026-11-02T10:00:00Z", StatusPedido.FINALIZADO,
                        "Pizza", 2, "50.00", "100.00"),
                pedido("2", "2026-11-02T14:00:00Z", StatusPedido.PRONTO,
                        "Pizza", 1, "50.00", "50.00"),
                pedido("3", "2026-11-03T10:00:00Z", StatusPedido.CANCELADO,
                        "Refrigerante", 3, "8.00", "24.00"),
                pedido("4", "2026-11-04T10:00:00Z", StatusPedido.RECEBIDO,
                        "Pizza", 1, "50.00", "50.00"));
        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class),
                org.mockito.ArgumentMatchers.eq(Pedido.class))).thenReturn(pedidos);
    }

    @Test
    void calculaIndicadoresSerieStatusERankingIgnorandoCancelamentosNasVendas() {
        DashboardResponse resposta = dashboardService.consultar(
                LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-04"), null);

        assertThat(resposta.totalPedidos()).isEqualTo(4);
        assertThat(resposta.pedidosComVenda()).isEqualTo(3);
        assertThat(resposta.totalVendas()).isEqualByComparingTo("200.00");
        assertThat(resposta.ticketMedio()).isEqualByComparingTo("66.67");
        assertThat(resposta.vendasPorDia()).extracting(DashboardResponse.VendaPorDia::pedidos)
                .containsExactly(2L, 0L, 1L);
        assertThat(resposta.pedidosPorStatus())
                .filteredOn(item -> item.status() == StatusPedido.CANCELADO)
                .singleElement().extracting(DashboardResponse.PedidosPorStatus::quantidade)
                .isEqualTo(1L);
        assertThat(resposta.produtosMaisVendidos()).first()
                .extracting(DashboardResponse.ProdutoMaisVendido::nome).isEqualTo("Pizza");
        assertThat(resposta.produtosMaisVendidos()).first()
                .extracting(DashboardResponse.ProdutoMaisVendido::quantidade).isEqualTo(4L);
    }

    @Test
    void aplicaFiltroDeStatusNaConsulta() {
        DashboardResponse resposta = dashboardService.consultar(
                LocalDate.parse("2026-11-02"), LocalDate.parse("2026-11-04"), StatusPedido.CANCELADO);

        assertThat(resposta.totalPedidos()).isEqualTo(1);
        assertThat(resposta.pedidosComVenda()).isZero();
        assertThat(resposta.totalVendas()).isEqualByComparingTo("0.00");
        assertThat(resposta.produtosMaisVendidos()).isEmpty();
    }

    @Test
    void rejeitaPeriodoInvertidoOuMaiorQueUmAno() {
        assertThatThrownBy(() -> dashboardService.consultar(
                LocalDate.parse("2026-11-05"), LocalDate.parse("2026-11-04"), null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("data inicial");

        assertThatThrownBy(() -> dashboardService.consultar(
                LocalDate.parse("2025-01-01"), LocalDate.parse("2026-01-02"), null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("366 dias");
    }

    private Pedido pedido(String id, String entrada, StatusPedido status, String produto,
                          int quantidade, String preco, String total) {
        ItemPedido item = new ItemPedido();
        item.setProdutoId(produto.toLowerCase());
        item.setNomeProduto(produto);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(new BigDecimal(preco));
        item.setSubtotal(new BigDecimal(preco).multiply(BigDecimal.valueOf(quantidade)));

        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setDataEntrada(Instant.parse(entrada));
        pedido.setStatus(status);
        pedido.setValorTotal(new BigDecimal(total));
        pedido.setItens(List.of(item));
        return pedido;
    }
}
