package itesm.medsync.interfaces.rest.article;

import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.usecase.AddTagToArticleUseCase;
import itesm.medsync.domain.article.usecase.CreateArticleUseCase;
import itesm.medsync.domain.article.usecase.GetArticleByIdUseCase;
import itesm.medsync.domain.article.usecase.GetRecentArticlesUseCase;
import itesm.medsync.domain.article.usecase.ListArticlesUseCase;
import itesm.medsync.domain.article.usecase.MarkArticleAsReadUseCase;
import itesm.medsync.domain.article.usecase.GetSavedArticlesUseCase;
import itesm.medsync.domain.article.usecase.RemoveTagFromArticleUseCase;
import itesm.medsync.domain.article.usecase.SaveArticleUseCase;
import itesm.medsync.domain.article.usecase.SyncPubmedArticlesUseCase;
import itesm.medsync.domain.article.usecase.UnsaveArticleUseCase;
import itesm.medsync.application.security.AuthenticatedUserContext;
import itesm.medsync.domain.shared.model.TipoClinico;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@Path("/api/articles")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Articles", description = "Artículos científicos y sus tags")
public class ArticleResource {

    @Inject
    AuthenticatedUserContext authContext;

    private final CreateArticleUseCase createArticle;
    private final GetArticleByIdUseCase getArticleById;
    private final ListArticlesUseCase listArticles;
    private final GetRecentArticlesUseCase getRecentArticles;
    private final AddTagToArticleUseCase addTag;
    private final RemoveTagFromArticleUseCase removeTag;
    private final SyncPubmedArticlesUseCase syncPubmed;
    private final MarkArticleAsReadUseCase markAsRead;
    private final SaveArticleUseCase saveArticle;
    private final UnsaveArticleUseCase unsaveArticle;
    private final GetSavedArticlesUseCase getSavedArticles;

    @Inject
    public ArticleResource(CreateArticleUseCase createArticle,
                           GetArticleByIdUseCase getArticleById,
                           ListArticlesUseCase listArticles,
                           GetRecentArticlesUseCase getRecentArticles,
                           AddTagToArticleUseCase addTag,
                           RemoveTagFromArticleUseCase removeTag,
                           SyncPubmedArticlesUseCase syncPubmed,
                           MarkArticleAsReadUseCase markAsRead,
                           SaveArticleUseCase saveArticle,
                           UnsaveArticleUseCase unsaveArticle,
                           GetSavedArticlesUseCase getSavedArticles) {
        this.createArticle = createArticle;
        this.getArticleById = getArticleById;
        this.listArticles = listArticles;
        this.getRecentArticles = getRecentArticles;
        this.addTag = addTag;
        this.removeTag = removeTag;
        this.syncPubmed = syncPubmed;
        this.markAsRead = markAsRead;
        this.saveArticle = saveArticle;
        this.unsaveArticle = unsaveArticle;
        this.getSavedArticles = getSavedArticles;
    }

    // -------------------------------------------------------------------------
    // CRUD existente
    // -------------------------------------------------------------------------

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
    @Operation(summary = "Listar artículos paginados")
    @APIResponse(responseCode = "200", description = "Página de artículos con total")
    public PagedArticlesResponse list(@QueryParam("page") @DefaultValue("0") int page,
                                      @QueryParam("size") @DefaultValue("20") int size) {
        return ArticleRestMapper.toPagedResponse(listArticles.execute(page, size));
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Obtener artículo por id — incluye fuente, revista, año y enlace oficial")
    @APIResponse(responseCode = "200", description = "Metadatos completos del artículo (fuente, revista, año, url)")
    @APIResponse(responseCode = "404", description = "Artículo no encontrado")
    public ArticleResponse getById(@PathParam("id") UUID id) {
        return ArticleRestMapper.toResponse(getArticleById.execute(id));
    }

    // -------------------------------------------------------------------------
    // NUEVO: Artículos recientes para el home del médico
    // -------------------------------------------------------------------------

    /**
     * GET /api/articles/recent
     * Devuelve los artículos más recientes (ordenados por fecha de actualización
     * descendente). Es el endpoint que consume el home del médico.
     * Incluye todos los metadatos: revista, año, url (enlace oficial), DOI.
     */
    @GET
    @Path("/recent")
    @Operation(summary = "Artículos recientes de PubMed para el home del médico",
               description = "Devuelve los artículos ordenados por fecha de actualización " +
                             "descendente. Cada artículo incluye fuente (PubMed), revista, " +
                             "año de publicación y enlace oficial.")
    @APIResponse(responseCode = "200", description = "Página de artículos recientes con metadatos completos")
    public PagedArticlesResponse recent(@QueryParam("page") @DefaultValue("0") int page,
                                        @QueryParam("size") @DefaultValue("20") int size) {
        return ArticleRestMapper.toPagedResponse(getRecentArticles.execute(page, size));
    }

    // -------------------------------------------------------------------------
    // NUEVO: Artículos guardados por el usuario
    // -------------------------------------------------------------------------

    @GET
    @Path("/saved")
    @Operation(summary = "Artículos guardados por el usuario actual",
               description = "Devuelve los artículos que el usuario ha guardado para leer después, ordenados por fecha de guardado descendente.")
    @APIResponse(responseCode = "200", description = "Página de artículos guardados")
    public PagedArticlesResponse saved(@QueryParam("page") @DefaultValue("0") int page,
                                       @QueryParam("size") @DefaultValue("20") int size) {
        UUID userId = authContext.getCurrentUser().user().getId();
        return ArticleRestMapper.toPagedResponse(getSavedArticles.execute(userId, page, size));
    }

    // -------------------------------------------------------------------------
    // NUEVO: Disparo manual de sincronización con PubMed
    // -------------------------------------------------------------------------

    /**
     * POST /api/articles/sync
     * Dispara la sincronización manual con la API de PubMed (esearch + efetch).
     * En producción la sincronización ocurre automáticamente una vez al día a las 12:00
     * via el scheduler. Este endpoint permite forzarla de forma inmediata.
     */
    @POST
    @Path("/sync")
    @Operation(summary = "Forzar sincronización manual con PubMed",
               description = "Llama a esearch (trending[sb]) y a efetch para actualizar " +
                             "la base de datos con los artículos más recientes de PubMed.")
    @APIResponse(responseCode = "200", description = "Sincronización ejecutada — devuelve cantidad de artículos procesados")
    public Response syncNow() {
        int count = syncPubmed.execute();
        return Response.ok(Map.of("articulosProcesados", count)).build();
    }

    // -------------------------------------------------------------------------
    // Marcar artículo como leído
    // -------------------------------------------------------------------------

    @POST
    @Path("/{id}/read")
    @Operation(summary = "Marcar artículo como leído",
               description = "Registra que el usuario autenticado ha leído el artículo. Idempotente.")
    @APIResponse(responseCode = "204", description = "Marcado como leído")
    @APIResponse(responseCode = "404", description = "Artículo no encontrado")
    public Response markAsRead(@PathParam("id") UUID articleId) {
        UUID userId = authContext.getCurrentUser().user().getId();
        markAsRead.execute(userId, articleId);
        return Response.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Guardar / Quitar artículo (Noticias Guardadas)
    // -------------------------------------------------------------------------

    @POST
    @Path("/{id}/save")
    @Operation(summary = "Guardar artículo para después",
               description = "Registra que el usuario autenticado ha guardado el artículo.")
    @APIResponse(responseCode = "204", description = "Artículo guardado")
    @APIResponse(responseCode = "404", description = "Artículo no encontrado")
    public Response saveArticle(@PathParam("id") UUID articleId) {
        UUID userId = authContext.getCurrentUser().user().getId();
        saveArticle.execute(userId, articleId);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/save")
    @Operation(summary = "Quitar artículo de guardados",
               description = "Elimina el artículo de la lista de guardados del usuario.")
    @APIResponse(responseCode = "204", description = "Artículo quitado de guardados")
    @APIResponse(responseCode = "404", description = "Artículo no encontrado")
    public Response unsaveArticle(@PathParam("id") UUID articleId) {
        UUID userId = authContext.getCurrentUser().user().getId();
        unsaveArticle.execute(userId, articleId);
        return Response.noContent().build();
    }

    // -------------------------------------------------------------------------
    // Tags
    // -------------------------------------------------------------------------

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

