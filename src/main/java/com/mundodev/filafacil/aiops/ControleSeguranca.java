package com.mundodev.filafacil.aiops;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ControleSeguranca {

    private static final Pattern SEGREDO = Pattern.compile("(?i)(?:gsk_|sk-)[A-Za-z0-9_\\-]+|\\d{11,}");

    private ControleSeguranca() {
    }

    public record Resultado(boolean aceito, boolean executouFerramenta, String motivo, String texto) {
    }

    public static Resultado avaliar(String fluxo, String texto, String ferramenta, Map<String, String> argumentos) {
        String normal = normalizar(texto);
        String mascarado = mascarar(texto);
        if (normal.contains("ignore as regras") || normal.contains("ignorar as regras")
                || normal.contains("mostre o prompt") || normal.contains("mostrar o prompt")) {
            return new Resultado(false, false, "injecao", "recusado");
        }
        if (normal.contains("outro cliente")) {
            return new Resultado(false, false, "vazamento", mascarado);
        }
        if (ferramenta != null && !ferramenta.isBlank()) {
            CatalogoFluxos.Fluxo ficha = CatalogoFluxos.carregar().obter(fluxo);
            if (!ficha.ferramentas().contains(ferramenta)) {
                return new Resultado(false, false, "fora_escopo", mascarado);
            }
            for (String campo : ficha.argumentos().getOrDefault(ferramenta, java.util.List.of())) {
                String valor = argumentos == null ? null : argumentos.get(campo);
                if (valor == null || valor.isBlank()) {
                    return new Resultado(false, false, "schema", mascarado);
                }
            }
            return new Resultado(true, true, "aceito", mascarado);
        }
        return new Resultado(true, false, "aceito", mascarado);
    }

    public static String mascarar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return SEGREDO.matcher(texto).replaceAll("[oculto]");
    }

    private static String normalizar(String texto) {
        String base = texto == null ? "" : texto.toLowerCase(Locale.ROOT);
        return base
                .replace("ç", "c")
                .replace("á", "a").replace("à", "a").replace("ã", "a").replace("â", "a")
                .replace("é", "e").replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o").replace("ô", "o").replace("õ", "o")
                .replace("ú", "u");
    }
}
