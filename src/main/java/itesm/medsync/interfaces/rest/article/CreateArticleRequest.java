package itesm.medsync.interfaces.rest.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateArticleRequest {

    @NotBlank
    @Size(max = 1000)
    public String titulo;

    private String autores;

    @Size(max = 500)
    public String revista;

    private Integer anioPub;

    @Size(max = 20)
    public String mesPub;

    @Size(max = 200)
    public String doi;

    private String abstractText;

    private String keywords;

    @Size(max = 100)
    public String tipoPublicacion;

    @Size(max = 500)
    public String url;

    public String getAutores() { return autores; }
    public void setAutores(String autores) { this.autores = autores; }

    public Integer getAnioPub() { return anioPub; }
    public void setAnioPub(Integer anioPub) { this.anioPub = anioPub; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
}
