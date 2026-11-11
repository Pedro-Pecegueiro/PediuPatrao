package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardResponse;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RegraNegocioException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class DashboardService {

    private static final int LIMITE_DIAS = 366;
    private static final int LIMITE_PRODUTOS = 5;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final MongoTemplate mongoTemplate;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public DashboardService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE')")
    public DashboardResponse consultar(LocalDate dataInicio, LocalDate dataFim, StatusPedido status) {
        LocalDate hoje = LocalDate.now(zoneId);
        LocalDate inicio = dataInicio == null ? hoje.minusDays(29) : dataInicio;
        LocalDate fim = dataFim == null ? hoje : dataFim;

        validarPeriodo(inicio, fim);

        Instant inicioInclusivo = inicio.atStartOfDay(zoneId).toInstant();
        Instant fimExclusivo = fim.plusDays(1).atStartOfDay(zoneId).toInstant();
        Criteria criterio = Criteria.where("dataEntrada").gte(inicioInclusivo).lt(fimExclusivo);
        if (status != null) {
            criterio.and("status").is(status);
        }
        List<Pedido> pedidos = mongoTemplate.find(Query.query(criterio), Pedido.class).stream()
                .filter(pedido -> status == null || pedido.getStatus() == status)
                .toList();

        long totalPedidos = pedidos.size();
        long pedidosComVenda = pedidos.stream().filter(this::geraVenda).count();
        BigDecimal totalVendas = pedidos.stream()
                .filter(this::geraVenda)
                .map(this::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal ticketMedio = pedidosComVenda == 0
                ? ZERO
                : totalVendas.divide(BigDecimal.valueOf(pedidosComVenda), 2, RoundingMode.HALF_UP);

        return new DashboardResponse(inicio, fim, status, totalPedidos, pedidosComVenda,
                totalVendas, ticketMedio, montarVendasPorDia(inicio, fim, pedidos),
                montarPedidosPorStatus(pedidos), montarRanking(pedidos));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data inicial deve ser anterior ou igual à data final.");
        }
        if (inicio.plusDays(LIMITE_DIAS - 1L).isBefore(fim)) {
            throw new RegraNegocioException("O período consultado não pode exceder 366 dias.");
        }
    }

    private boolean geraVenda(Pedido pedido) {
        return pedido.getStatus() != StatusPedido.CANCELADO;
    }

    private BigDecimal valorTotal(Pedido pedido) {
        return pedido.getValorTotal() == null ? BigDecimal.ZERO : pedido.getValorTotal();
    }

    private List<DashboardResponse.VendaPorDia> montarVendasPorDia(
            LocalDate inicio, LocalDate fim, List<Pedido> pedidos) {
        Map<LocalDate, List<Pedido>> porDia = new TreeMap<>();
        for (LocalDate dia = inicio; !dia.isAfter(fim); dia = dia.plusDays(1)) {
            porDia.put(dia, new ArrayList<>());
        }
        pedidos.stream()
                .filter(this::geraVenda)
                .forEach(pedido -> porDia.get(pedido.getDataEntrada().atZone(zoneId).toLocalDate()).add(pedido));

        return porDia.entrySet().stream()
                .map(entrada -> new DashboardResponse.VendaPorDia(
                        entrada.getKey(), entrada.getValue().size(),
                        entrada.getValue().stream().map(this::valorTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private List<DashboardResponse.PedidosPorStatus> montarPedidosPorStatus(List<Pedido> pedidos) {
        Map<StatusPedido, Long> quantidades = new EnumMap<>(StatusPedido.class);
        for (StatusPedido status : StatusPedido.values()) {
            quantidades.put(status, 0L);
        }
        pedidos.stream().map(Pedido::getStatus).filter(status -> status != null)
                .forEach(status -> quantidades.compute(status, (chave, quantidade) -> quantidade + 1));

        return quantidades.entrySet().stream()
                .map(entrada -> new DashboardResponse.PedidosPorStatus(
                        entrada.getKey(), entrada.getKey().getDescricao(), entrada.getValue()))
                .toList();
    }

    private List<DashboardResponse.ProdutoMaisVendido> montarRanking(List<Pedido> pedidos) {
        Map<String, ProdutoResumo> produtos = new HashMap<>();
        pedidos.stream().filter(this::geraVenda)
                .map(Pedido::getItens).filter(itens -> itens != null).flatMap(List::stream)
                .forEach(item -> acumularProduto(produtos, item));

        return produtos.values().stream()
                .sorted(Comparator.comparingLong(ProdutoResumo::quantidade).reversed()
                        .thenComparing(ProdutoResumo::vendas, Comparator.reverseOrder())
                        .thenComparing(ProdutoResumo::nome, String.CASE_INSENSITIVE_ORDER))
                .limit(LIMITE_PRODUTOS)
                .map(produto -> new DashboardResponse.ProdutoMaisVendido(
                        produto.id(), produto.nome(), produto.quantidade(),
                        produto.vendas().setScale(2, RoundingMode.HALF_UP)))
                .toList();
    }

    private void acumularProduto(Map<String, ProdutoResumo> produtos, ItemPedido item) {
        if (item == null || item.getQuantidade() <= 0) {
            return;
        }
        String id = item.getProdutoId() == null ? item.getNomeProduto() : item.getProdutoId();
        String chave = id == null ? "produto-sem-identificacao" : id;
        String nome = item.getNomeProduto() == null ? "Produto sem nome" : item.getNomeProduto();
        BigDecimal valor = item.getSubtotal();
        if (valor == null) {
            valor = item.getPrecoUnitario() == null ? BigDecimal.ZERO
                    : item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
        }
        ProdutoResumo atual = produtos.getOrDefault(chave, new ProdutoResumo(id, nome, 0, BigDecimal.ZERO));
        produtos.put(chave, new ProdutoResumo(id, nome, atual.quantidade() + item.getQuantidade(),
                atual.vendas().add(valor)));
    }

    private record ProdutoResumo(String id, String nome, long quantidade, BigDecimal vendas) { }
}
