package itesm.medsync.interfaces.rest.article;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class ArticleResourceIT {

    private Map<String, Object> validPayload(String doi) {
        Map<String, Object> m = new HashMap<>();
        m.put("titulo", "Tratamiento de hipertensión");
        m.put("autores", "García J");
        m.put("revista", "JAMA");
        m.put("anioPub", 2024);
        m.put("mesPub", "Mar");
        m.put("doi", doi);
        m.put("abstractText", "abstract");
        m.put("keywords", "hipertension");
        m.put("tipoPublicacion", "Journal Article");
        m.put("url", "https://doi.org/" + doi);
        return m;
    }

    @Test
    @DisplayName("POST válido devuelve 201 con body completo y Location")
    void createOk() {
        String doi = "IT-" + UUID.randomUUID();
        given()
                .contentType(ContentType.JSON)
                .body(validPayload(doi))
                .when().post("/api/articles")
                .then()
                .statusCode(201)
                .header("Location", containsString("/api/articles/"))
                .body("id", not(emptyOrNullString()))
                .body("titulo", equalTo("Tratamiento de hipertensión"))
                .body("doi", equalTo(doi))
                .body("anioPub", equalTo(2024))
                .body("tags", hasSize(0));
    }

    @Test
    @DisplayName("POST sin titulo devuelve 400")
    void createBlankTitulo() {
        Map<String, Object> p = validPayload("IT-" + UUID.randomUUID());
        p.put("titulo", "  ");
        given().contentType(ContentType.JSON).body(p)
                .when().post("/api/articles")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST con DOI duplicado devuelve 409")
    void createDuplicateDoi() {
        String doi = "IT-DUP-" + UUID.randomUUID();
        given().contentType(ContentType.JSON).body(validPayload(doi))
                .when().post("/api/articles").then().statusCode(201);
        given().contentType(ContentType.JSON).body(validPayload(doi))
                .when().post("/api/articles").then().statusCode(409);
    }

    @Test
    @DisplayName("GET /api/articles devuelve lista")
    void listArticles() {
        given().when().get("/api/articles")
                .then().statusCode(200)
                .body("$", instanceOf(java.util.List.class));
    }

    @Test
    @DisplayName("GET /api/articles/{id-inexistente} devuelve 404")
    void getNotFound() {
        given().when().get("/api/articles/" + UUID.randomUUID())
                .then().statusCode(404);
    }

    @Test
    @DisplayName("Flow agregar tag, listar, eliminar tag")
    void tagFlow() {
        String articleId = given().contentType(ContentType.JSON)
                .body(validPayload("IT-TAG-" + UUID.randomUUID()))
                .when().post("/api/articles")
                .then().statusCode(201)
                .extract().path("id");

        Map<String, Object> tagPayload = new HashMap<>();
        tagPayload.put("tipo", "enfermedad");
        tagPayload.put("valor", "Hipertensión");
        String tagId = given().contentType(ContentType.JSON).body(tagPayload)
                .when().post("/api/articles/" + articleId + "/tags")
                .then().statusCode(201)
                .body("tipo", equalTo("enfermedad"))
                .body("valor", equalTo("Hipertensión"))
                .extract().path("id");

        given().when().get("/api/articles/" + articleId)
                .then().statusCode(200)
                .body("tags", hasSize(1))
                .body("tags[0].id", equalTo(tagId));

        given().when().delete("/api/articles/" + articleId + "/tags/" + tagId)
                .then().statusCode(204);

        given().when().get("/api/articles/" + articleId)
                .then().statusCode(200)
                .body("tags", hasSize(0));
    }

    @Test
    @DisplayName("POST tag con tipo inválido devuelve 400")
    void addTagInvalidTipo() {
        String articleId = given().contentType(ContentType.JSON)
                .body(validPayload("IT-INVTIPO-" + UUID.randomUUID()))
                .when().post("/api/articles")
                .then().statusCode(201)
                .extract().path("id");

        Map<String, Object> p = new HashMap<>();
        p.put("tipo", "otro");
        p.put("valor", "x");
        given().contentType(ContentType.JSON).body(p)
                .when().post("/api/articles/" + articleId + "/tags")
                .then().statusCode(400);
    }

    @Test
    @DisplayName("POST tag a artículo inexistente devuelve 404")
    void addTagArticleNotFound() {
        Map<String, Object> p = new HashMap<>();
        p.put("tipo", "enfermedad");
        p.put("valor", "x");
        given().contentType(ContentType.JSON).body(p)
                .when().post("/api/articles/" + UUID.randomUUID() + "/tags")
                .then().statusCode(404);
    }

    @Test
    @DisplayName("DELETE tag inexistente devuelve 404")
    void removeTagNotFound() {
        String articleId = given().contentType(ContentType.JSON)
                .body(validPayload("IT-RM-" + UUID.randomUUID()))
                .when().post("/api/articles")
                .then().statusCode(201)
                .extract().path("id");

        given().when().delete("/api/articles/" + articleId + "/tags/" + UUID.randomUUID())
                .then().statusCode(404);
    }
}
