package com.mundodev.filafacil.state;

import com.mundodev.filafacil.coletas.Coleta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppState {

    public record AppError(String origem, String mensagem) {
    }

    public record FiltrosAtivos(String status, String prioridade) {
        public static FiltrosAtivos nenhum() {
            return new FiltrosAtivos(null, null);
        }
    }

    private final List<Coleta> coletas = new ArrayList<>();
    private boolean loading;
    private AppError ultimoErro;
    private FiltrosAtivos filtros = FiltrosAtivos.nenhum();

    public List<Coleta> getColetas() {
        return Collections.unmodifiableList(coletas);
    }

    public void setColetas(List<Coleta> novas) {
        coletas.clear();
        if (novas != null) {
            coletas.addAll(novas);
        }
    }

    public void addColeta(Coleta coleta) {
        coletas.add(coleta);
    }

    public boolean isLoading() {
        return loading;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public AppError getUltimoErro() {
        return ultimoErro;
    }

    public void setUltimoErro(String origem, String mensagem) {
        this.ultimoErro = new AppError(origem, mensagem);
    }

    public void limparErro() {
        this.ultimoErro = null;
    }

    public FiltrosAtivos getFiltros() {
        return filtros;
    }

    public void setFiltros(FiltrosAtivos filtros) {
        this.filtros = filtros == null ? FiltrosAtivos.nenhum() : filtros;
    }
}
