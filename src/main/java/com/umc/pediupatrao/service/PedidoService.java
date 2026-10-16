package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoRequest;
import com.umc.pediupatrao.entity.AlteracaoCampo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.exception.RecursoNaoEncontradoException;
import com.umc.pediupatrao.exception.RegraNegocioException;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private static final BigDecimal CEM_POR_CENTO = new BigDecimal("100");
    private static final BigDecimal LIMITE_DESCONTO = new BigDecimal("20.00");

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    public PedidoService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository,
                         ProdutoRepository produtoRepository, AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAllByOrderByDataEntradaDesc();
    }

    @PreAuthorize("hasAnyRole('ADMIN','GERENTE','ATENDENTE')")
    public Pedido buscarPorId(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido não encontrado."));
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido criarPedido(PedidoRequest request) {
        Cliente cliente = clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado."));
        if (request.itens() == null || request.itens().isEmpty()) {
            throw new RegraNegocioException("O pedido deve possuir pelo menos um item.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (ItemPedidoRequest itemRequest : request.itens()) {
            if (itemRequest.quantidade() <= 0) {
                throw new RegraNegocioException("A quantidade dos itens deve ser maior que zero.");
            }
            Produto produto = produtoRepository.findById(itemRequest.produtoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado."));
            if (!Boolean.TRUE.equals(produto.getAtivo())) {
                throw new RegraNegocioException("O produto " + produto.getNome() + " está inativo.");
            }
            BigDecimal subtotalItem = dinheiro(produto.getPreco().multiply(BigDecimal.valueOf(itemRequest.quantidade())));
            ItemPedido item = new ItemPedido();
            item.setProdutoId(produto.getId());
            item.setNomeProduto(produto.getNome());
            item.setQuantidade(itemRequest.quantidade());
            item.setPrecoUnitario(dinheiro(produto.getPreco()));
            item.setSubtotal(subtotalItem);
            itens.add(item);
            subtotal = subtotal.add(subtotalItem);
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(cliente.getId());
        pedido.setClienteNome(cliente.getNome());
        pedido.setItens(itens);
        pedido.setSubtotal(dinheiro(subtotal));
        pedido.setDescontoPercentual(BigDecimal.ZERO.setScale(2));
        pedido.setValorDesconto(BigDecimal.ZERO.setScale(2));
        pedido.setValorTotal(dinheiro(subtotal));
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setResponsavelRegistro(usuarioAtual());
        pedido.setDataEntrada(Instant.now());

        Pedido salvo = pedidoRepository.save(pedido);
        Map<String, AlteracaoCampo> campos = new LinkedHashMap<>();
        campos.put("clienteId", new AlteracaoCampo(null, salvo.getClienteId()));
        campos.put("itens", new AlteracaoCampo(null, resumirItens(salvo.getItens())));
        campos.put("subtotal", new AlteracaoCampo(null, salvo.getSubtotal()));
        campos.put("descontoPercentual", new AlteracaoCampo(null, salvo.getDescontoPercentual()));
        campos.put("valorDesconto", new AlteracaoCampo(null, salvo.getValorDesconto()));
        campos.put("valorTotal", new AlteracaoCampo(null, salvo.getValorTotal()));
        campos.put("status", new AlteracaoCampo(null, salvo.getStatus()));
        campos.put("responsavelRegistro", new AlteracaoCampo(null, salvo.getResponsavelRegistro()));
        campos.put("dataEntrada", new AlteracaoCampo(null, salvo.getDataEntrada()));
        auditoriaService.registrar("INCLUSAO", "PEDIDO", salvo.getId(), campos, null);
        return salvo;
    }

    @PreAuthorize("hasAnyRole('GERENTE','ATENDENTE')")
    public Pedido atualizarStatus(String id, StatusPedido novoStatus) {
        Pedido pedido = buscarPorId(id);
        StatusPedido anterior = pedido.getStatus();
        if (novoStatus == null || novoStatus == StatusPedido.CANCELADO) {
            throw new RegraNegocioException("Use a operação específica de cancelamento.");
        }
        if (!anterior.proximosStatus().contains(novoStatus)) {
            throw new RegraNegocioException("Transição inválida de " + anterior.getDescricao()
                    + " para " + novoStatus.getDescricao() + ".");
        }

        pedido.setStatus(novoStatus);
        if (novoStatus.registraSaida()) {
            pedido.setDataSaida(Instant.now());
            pedido.setResponsavelSaida(usuarioAtual());
        }
        Pedido salvo = pedidoRepository.save(pedido);

        Map<String, AlteracaoCampo> campos = new LinkedHashMap<>();
        campos.put("status", new AlteracaoCampo(anterior, novoStatus));
        if (novoStatus.registraSaida()) {
            campos.put("dataSaida", new AlteracaoCampo(null, salvo.getDataSaida()));
            campos.put("responsavelSaida", new AlteracaoCampo(null, salvo.getResponsavelSaida()));
        }
        auditoriaService.registrar("MUDANCA_STATUS", "PEDIDO", salvo.getId(), campos, null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().permiteDescontoOuCancelamento()) {
            throw new RegraNegocioException("O desconto só pode ser aplicado antes da saída ou retirada.");
        }
        if (percentual == null || percentual.compareTo(BigDecimal.ZERO) <= 0
                || percentual.compareTo(LIMITE_DESCONTO) > 0) {
            throw new RegraNegocioException("O desconto deve ser maior que 0% e limitado a 20%.");
        }

        BigDecimal percentualAnterior = pedido.getDescontoPercentual();
        BigDecimal descontoAnterior = pedido.getValorDesconto();
        BigDecimal totalAnterior = pedido.getValorTotal();
        BigDecimal desconto = dinheiro(pedido.getSubtotal().multiply(percentual)
                .divide(CEM_POR_CENTO, 4, RoundingMode.HALF_UP));
        pedido.setDescontoPercentual(percentual.setScale(2, RoundingMode.HALF_UP));
        pedido.setValorDesconto(desconto);
        pedido.setValorTotal(dinheiro(pedido.getSubtotal().subtract(desconto)));
        Pedido salvo = pedidoRepository.save(pedido);

        Map<String, AlteracaoCampo> campos = new LinkedHashMap<>();
        campos.put("descontoPercentual", new AlteracaoCampo(percentualAnterior, salvo.getDescontoPercentual()));
        campos.put("valorDesconto", new AlteracaoCampo(descontoAnterior, salvo.getValorDesconto()));
        campos.put("valorTotal", new AlteracaoCampo(totalAnterior, salvo.getValorTotal()));
        auditoriaService.registrar("APLICACAO_DESCONTO", "PEDIDO", salvo.getId(), campos, null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa) {
        Pedido pedido = buscarPorId(id);
        if (!pedido.getStatus().permiteDescontoOuCancelamento()) {
            throw new RegraNegocioException("O pedido não pode ser cancelado após a saída, retirada ou encerramento.");
        }
        if (justificativa == null || justificativa.trim().length() < 5) {
            throw new RegraNegocioException("Informe uma justificativa de cancelamento com pelo menos 5 caracteres.");
        }

        StatusPedido anterior = pedido.getStatus();
        pedido.setStatus(StatusPedido.CANCELADO);
        pedido.setJustificativaCancelamento(justificativa.trim());
        pedido.setResponsavelCancelamento(usuarioAtual());
        pedido.setDataCancelamento(Instant.now());
        Pedido salvo = pedidoRepository.save(pedido);

        Map<String, AlteracaoCampo> campos = new LinkedHashMap<>();
        campos.put("status", new AlteracaoCampo(anterior, StatusPedido.CANCELADO));
        campos.put("dataCancelamento", new AlteracaoCampo(null, salvo.getDataCancelamento()));
        auditoriaService.registrar("CANCELAMENTO", "PEDIDO", salvo.getId(), campos, justificativa);
        return salvo;
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "SISTEMA" : authentication.getName();
    }

    private BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private String resumirItens(List<ItemPedido> itens) {
        return itens.stream()
                .map(item -> item.getQuantidade() + "x " + item.getNomeProduto() + " @ " + item.getPrecoUnitario())
                .collect(Collectors.joining("; "));
    }
}
