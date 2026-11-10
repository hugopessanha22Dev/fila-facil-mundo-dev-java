package com.mundodev.filafacil;

import com.mundodev.filafacil.auditoria.AuditoriaConfig;
import com.mundodev.filafacil.auditoria.AuditoriaModule;
import com.mundodev.filafacil.auditoria.service.AuditoriaService;
import com.mundodev.filafacil.coletas.ColetaService;
import com.mundodev.filafacil.coletas.factory.ColetaFactory;
import com.mundodev.filafacil.coletas.observer.ColetaEventPublisher;
import com.mundodev.filafacil.coletas.observer.ContadorMemoriaObserver;
import com.mundodev.filafacil.coletas.observer.LogOperacionalObserver;
import com.mundodev.filafacil.coletas.priority.PrioridadeManualStrategy;
import com.mundodev.filafacil.coletas.repo.FileColetaRepository;
import com.mundodev.filafacil.controller.MenuController;
import com.mundodev.filafacil.inference.InferenceConfig;
import com.mundodev.filafacil.inference.InferenceFactory;
import com.mundodev.filafacil.inference.OperationalTriageService;
import com.mundodev.filafacil.inference.prompt.ConversationOrchestrator;
import com.mundodev.filafacil.integration.ViaCepApiClient;
import com.mundodev.filafacil.state.AppState;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Path arquivo = Path.of("data", "coletas.json");
        InferenceConfig inferenceConfig = InferenceConfig.fromEnvironment();
        AuditoriaConfig auditoriaConfig = AuditoriaConfig.fromEnv();

        System.out.println("Iniciando FilaFacil Logistica...");
        System.out.println("Arquivo de dados: " + arquivo.toAbsolutePath());
        System.out.println("API de enriquecimento: ViaCEP");
        System.out.println("LLM mode: " + inferenceConfig.mode()
                + " | light=" + inferenceConfig.modelLight()
                + " | detailed=" + inferenceConfig.modelDetailed());
        System.out.println("Priorizacao: strategy manual (troque no wiring do Main)");

        AppState state = new AppState();
        FileColetaRepository repository = new FileColetaRepository(arquivo);
        ColetaFactory factory = new ColetaFactory();
        PrioridadeManualStrategy priorizacao = new PrioridadeManualStrategy();
        ContadorMemoriaObserver contador = new ContadorMemoriaObserver();
        ColetaEventPublisher publisher = new ColetaEventPublisher(List.of(
                new LogOperacionalObserver(),
                contador));

        ColetaService service = new ColetaService(
                repository,
                state,
                new ViaCepApiClient(),
                factory,
                priorizacao,
                publisher);

        OperationalTriageService triageService = InferenceFactory.create(state, inferenceConfig);
        ConversationOrchestrator conversation = InferenceFactory.createConversation(state, inferenceConfig);
        AuditoriaService auditoriaService = AuditoriaModule.create(auditoriaConfig);

        if (state.getUltimoErro() != null) {
            System.out.println("Aviso na carga [" + state.getUltimoErro().origem() + "]: "
                    + state.getUltimoErro().mensagem());
        }

        try (Scanner scanner = new Scanner(System.in)) {
            new MenuController(service, triageService, conversation, auditoriaService, scanner).iniciar();
        }
    }
}
