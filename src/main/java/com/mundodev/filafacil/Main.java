package com.mundodev.filafacil;

import com.mundodev.filafacil.controller.MenuController;
import com.mundodev.filafacil.service.ColetaService;
import com.mundodev.filafacil.store.ColetaFileStore;

import java.nio.file.Path;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Path arquivo = Path.of("data", "coletas.json");
        System.out.println("Iniciando FilaFacil Logistica...");
        System.out.println("Arquivo de dados: " + arquivo.toAbsolutePath());

        ColetaFileStore store = new ColetaFileStore(arquivo);
        ColetaService service = new ColetaService(store);

        try (Scanner scanner = new Scanner(System.in)) {
            new MenuController(service, scanner).iniciar();
        }
    }
}
