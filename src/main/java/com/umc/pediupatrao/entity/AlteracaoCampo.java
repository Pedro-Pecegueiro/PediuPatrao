package com.umc.pediupatrao.entity;

public class AlteracaoCampo {
    private String valorAnterior;
    private String valorNovo;

    public AlteracaoCampo() {
    }

    public AlteracaoCampo(Object valorAnterior, Object valorNovo) {
        this.valorAnterior = valorAnterior == null ? null : String.valueOf(valorAnterior);
        this.valorNovo = valorNovo == null ? null : String.valueOf(valorNovo);
    }

    public String getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(String valorAnterior) { this.valorAnterior = valorAnterior; }
    public String getValorNovo() { return valorNovo; }
    public void setValorNovo(String valorNovo) { this.valorNovo = valorNovo; }
}
