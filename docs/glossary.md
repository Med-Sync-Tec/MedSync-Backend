# Glossary

This glossary is **bilingual (EN ↔ ES)** because MedSync's clinical domain and the external hospital database use Spanish terminology, while our engineering documentation is in English.

When in doubt about whether a term should be translated in code or kept in Spanish, use this glossary. The default rule: **technical identifiers in English, clinical/business terms keep their canonical Spanish form if that's what the hospital schema uses**.

## Clinical terms

| Español             | English                | Definition                                                                       |
|---------------------|------------------------|----------------------------------------------------------------------------------|
| Paciente            | Patient                | Person receiving care. In MedSync, identified by a UUID + an external `expedienteExternoId`. |
| Médico              | Doctor / physician     | User with the `DOCTOR` role. Treats patients; can be referenced by `medicoId` on a `Patient`. |
| Expediente clínico  | Clinical record / chart | Hospital-owned record that aggregates a patient's clinical history. Lives in the hospital DB. |
| Consulta            | Medical consultation / visit | A single doctor-patient encounter, stored in SOAP format (Subjective, Objective, Assessment, Plan) + prescription + diagnosis. |
| Contexto clínico    | Clinical context       | A tagged attribute of a patient (`ENFERMEDAD`, `SINTOMA`, `TRATAMIENTO`, `MEDICAMENTO`) used to match scientific articles. Lives in MedSync's `paciente_contexto` table. |
| Diagnóstico         | Diagnosis              | A clinician's conclusion from a consultation. Free-text field on `consulta`.     |
| Prescripción        | Prescription           | Medications prescribed during a consultation. Free-text field on `consulta` today; may be structured later. |
| Subjetivo / Objetivo / Evaluación / Plan | Subjective / Objective / Assessment / Plan | The four SOAP sections of a consultation note. |
| Motivo de consulta  | Chief complaint        | Why the patient came in. Free-text field on `consulta`.                         |
| Alergia             | Allergy                | A `contexto_clinico` of type `ENFERMEDAD` (subcategory tracked by value). Not yet a first-class concept. |

## Pharmacovigilance terms

| Español             | English                | Definition                                                                       |
|---------------------|------------------------|----------------------------------------------------------------------------------|
| Farmacovigilancia   | Pharmacovigilance      | The science of detecting, assessing, and preventing adverse effects of medications. The core mission of MedSync. |
| Medicamento         | Medication / drug      | A pharmaceutical product. Has a `nombre`, a `MedicamentoEstado`, and optional `descripcion`. |
| Estado del medicamento | Medication status   | One of `VIGENTE` (currently approved), `EN_REVISION` (under reevaluation), `OBSOLETO` (withdrawn / discontinued). Stored as a row in `medicamento_estados`. |
| Vigente             | Active / approved      | A medication currently approved for clinical use.                               |
| En revisión         | Under review           | A medication being reevaluated due to new evidence of risk.                     |
| Obsoleto            | Obsolete / withdrawn   | A medication withdrawn from clinical use (e.g. Rofecoxib, Cloranfenicol).       |
| Alerta              | Alert                  | A notification raised when a patient's clinical context intersects with new evidence (article tags) or an obsolete medication. Planned feature, not yet built. |
| Artículo científico | Scientific article     | A peer-reviewed publication, typically sourced from PubMed. Has metadata (title, authors, DOI) and clinical tags. |
| PubMed              | PubMed                 | NCBI's database of biomedical literature. We sync articles via its E-utilities API. |
| Tag                 | Tag                    | A `(TipoClinico, valor)` pair attached to an article or a patient's context. Drives the matching algorithm. |
| TipoClinico         | Clinical type          | Enum: `ENFERMEDAD`, `SINTOMA`, `TRATAMIENTO`, `MEDICAMENTO`. Categorizes tags. Lives in `domain/shared/`. |

## Identifiers

| Español                  | English                       | Notes                                              |
|--------------------------|-------------------------------|----------------------------------------------------|
| `id`                     | id                            | Always `UUID` in MedSync, stored as `BINARY(16)`.  |
| `expedienteExternoId`    | external chart id             | The hospital-side identifier for a patient's chart. Bridges MedSync's `Patient` and the hospital's `expediente_clinico`. |
| `pacienteExternoId`      | external patient id           | The hospital-side identifier for a patient (column on `expediente_clinico`). Matches `Patient.expedienteExternoId` by convention (no DB-level FK because they live in separate databases). |
| `medicoId`               | doctor id                     | UUID of a `User` with the `DOCTOR` role.           |
| `pacienteId`             | patient id                    | UUID of a `Patient`.                               |
| `doctorResponsableId`    | responsible doctor id         | Optional doctor on a hospital `expediente_clinico` (varchar, hospital-owned). |

## System / role terms

| Term                | Definition                                                                       |
|---------------------|----------------------------------------------------------------------------------|
| `COO`               | Chief Operating Officer. Only role allowed to create other users via the admin endpoint. |
| `DOCTOR`            | Default role assigned on first login. Has read access to assigned patients.     |
| `CMO`               | Chief Medical Officer. Reserved for future authorization rules.                 |
| MedSync             | Our system. The product.                                                         |
| Hospital DB         | The external MySQL database we read from (and write `consulta` to). We do not own its schema. |

## Why we don't translate everything

The hospital's database uses Spanish identifiers (`paciente_externo_id`, `expediente_clinico`, `consulta`). Translating column names in our code would force every join, every entity, every JPQL query to do a mental translation — and the SQL we'd write at the DB CLI wouldn't match.

So:

- **Java packages, classes, methods, variables** → English (industry standard, AI-tooling-friendly).
- **Database column names, hospital-side identifiers, clinical concepts** → Spanish (matches the source of truth).
- **Documentation** → English, except this glossary.
- **User-facing strings** (eventual) → Spanish (target users are Spanish-speaking clinicians).
