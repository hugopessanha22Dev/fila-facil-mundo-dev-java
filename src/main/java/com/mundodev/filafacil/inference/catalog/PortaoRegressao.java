package com.mundodev.filafacil.inference.catalog;

import com.fasterxml.jackson.databind.JsonNode;

public final class PortaoRegressao {

    private final LimitesInferencia limites;
    private final double acuraciaBase;
    private final double formatoBase;

    public PortaoRegressao(LimitesInferencia limites, double acuraciaBase, double formatoBase) {
        this.limites = limites;
        this.acuraciaBase = acuraciaBase;
        this.formatoBase = formatoBase;
    }

    public static PortaoRegressao carregar() {
        JsonNode node = Recursos.ler("/inference/catalog/baseline.json");
        return new PortaoRegressao(
                LimitesInferencia.carregar(),
                node.path("acuracia").asDouble(),
                node.path("formato").asDouble());
    }

    public RelatorioPromocao avaliar(PonteiroVersao ponteiro, String candidata, double acuracia, double formato) {
        double quedaAcuracia = acuraciaBase - acuracia;
        double quedaFormato = formatoBase - formato;
        if (quedaAcuracia > limites.limiteRegressao() || quedaFormato > limites.limiteRegressao()) {
            return new RelatorioPromocao(false, ponteiro.ativa(), "promocao bloqueada");
        }
        ponteiro.promover(candidata);
        return new RelatorioPromocao(true, ponteiro.ativa(), "promocao aceita");
    }
}
