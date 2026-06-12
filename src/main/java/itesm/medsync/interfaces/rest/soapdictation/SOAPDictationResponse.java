package itesm.medsync.interfaces.rest.soapdictation;

public record SOAPDictationResponse(
        String motivoConsulta,
        String subjetivo,
        String objetivo,
        String evaluacion,
        String diagnostico,
        String plan,
        String prescripcion) {
}
