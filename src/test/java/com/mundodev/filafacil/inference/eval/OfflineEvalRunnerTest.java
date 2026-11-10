package com.mundodev.filafacil.inference.eval;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OfflineEvalRunnerTest {

    @Test
    void goldenDatasetEvalEmModoMock() throws Exception {
        OfflineEvalRunner.EvalReport report = OfflineEvalRunner.runFromClasspath();
        System.out.println(report.summary());
        assertEquals(3, report.total());
        assertEquals(3, report.jsonValido());
        assertEquals(0, report.failures());
        assertTrue(report.summary().contains("jsonValido%"));
    }
}
