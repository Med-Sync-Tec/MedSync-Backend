package itesm.medsync.interfaces.rest.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.usecase.AddTagToArticleUseCase;
import itesm.medsync.domain.article.usecase.CreateArticleUseCase;
import itesm.medsync.domain.article.usecase.GetArticleByIdUseCase;
import itesm.medsync.domain.article.usecase.ListArticlesUseCase;
import itesm.medsync.domain.article.usecase.RemoveTagFromArticleUseCase;
import itesm.medsync.domain.shared.model.TipoClinico;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/articles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Articles", description = "Artículos científicos y sus tags")
public class ArticleResource {

    private final CreateArticleUseCase createArticle;
    private final GetArticleByIdUseCase getArticleById;
    private final ListArticlesUseCase listArticles;
    private final AddTagToArticleUseCase addTag;
    private final RemoveTagFromArticleUseCase removeTag;

    @Inject
    public ArticleResource(CreateArticleUseCase createArticle,
                           GetArticleByIdUseCase getArticleById,
                           ListArticlesUseCase listArticles,
                           AddTagToArticleUseCase addTag,
                           RemoveTagFromArticleUseCase removeTag) {
        this.createArticle = createArticle;
        this.getArticleById = getArticleById;
        this.listArticles = listArticles;
        this.addTag = addTag;
        this.removeTag = removeTag;
    }

    @POST
    @Operation(summary = "Crear artículo científico")
    @APIResponse(responseCode = "201", description = "Artículo creado")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "409", description = "DOI duplicado")
    public Response create(@Valid CreateArticleRequest request, @Context UriInfo uriInfo) {
        Article created = createArticle.execute(
                request.titulo,
                request.autores,
                request.revista,
                request.anioPub,
                request.mesPub,
                request.doi,
                request.abstractText,
                request.keywords,
                request.tipoPublicacion,
                request.url);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId().toString()).build();
        return Response.created(location)
                .entity(ArticleRestMapper.toResponse(created))
                .build();
    }

    @GET
    @Operation(summary = "Listar artículos")
    @APIResponse(responseCode = "200", description = "Lista de artículos")
    public List<ArticleResponse> list() {
        return listArticles.execute().stream()
                .map(ArticleRestMapper::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener artículo por id")
    @APIResponse(responseCode = "200")
    @APIResponse(responseCode = "404", description = "Artículo no encontrado")
    public ArticleResponse getById(@PathParam("id") UUID id) {
        return ArticleRestMapper.toResponse(getArticleById.execute(id));
    }

    @POST
    @Path("/{id}/tags")
    @Operation(summary = "Agregar tag al artículo")
    @APIResponse(responseCode = "201", description = "Tag agregado")
    @APIResponse(responseCode = "400", description = "Datos inválidos")
    @APIResponse(responseCode = "404", description = "Artículo no encontrado")
    public Response addTag(@PathParam("id") UUID articleId,
                           @Valid AddArticleTagRequest request,
                           @Context UriInfo uriInfo) {
        TipoClinico tipo = TipoClinico.fromString(request.tipo);
        ArticleTag created = addTag.execute(articleId, tipo, request.valor);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId().toString()).build();
        return Response.created(location)
                .entity(ArticleRestMapper.toTagResponse(created))
                .build();
    }

    @DELETE
    @Path("/{id}/tags/{tagId}")
    @Operation(summary = "Eliminar tag del artículo")
    @APIResponse(responseCode = "204", description = "Tag eliminado")
    @APIResponse(responseCode = "404", description = "Artículo o tag no encontrado")
    public Response removeTag(@PathParam("id") UUID articleId,
                              @PathParam("tagId") UUID tagId) {
        removeTag.execute(articleId, tagId);
        return Response.noContent().build();
    }
}
