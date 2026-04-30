package itesm.medsync.infrastructure.persistence.article;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import itesm.medsync.domain.article.model.Article;
import itesm.medsync.domain.article.model.ArticleTag;
import itesm.medsync.domain.article.repository.ArticleRepository;
import itesm.medsync.domain.pacientecontexto.model.PacienteContexto;
import itesm.medsync.domain.pacientecontexto.repository.PacienteContextoRepository;
import itesm.medsync.domain.patient.model.Patient;
import itesm.medsync.domain.patient.repository.PatientRepository;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ArticleRepositoryImplTest {

    @Inject
    ArticleRepository repository;

    @Inject
    PacienteContextoRepository contextoRepository;

    @Inject
    PatientRepository patientRepository;

    @Inject
    UserRepository userRepository;

    @Inject
    RoleRepository roleRepository;

    private Article newArticle(String doi) {
        return Article.create(
                "Tratamiento de hipertensión", "García J", "JAMA", 2024, "Mar",
                doi, "abstract", "hipertension", "Journal Article",
                "https://doi.org/" + (doi == null ? "x" : doi));
    }

    @Test
    @TestTransaction
    @DisplayName("save inserta y findByUuid usa EntityGraph para traer tags")
    void saveAndFindByUuid() {
        Article article = newArticle("10.1234/save").withTagAdded(
                ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión"));

        Article saved = repository.save(article);
        assertNotNull(saved.getCreatedAt());
        assertEquals(1, saved.getTags().size());

        Optional<Article> found = repository.findByUuid(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(1, found.get().getTags().size());
        assertEquals("Hipertensión", found.get().getTags().get(0).getValor());
    }

    @Test
    @TestTransaction
    @DisplayName("save sobre existente reconcilia tags (mantiene, agrega y elimina)")
    void saveReconcilesTags() {
        ArticleTag t1 = ArticleTag.create(TipoClinico.ENFERMEDAD, "HTA");
        ArticleTag t2 = ArticleTag.create(TipoClinico.SINTOMA, "Mareo");

        Article original = repository.save(
                newArticle("10.1234/recon").withTagAdded(t1).withTagAdded(t2));

        ArticleTag t3 = ArticleTag.create(TipoClinico.MEDICAMENTO, "Losartán");
        Article updated = original.withTagRemoved(t2.getId()).withTagAdded(t3);

        Article afterSave = repository.save(updated);

        assertEquals(2, afterSave.getTags().size());
        assertTrue(afterSave.getTags().stream().anyMatch(t -> t.getId().equals(t1.getId())));
        assertTrue(afterSave.getTags().stream().anyMatch(t -> t.getId().equals(t3.getId())));
        assertFalse(afterSave.getTags().stream().anyMatch(t -> t.getId().equals(t2.getId())));
    }

    @Test
    @TestTransaction
    @DisplayName("listAllArticles devuelve todos con tags cargados")
    void listAllWithTags() {
        repository.save(newArticle("10.1234/a").withTagAdded(
                ArticleTag.create(TipoClinico.ENFERMEDAD, "X")));
        repository.save(newArticle("10.1234/b"));

        List<Article> all = repository.listAllArticles();
        assertTrue(all.size() >= 2);
    }

    @Test
    @TestTransaction
    @DisplayName("existsByDoi true para DOI persistido")
    void existsByDoi() {
        repository.save(newArticle("10.1234/exists"));
        assertTrue(repository.existsByDoi("10.1234/exists"));
        assertFalse(repository.existsByDoi("nope"));
    }

    @Test
    @TestTransaction
    @DisplayName("findMatchingArticlesForPaciente devuelve solo artículos cuyas tags coinciden con paciente_contexto")
    void crossQueryWorks() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medico = userRepository.save(
                User.create("M", "match-" + UUID.randomUUID() + "@tec.mx", doctorRoleId));
        Patient paciente = patientRepository.save(
                Patient.create("EXP-MATCH-" + UUID.randomUUID(),
                        "P", LocalDate.of(1990, 1, 1), "F", medico.getId()));

        contextoRepository.save(PacienteContexto.create(
                paciente.getId(), TipoClinico.ENFERMEDAD, "Hipertensión"));
        contextoRepository.save(PacienteContexto.create(
                paciente.getId(), TipoClinico.MEDICAMENTO, "Losartán"));

        Article matching1 = repository.save(
                newArticle("10.1234/match1").withTagAdded(
                        ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión")));
        Article matching2 = repository.save(
                newArticle("10.1234/match2").withTagAdded(
                        ArticleTag.create(TipoClinico.MEDICAMENTO, "Losartán")));
        Article noMatch = repository.save(
                newArticle("10.1234/nope").withTagAdded(
                        ArticleTag.create(TipoClinico.SINTOMA, "Mareo")));

        List<Article> results = repository.findMatchingArticlesForPaciente(paciente.getId());

        List<UUID> ids = results.stream().map(Article::getId).toList();
        assertTrue(ids.contains(matching1.getId()));
        assertTrue(ids.contains(matching2.getId()));
        assertFalse(ids.contains(noMatch.getId()));
    }

    @Test
    @TestTransaction
    @DisplayName("findMatchingArticlesForPaciente sin contextos devuelve lista vacía")
    void crossQueryEmpty() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medico = userRepository.save(
                User.create("M", "empty-" + UUID.randomUUID() + "@tec.mx", doctorRoleId));
        Patient paciente = patientRepository.save(
                Patient.create("EXP-EMPTY-" + UUID.randomUUID(),
                        "P", LocalDate.of(1990, 1, 1), "F", medico.getId()));

        repository.save(newArticle("10.1234/lonely").withTagAdded(
                ArticleTag.create(TipoClinico.ENFERMEDAD, "X")));

        List<Article> results = repository.findMatchingArticlesForPaciente(paciente.getId());
        assertTrue(results.isEmpty());
    }
}
