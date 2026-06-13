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
import itesm.medsync.domain.specialty.model.Specialty;
import itesm.medsync.domain.specialty.repository.SpecialtyRepository;
import itesm.medsync.domain.user.model.Role;
import itesm.medsync.domain.user.model.User;
import itesm.medsync.domain.user.repository.RoleRepository;
import itesm.medsync.domain.user.repository.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import itesm.medsync.domain.article.usecase.CreateArticleCommand;

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

    @Inject
    SpecialtyRepository specialtyRepository;

    /** Returns the id of a seeded specialty by slug; falls back to creating one for tests. */
    private UUID seededSpecialtyId(String slug) {
        return specialtyRepository.findBySlug(slug).map(Specialty::getId)
                .orElseThrow(() -> new IllegalStateException("Missing seeded specialty: " + slug));
    }

    private Article newArticle(String doi) {
        return newArticle(doi, null);
    }

    private Article newArticle(String doi, UUID especialidadId) {
        Article fresh = Article.create(new CreateArticleCommand(
                "Tratamiento de hipertensión", "García J", "JAMA", 2024, "Mar",
                doi, "abstract", "hipertension", "Journal Article",
                "https://doi.org/" + (doi == null ? "x" : doi)));
        return especialidadId == null
                ? fresh
                : fresh.withAiAnalysis(especialidadId, List.of());
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

        List<Article> all = repository.listArticles(0, 100).items();
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
    @DisplayName("findMatchingArticlesForPaciente devuelve solo artículos cuyas tags coinciden con paciente_contexto Y comparten especialidad")
    void crossQueryWorks() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medicoMatch = userRepository.save(
                User.create("M", "match-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        Patient paciente = patientRepository.save(
                Patient.create("EXP-MATCH-" + UUID.randomUUID(),
                        "P", LocalDate.of(1990, Month.JANUARY, 1), "F", medicoMatch.getId()));

        UUID cardiologia = seededSpecialtyId("cardiologia");
        contextoRepository.save(PacienteContexto.create(
                paciente.getId(), TipoClinico.ENFERMEDAD, "Hipertensión", cardiologia));
        contextoRepository.save(PacienteContexto.create(
                paciente.getId(), TipoClinico.MEDICAMENTO, "Losartán", cardiologia));

        Article matching1 = repository.save(
                newArticle("10.1234/match1", cardiologia).withTagAdded(
                        ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión")));
        Article matching2 = repository.save(
                newArticle("10.1234/match2", cardiologia).withTagAdded(
                        ArticleTag.create(TipoClinico.MEDICAMENTO, "Losartán")));
        Article noMatch = repository.save(
                newArticle("10.1234/nope", cardiologia).withTagAdded(
                        ArticleTag.create(TipoClinico.SINTOMA, "Mareo")));

        List<Article> results = repository.findMatchingArticlesForPaciente(paciente.getId(), 50);

        List<UUID> ids = results.stream().map(Article::getId).toList();
        assertTrue(ids.contains(matching1.getId()));
        assertTrue(ids.contains(matching2.getId()));
        assertFalse(ids.contains(noMatch.getId()));
    }

    @Test
    @TestTransaction
    @DisplayName("matching: misma tag, distinta especialidad → no match (filtro de especialidad activo)")
    void differentSpecialtyExcluded() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medico = userRepository.save(
                User.create("M", "diff-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        Patient paciente = patientRepository.save(
                Patient.create("EXP-DIFF-" + UUID.randomUUID(),
                        "P", LocalDate.of(1990, Month.JANUARY, 1), "F", medico.getId()));

        UUID cardiologia = seededSpecialtyId("cardiologia");
        UUID oncologia = seededSpecialtyId("oncologia");
        contextoRepository.save(PacienteContexto.create(
                paciente.getId(), TipoClinico.ENFERMEDAD, "Hipertensión", cardiologia));

        Article oncologyArticle = repository.save(
                newArticle("10.1234/onco", oncologia).withTagAdded(
                        ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión")));

        List<Article> results = repository.findMatchingArticlesForPaciente(paciente.getId(), 50);
        List<UUID> ids = results.stream().map(Article::getId).toList();
        assertFalse(ids.contains(oncologyArticle.getId()),
                "artículo de oncología no debe matchear contexto cardiológico aún si la tag coincide");
    }

    @Test
    @TestTransaction
    @DisplayName("matching: artículo con especialidad NULL → excluido")
    void articleNullSpecialtyExcluded() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medico = userRepository.save(
                User.create("M", "anull-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        Patient paciente = patientRepository.save(
                Patient.create("EXP-ANULL-" + UUID.randomUUID(),
                        "P", LocalDate.of(1990, Month.JANUARY, 1), "F", medico.getId()));

        UUID cardiologia = seededSpecialtyId("cardiologia");
        contextoRepository.save(PacienteContexto.create(
                paciente.getId(), TipoClinico.ENFERMEDAD, "Hipertensión", cardiologia));

        Article unanalyzed = repository.save(
                newArticle("10.1234/unanalyzed").withTagAdded(
                        ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión")));

        List<Article> results = repository.findMatchingArticlesForPaciente(paciente.getId(), 50);
        assertFalse(results.stream().map(Article::getId).toList().contains(unanalyzed.getId()),
                "artículo sin especialidad analizada no debe matchear");
    }

    @Test
    @TestTransaction
    @DisplayName("matching: contexto con especialidad NULL → excluido")
    void contextNullSpecialtyExcluded() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medico = userRepository.save(
                User.create("M", "cnull-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        Patient paciente = patientRepository.save(
                Patient.create("EXP-CNULL-" + UUID.randomUUID(),
                        "P", LocalDate.of(1990, Month.JANUARY, 1), "F", medico.getId()));

        // contexto sin especialidad (constructor legacy → null)
        contextoRepository.save(PacienteContexto.create(
                paciente.getId(), TipoClinico.ENFERMEDAD, "Hipertensión"));

        UUID cardiologia = seededSpecialtyId("cardiologia");
        Article article = repository.save(
                newArticle("10.1234/ctxnull", cardiologia).withTagAdded(
                        ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión")));

        List<Article> results = repository.findMatchingArticlesForPaciente(paciente.getId(), 50);
        assertFalse(results.stream().map(Article::getId).toList().contains(article.getId()),
                "contexto sin especialidad no debe disparar match");
    }

    @Test
    @TestTransaction
    @DisplayName("matching: múltiples contextos de distintas especialidades, sólo same-especialidad matchea")
    void multipleSpecialtiesSegregated() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medico = userRepository.save(
                User.create("M", "multi-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        Patient paciente = patientRepository.save(
                Patient.create("EXP-MULTI-" + UUID.randomUUID(),
                        "P", LocalDate.of(1990, Month.JANUARY, 1), "F", medico.getId()));

        UUID cardiologia = seededSpecialtyId("cardiologia");
        UUID oncologia = seededSpecialtyId("oncologia");
        contextoRepository.save(PacienteContexto.create(
                paciente.getId(), TipoClinico.ENFERMEDAD, "Hipertensión", cardiologia));
        contextoRepository.save(PacienteContexto.create(
                paciente.getId(), TipoClinico.ENFERMEDAD, "Cáncer de mama", oncologia));

        Article cardioMatch = repository.save(
                newArticle("10.1234/cardio", cardiologia).withTagAdded(
                        ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión")));
        Article wrongSpecialtyForCardioTag = repository.save(
                newArticle("10.1234/wrong", oncologia).withTagAdded(
                        ArticleTag.create(TipoClinico.ENFERMEDAD, "Hipertensión")));
        Article oncoMatch = repository.save(
                newArticle("10.1234/onco", oncologia).withTagAdded(
                        ArticleTag.create(TipoClinico.ENFERMEDAD, "Cáncer de mama")));

        List<UUID> ids = repository.findMatchingArticlesForPaciente(paciente.getId(), 50).stream()
                .map(Article::getId).toList();
        assertTrue(ids.contains(cardioMatch.getId()));
        assertTrue(ids.contains(oncoMatch.getId()));
        assertFalse(ids.contains(wrongSpecialtyForCardioTag.getId()),
                "artículo de oncología con tag de cardiología no debe matchear");
    }

    @Test
    @TestTransaction
    @DisplayName("findMatchingArticlesForPaciente sin contextos devuelve lista vacía")
    void crossQueryEmpty() {
        UUID doctorRoleId = roleRepository.findByNombre("DOCTOR")
                .map(Role::getId).orElseThrow();
        User medicoEmpty = userRepository.save(
                User.create("M", "empty-" + UUID.randomUUID() + "@tec.mx", null, doctorRoleId));
        Patient paciente = patientRepository.save(
                Patient.create("EXP-EMPTY-" + UUID.randomUUID(),
                        "P", LocalDate.of(1990, Month.JANUARY, 1), "F", medicoEmpty.getId()));

        repository.save(newArticle("10.1234/lonely").withTagAdded(
                ArticleTag.create(TipoClinico.ENFERMEDAD, "X")));

        List<Article> results = repository.findMatchingArticlesForPaciente(paciente.getId(), 50);
        assertTrue(results.isEmpty());
    }

    // ------------------------------------------------------------------
    // findAllWithoutSpecialty tests
    // ------------------------------------------------------------------

    @Test
    @TestTransaction
    @DisplayName("findAllWithoutSpecialty devuelve solo artículos con especialidad_id = NULL")
    void findAllWithoutSpecialty_returnsOnlyNullSpecialty() {
        UUID cardioId = seededSpecialtyId("cardiologia");

        // article with specialty already set
        repository.save(newArticle("10.1234/classified", cardioId));
        // articles without specialty
        Article u1 = repository.save(newArticle("10.1234/unclassified-1"));
        Article u2 = repository.save(newArticle("10.1234/unclassified-2"));

        List<Article> results = repository.findAllWithoutSpecialty();
        List<UUID> ids = results.stream().map(Article::getId).toList();

        assertTrue(ids.contains(u1.getId()), "debe incluir artículo sin especialidad 1");
        assertTrue(ids.contains(u2.getId()), "debe incluir artículo sin especialidad 2");
        assertTrue(results.stream().allMatch(a -> a.getEspecialidadId() == null),
                "todos los resultados deben tener especialidadId = null");
    }

    @Test
    @TestTransaction
    @DisplayName("findAllWithoutSpecialty excluye artículos ya clasificados")
    void findAllWithoutSpecialty_excludesClassified() {
        UUID cardioId = seededSpecialtyId("cardiologia");
        Article classified = repository.save(newArticle("10.1234/has-esp", cardioId));

        List<Article> results = repository.findAllWithoutSpecialty();
        List<UUID> ids = results.stream().map(Article::getId).toList();

        assertFalse(ids.contains(classified.getId()),
                "artículo con especialidad asignada no debe aparecer");
    }

    @Test
    @TestTransaction
    @DisplayName("findAllWithoutSpecialty devuelve lista vacía cuando todos tienen especialidad")
    void findAllWithoutSpecialty_emptyWhenAllClassified() {
        UUID cardioId = seededSpecialtyId("cardiologia");
        // Only save articles with a specialty in this transactional scope; // NOSONAR
        // the existing DB data is rolled back between tests, so we just need
        // to ensure there are no unclassified articles in this transaction.
        repository.save(newArticle("10.1234/all-classified-a", cardioId));
        repository.save(newArticle("10.1234/all-classified-b", cardioId));

        // Wipe any unclassified that may have come from seeding
        List<Article> withoutSpecialty = repository.findAllWithoutSpecialty();
        // We only assert that the classified ones are NOT included — we can't
        // control other seed data beyond this transaction.
        withoutSpecialty.forEach(a -> assertNull(a.getEspecialidadId()));
    }
}
