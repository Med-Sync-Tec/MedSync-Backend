package itesm.medsync.interfaces.rest.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateArticleRequest {

    @NotBlank
    @Size(max = 1000)
    public String titulo;

    public String autores;

    @Size(max = 500)
    public String revista;

    public Integer anioPub;

    @Size(max = 20)
    public String mesPub;

    @Size(max = 200)
    public String doi;

    public String abstractText;

    public String keywords;

    @Size(max = 100)
    public String tipoPublicacion;

    @Size(max = 500)
    public String url;
}
