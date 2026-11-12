package com.mundodev.filafacil.excecoes.adapters;

import com.mundodev.filafacil.excecoes.application.GatewayContextos;
import com.mundodev.filafacil.excecoes.domain.OrigemExcecao;
import com.mundodev.filafacil.excecoes.domain.TipoExcecao;

import java.util.Optional;

public final class GatewayContextosMemoria implements GatewayContextos {

    @Override
    public Optional<Traducao> traduzir(String termo) {
        if (termo == null) {
            return Optional.empty();
        }
        return switch (termo) {
            case "route_blocked" -> Optional.of(new Traducao(TipoExcecao.rota_bloqueada, OrigemExcecao.roteirizacao));
            case "customer_absent" -> Optional.of(new Traducao(TipoExcecao.cliente_ausente, OrigemExcecao.atendimento));
            case "damaged_charge" -> Optional.of(new Traducao(TipoExcecao.pacote_danificado, OrigemExcecao.financeiro));
            default -> Optional.empty();
        };
    }
}
