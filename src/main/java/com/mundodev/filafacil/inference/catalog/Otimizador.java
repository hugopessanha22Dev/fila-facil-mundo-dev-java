package com.mundodev.filafacil.inference.catalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Otimizador {

    static final String MARCADOR = "evidencia-critica";

    private final ProvedorInferencia provedor;
    private final Map<String, String> cache = new LinkedHashMap<>();

    public Otimizador(ProvedorInferencia provedor) {
        this.provedor = provedor;
    }

    public String classificar(String texto) {
        String chave = "classificacao\n" + texto;
        String salva = cache.get(chave);
        if (salva != null) {
            return salva;
        }
        String resposta = provedor.completar("classificacao", comprimir(texto)).texto();
        cache.put(chave, resposta);
        return resposta;
    }

    public String buscar(String pergunta) {
        return provedor.completar("busca_contextual", podar(pergunta)).texto();
    }

    public void classificarLote(List<String> textos) {
        String corpo = textos.stream().map(Otimizador::comprimir).reduce((a, b) -> a + "\n" + b).orElse("");
        provedor.completar("classificacao", corpo);
    }

    public static String comprimir(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replaceAll("[ \\t]{2,}", " ").replaceAll("\\n{3,}", "\n\n").trim();
    }

    public static String podar(String contexto) {
        if (contexto == null) {
            return "";
        }
        boolean temMarcador = contexto.contains(MARCADOR);
        if (contexto.length() <= 120) {
            return contexto;
        }
        String inicio = contexto.substring(0, 40);
        String fim = contexto.substring(contexto.length() - 40);
        String meio = temMarcador ? MARCADOR : "";
        return inicio + "\n" + meio + "\n" + fim;
    }
}
