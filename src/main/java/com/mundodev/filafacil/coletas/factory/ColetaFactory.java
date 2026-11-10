package com.mundodev.filafacil.coletas.factory;

import com.mundodev.filafacil.coletas.Coleta;
import com.mundodev.filafacil.error.EntradaException;
import com.mundodev.filafacil.model.EnderecoCep;

import java.util.Locale;
import java.util.Set;

/**
 * Factory centraliza criacao de coletas validas (status inicial pendente).
 */
public class ColetaFactory {

    public static final String STATUS_INICIAL = "pendente";
    private static final Set<String> PRIORIDADES = Set.of("baixa", "media", "alta");

    public Coleta criar(
            long id,
            String nome,
            String endereco,
            Integer pacotes,
            String prioridade,
            EnderecoCep enderecoCep) {
        if (isBlank(nome)) {
            throw new EntradaException("Nome do cliente e obrigatorio.");
        }
        if (isBlank(endereco)) {
            throw new EntradaException("Endereco e obrigatorio.");
        }
        if (pacotes == null) {
            throw new EntradaException("Quantidade de pacotes e obrigatoria.");
        }
        if (pacotes <= 0) {
            throw new EntradaException("Quantidade de pacotes deve ser maior que zero.");
        }
        if (isBlank(prioridade)) {
            throw new EntradaException("Prioridade e obrigatoria.");
        }
        String prioridadeNorm = prioridade.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
        if (!PRIORIDADES.contains(prioridadeNorm)) {
            throw new EntradaException("Prioridade invalida. Use: baixa, media, alta");
        }
        if (enderecoCep == null) {
            throw new EntradaException("Endereco CEP e obrigatorio.");
        }

        Coleta coleta = new Coleta(id, nome.trim(), endereco.trim(), pacotes, prioridadeNorm, STATUS_INICIAL);
        coleta.setCep(enderecoCep.getCep());
        coleta.setBairro(enderecoCep.getBairro());
        coleta.setCidade(enderecoCep.getLocalidade());
        coleta.setUf(enderecoCep.getUf());
        return coleta;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
