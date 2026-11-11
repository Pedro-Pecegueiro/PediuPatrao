package com.umc.pediupatrao.dto;

import com.umc.pediupatrao.entity.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardResponse(
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusPedido statusFiltrado,
        long totalPedidos,
        long pedidosComVenda,
        BigDecimal totalVendas,
        BigDecimal ticketMedio,
        List<VendaPorDia> vendasPorDia,
        List<PedidosPorStatus> pedidosPorStatus,
        List<ProdutoMaisVendido> produtosMaisVendidos) {

    public record VendaPorDia(LocalDate data, long pedidos, BigDecimal vendas) { }

    public record PedidosPorStatus(StatusPedido status, String descricao, long quantidade) { }

    public record ProdutoMaisVendido(String produtoId, String nome, long quantidade,
                                     BigDecimal vendas) { }
}
