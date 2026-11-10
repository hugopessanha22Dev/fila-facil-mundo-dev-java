package com.mundodev.filafacil.manifest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManifestValidatorTest {

    private final ManifestValidator validator = new ManifestValidator();
    private final ManifestReader reader = new ManifestReader();
    private final ManifestValidationService service = new ManifestValidationService(reader, validator);

    @Test
    void validaManifestoValidoJsonDoFixture() {
        Path fixture = Path.of("data/manifestos/valido.json");
        ValidationReport report = service.validar(fixture);
        assertTrue(report.aprovado());
        assertEquals("aprovado", report.statusFinal());
        assertEquals(2, report.totalRegistros());
        assertEquals(0, report.totalErros());
        assertTrue(report.render().contains("Status final: aprovado"));
    }

    @Test
    void validaManifestoValidoCsvDoFixture() {
        Path fixture = Path.of("data/manifestos/valido.csv");
        ValidationReport report = service.validar(fixture);
        assertTrue(report.aprovado());
        assertEquals(2, report.totalRegistros());
    }

    @Test
    void reprovaDuplicataECamposVaziosNoFixtureInvalido() {
        Path fixture = Path.of("data/manifestos/invalido.json");
        ValidationReport report = service.validar(fixture);
        assertFalse(report.aprovado());
        assertEquals("reprovado", report.statusFinal());
        assertEquals(3, report.totalRegistros());
        assertTrue(report.totalErros() >= 3);
        assertTrue(report.erros().stream().anyMatch(e -> "PACOTE_DUPLICADO".equals(e.codigo())));
        assertTrue(report.erros().stream().anyMatch(e -> "ROTA_VAZIA".equals(e.codigo())));
        assertTrue(report.erros().stream().anyMatch(e -> "SEM_VEICULO".equals(e.codigo())));
        assertTrue(report.erros().stream().anyMatch(e -> "SEM_RESPONSAVEL".equals(e.codigo())));
        assertTrue(report.render().contains("Status final: reprovado"));
    }

    @Test
    void detectaCampoObrigatorioVazio() {
        List<ManifestRecord> records = List.of(
                new ManifestRecord("PKG-1", "R1", "V1", "Resp", ""));
        ValidationReport report = validator.validate(records);
        assertFalse(report.aprovado());
        assertTrue(report.erros().stream().anyMatch(e -> "CAMPO_OBRIGATORIO".equals(e.codigo())));
    }

    @Test
    void detectaPacoteDuplicado() {
        List<ManifestRecord> records = List.of(
                new ManifestRecord("PKG-1", "R1", "V1", "A", "pendente"),
                new ManifestRecord("PKG-1", "R2", "V2", "B", "pendente"));
        ValidationReport report = validator.validate(records);
        assertFalse(report.aprovado());
        assertEquals(1, report.erros().stream().filter(e -> "PACOTE_DUPLICADO".equals(e.codigo())).count());
    }

    @Test
    void salvaRelatorioEmArquivo(@TempDir Path tempDir) throws Exception {
        ValidationReport report = validator.validate(List.of(
                new ManifestRecord("PKG-1", "R1", "V1", "A", "pendente")));
        Path out = tempDir.resolve("relatorio.txt");
        service.salvarRelatorio(report, out);
        String content = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(content.contains("aprovado"));
        assertTrue(content.contains("Total de registros: 1"));
    }

    @Test
    void cliRetornaZeroParaValidoEUmParaInvalido() {
        assertEquals(0, ManifestCli.run(new String[]{"data/manifestos/valido.json"}));
        assertEquals(1, ManifestCli.run(new String[]{"data/manifestos/invalido.json"}));
    }

    @Test
    void cliSalvaRelatorioComFlagOut(@TempDir Path tempDir) {
        Path out = tempDir.resolve("out.txt");
        int code = ManifestCli.run(new String[]{
                "data/manifestos/valido.json",
                "--out",
                out.toString()
        });
        assertEquals(0, code);
        assertTrue(Files.exists(out));
    }
}
