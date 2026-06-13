package itesm.medsync.domain.article.usecase;

public record CreateArticleCommand(
        String titulo,
        String autores,
        String revista,
        Integer anioPub,
        String mesPub,
        String doi,
        String abstractText,
        String keywords,
        String tipoPublicacion,
        String url) {}
