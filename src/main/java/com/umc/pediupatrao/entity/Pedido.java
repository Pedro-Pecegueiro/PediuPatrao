package com.umc.pediupatrao.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "pedidos")
public class Pedido {
    @Id
    private String id;
    @Indexed
    private String clienteId;
    private String clienteNome;
    private List<ItemPedido> itens = new ArrayList<>();
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal descontoPercentual = BigDecimal.ZERO;
    private BigDecimal valorDesconto = BigDecimal.ZERO;
    private BigDecimal valorTotal = BigDecimal.ZERO;
    @Indexed
    private StatusPedido status;
    private String responsavelRegistro;
    private Instant dataEntrada;
    private String responsavelSaida;
    private Instant dataSaida;
    private String justificativaCancelamento;
    private String responsavelCancelamento;
    private Instant dataCancelamento;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public void setClienteId(String clienteId) {
        this.clienteId = clienteId;
    }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }
    public List<ItemPedido> getItens() { return itens; }
    public void setItens(List<ItemPedido> itens) { this.itens = itens; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDescontoPercentual() { return descontoPercentual; }
    public void setDescontoPercentual(BigDecimal descontoPercentual) { this.descontoPercentual = descontoPercentual; }
    public BigDecimal getValorDesconto() { return valorDesconto; }
    public void setValorDesconto(BigDecimal valorDesconto) { this.valorDesconto = valorDesconto; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public StatusPedido getStatus() { return status; }
    public void setStatus(StatusPedido status) { this.status = status; }
    public String getResponsavelRegistro() { return responsavelRegistro; }
    public void setResponsavelRegistro(String responsavelRegistro) { this.responsavelRegistro = responsavelRegistro; }
    public Instant getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(Instant dataEntrada) { this.dataEntrada = dataEntrada; }
    public String getResponsavelSaida() { return responsavelSaida; }
    public void setResponsavelSaida(String responsavelSaida) { this.responsavelSaida = responsavelSaida; }
    public Instant getDataSaida() { return dataSaida; }
    public void setDataSaida(Instant dataSaida) { this.dataSaida = dataSaida; }
    public String getJustificativaCancelamento() { return justificativaCancelamento; }
    public void setJustificativaCancelamento(String justificativaCancelamento) { this.justificativaCancelamento = justificativaCancelamento; }
    public String getResponsavelCancelamento() { return responsavelCancelamento; }
    public void setResponsavelCancelamento(String responsavelCancelamento) { this.responsavelCancelamento = responsavelCancelamento; }
    public Instant getDataCancelamento() { return dataCancelamento; }
    public void setDataCancelamento(Instant dataCancelamento) { this.dataCancelamento = dataCancelamento; }
}
