package itesm.medsync.application.consultaaianalysis;

import itesm.medsync.domain.hospital.model.Consulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaSoapJoinerTest {

    private Consulta consulta(String motivo, String sub, String obj, String eval,
                              String plan, String presc, String diag) {
        return new Consulta(
                "c-1", "exp-1", LocalDateTime.of(2025, 1, 15, 10, 0),
                motivo, sub, obj, eval, plan, presc, diag,
                null, null);
    }

    @Test
    @DisplayName("Todos los campos poblados → joined contiene los 7 headers en orden y los valores")
    void allFieldsPopulated() {
        Consulta c = consulta(
                "Dolor torácico",
                "Refiere disnea",
                "TA 150/95",
                "HTA estadio 2",
                "Iniciar Losartán",
                "Losartán 50mg",
                "I10 Hipertensión");

        ConsultaSoapJoiner.JoinedConsulta j = ConsultaSoapJoiner.join(c);

        assertFalse(j.allBlank());
        assertFalse(j.wasTruncated());
        // Order check: motivo first, then subjetivo, objetivo, evaluación, plan, prescripción, diagnóstico
        int iMotivo = j.text().indexOf("[MOTIVO DE CONSULTA]");
        int iSub = j.text().indexOf("[SUBJETIVO]");
        int iObj = j.text().indexOf("[OBJETIVO]");
        int iEval = j.text().indexOf("[EVALUACIÓN]");
        int iPlan = j.text().indexOf("[PLAN]");
        int iPresc = j.text().indexOf("[PRESCRIPCIÓN]");
        int iDiag = j.text().indexOf("[DIAGNÓSTICO]");
        assertTrue(iMotivo >= 0 && iSub > iMotivo && iObj > iSub && iEval > iObj
                && iPlan > iEval && iPresc > iPlan && iDiag > iPresc);
        assertTrue(j.text().contains("Dolor torácico"));
        assertTrue(j.text().contains("Losartán 50mg"));
        assertTrue(j.text().contains("I10 Hipertensión"));
    }

    @Test
    @DisplayName("Campo individual blank → ese sólo sección usa '(no registrado)'")
    void singleFieldBlank() {
        Consulta c = consulta(
                "Dolor torácico", "", null, "  ",
                "Plan X", null, "I10");

        ConsultaSoapJoiner.JoinedConsulta j = ConsultaSoapJoiner.join(c);

        assertFalse(j.allBlank());
        // Conteo: 4 secciones llevan "(no registrado)" — subjetivo, objetivo, evaluación, prescripción
        long placeholders = j.text().lines()
                .filter(line -> line.equals("(no registrado)"))
                .count();
        assertEquals(4, placeholders);
        assertTrue(j.text().contains("Dolor torácico"));
        assertTrue(j.text().contains("Plan X"));
    }

    @Test
    @DisplayName("Los 7 campos null/blank → allBlank=true para que el caller dispare 400")
    void allFieldsBlank() {
        Consulta c = consulta(null, "", "  ", null, "", null, "   ");

        ConsultaSoapJoiner.JoinedConsulta j = ConsultaSoapJoiner.join(c);

        assertTrue(j.allBlank());
        assertFalse(j.wasTruncated());
    }

    @Test
    @DisplayName("Texto > 50k caracteres se trunca a exactamente 50000; señala wasTruncated y originalLength")
    void truncationAt50k() {
        String huge = "x".repeat(60_000);
        Consulta c = consulta(huge, null, null, null, null, null, null);

        ConsultaSoapJoiner.JoinedConsulta j = ConsultaSoapJoiner.join(c);

        assertEquals(50_000, j.text().length());
        assertTrue(j.wasTruncated());
        assertTrue(j.originalLength() > 50_000);
        assertFalse(j.allBlank());
    }

    @Test
    @DisplayName("Texto <= 50k no se trunca y wasTruncated=false")
    void noTruncationUnderCap() {
        Consulta c = consulta("Corto", null, null, null, null, null, null);

        ConsultaSoapJoiner.JoinedConsulta j = ConsultaSoapJoiner.join(c);

        assertFalse(j.wasTruncated());
        assertEquals(j.text().length(), j.originalLength());
    }
}
