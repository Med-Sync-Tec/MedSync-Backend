package itesm.medsync.interfaces.rest.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddArticleTagRequest {

    @NotBlank
    @Size(max = 30)
    public String tipo;

    @NotBlank
    @Size(max = 500)
    public String valor;
}
