package com.mundodev.filafacil.inference.catalog;

public final class MockCatalogo implements ProvedorInferencia {

    private int chamadas;

    public int chamadas() {
        return chamadas;
    }

    @Override
    public RespostaProvedor completar(String tarefa, String texto) {
        chamadas++;
        boolean json = "extracao".equals(tarefa) || "resposta_assistida".equals(tarefa);
        String corpo = json ? "{\"tarefa\":\"" + tarefa + "\"}" : "ok " + tarefa;
        int tokens = Math.max(1, corpo.length() / 4);
        return new RespostaProvedor(corpo, 1, 0.001, tokens, false, "nenhuma");
    }
}
