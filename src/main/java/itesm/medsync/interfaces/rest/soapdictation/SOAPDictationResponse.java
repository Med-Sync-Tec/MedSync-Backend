package itesm.medsync.interfaces.rest.soapdictation;

public class SOAPDictationResponse {
    public String motivoConsulta;
    public String subjetivo;
    public String objetivo;
    public String evaluacion;
    public String diagnostico;
    public String plan;
    public String prescripcion;

    public SOAPDictationResponse(String motivoConsulta, String subjetivo, String objetivo,
                                  String evaluacion, String diagnostico, String plan,
                                  String prescripcion) {
        this.motivoConsulta = motivoConsulta;
        this.subjetivo = subjetivo;
        this.objetivo = objetivo;
        this.evaluacion = evaluacion;
        this.diagnostico = diagnostico;
        this.plan = plan;
        this.prescripcion = prescripcion;
    }
}
