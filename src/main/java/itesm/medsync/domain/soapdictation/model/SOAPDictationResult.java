package itesm.medsync.domain.soapdictation.model;

public record SOAPDictationResult(
        String motivoConsulta,
        String subjetivo,
        String objetivo,
        String evaluacion,
        String diagnostico,
        String plan,
        String prescripcion
) {}
