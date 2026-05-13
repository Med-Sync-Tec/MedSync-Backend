package itesm.medsync.infrastructure.vocabulary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import itesm.medsync.domain.shared.model.TipoClinico;
import itesm.medsync.domain.vocabulary.exception.VocabularyParseException;
import itesm.medsync.domain.vocabulary.model.VocabularyTerm;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Stateless parser for vocabulary JSON files. Validates every schema rule and
 * fails fast on the first violation with a {@link VocabularyParseException} that
 * names the file and the offending field — so a typo at boot produces a log
 * entry that points directly at the line a developer needs to fix.
 *
 * Schema rules (see {@code docs/specs/medical-vocabulary/12-05-2026/design.md}):
 * required {@code especialidadSlug} matching the filename, required {@code version},
 * required {@code terms} object with only the four known {@link TipoClinico} keys,
 * terms in {@code 1..500} chars and non-blank, case-insensitive dedup within each
 * bucket, and an optional {@code $schema} field that must equal
 * {@code "medsync.vocabulary.v1"} when present.
 */
@ApplicationScoped
public class VocabularyJsonReader {

    private static final String SCHEMA_VALUE = "medsync.vocabulary.v1";
    private static final int MAX_VERSION_LENGTH = 50;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Parses {@code jsonBytes} and verifies it matches {@code expectedSlug}.
     *
     * @param expectedSlug the slug taken from the filename (without {@code .json})
     * @param jsonBytes    raw UTF-8 bytes of the file
     * @return the validated intermediate representation
     * @throws VocabularyParseException on any schema violation; the message names
     *                                  the offending field and the file
     */
    public ParsedVocabularyFile parse(String expectedSlug, byte[] jsonBytes) {
        String filename = expectedSlug + ".json";
        JsonNode root;
        try {
            root = mapper.readTree(jsonBytes);
        } catch (JsonProcessingException ex) {
            throw new VocabularyParseException(filename, null, "malformed JSON: " + ex.getOriginalMessage(), ex);
        } catch (java.io.IOException ex) {
            throw new VocabularyParseException(filename, null, "unable to read JSON bytes", ex);
        }

        if (root == null || !root.isObject()) {
            throw new VocabularyParseException(filename, null, "root must be a JSON object");
        }

        if (root.has("$schema")) {
            JsonNode schemaNode = root.get("$schema");
            if (!schemaNode.isTextual() || !SCHEMA_VALUE.equals(schemaNode.asText())) {
                throw new VocabularyParseException(filename, "$schema",
                        "value must be '" + SCHEMA_VALUE + "' (got: " + schemaNode.asText() + ")");
            }
        }

        String slug = requireText(root, "especialidadSlug", filename);
        if (!expectedSlug.equals(slug)) {
            throw new VocabularyParseException(filename, "especialidadSlug",
                    "slug '" + slug + "' does not match filename '" + expectedSlug + "'");
        }

        String version = requireText(root, "version", filename);
        if (version.length() > MAX_VERSION_LENGTH) {
            throw new VocabularyParseException(filename, "version",
                    "version exceeds " + MAX_VERSION_LENGTH + " chars");
        }

        JsonNode termsNode = root.get("terms");
        if (termsNode == null || termsNode.isNull()) {
            throw new VocabularyParseException(filename, "terms", "field is required");
        }
        if (!termsNode.isObject()) {
            throw new VocabularyParseException(filename, "terms", "must be a JSON object");
        }

        Map<TipoClinico, List<String>> termsByType = new EnumMap<>(TipoClinico.class);
        for (TipoClinico tipo : TipoClinico.values()) {
            termsByType.put(tipo, List.of());
        }

        Iterator<Map.Entry<String, JsonNode>> fields = termsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            TipoClinico tipo;
            try {
                tipo = TipoClinico.valueOf(key);
            } catch (IllegalArgumentException ex) {
                throw new VocabularyParseException(filename, "terms." + key,
                        "unknown TipoClinico key '" + key + "' (allowed: ENFERMEDAD, SINTOMA, TRATAMIENTO, MEDICAMENTO)");
            }
            JsonNode arr = entry.getValue();
            if (!arr.isArray()) {
                throw new VocabularyParseException(filename, "terms." + key,
                        "bucket must be a JSON array");
            }
            List<String> bucket = new ArrayList<>(arr.size());
            Map<String, String> seen = new HashMap<>();
            for (int i = 0; i < arr.size(); i++) {
                JsonNode node = arr.get(i);
                if (!node.isTextual()) {
                    throw new VocabularyParseException(filename, "terms." + key + "[" + i + "]",
                            "term must be a string");
                }
                String valor = node.asText();
                if (valor == null || valor.isBlank()) {
                    throw new VocabularyParseException(filename, "terms." + key + "[" + i + "]",
                            "term is blank or whitespace-only");
                }
                if (valor.length() > VocabularyTerm.MAX_VALOR_LENGTH) {
                    throw new VocabularyParseException(filename, "terms." + key + "[" + i + "]",
                            "term exceeds " + VocabularyTerm.MAX_VALOR_LENGTH + " characters");
                }
                // Dedup is case-insensitive and trim-tolerant on the lookup key, but the
                // error message reports both canonical forms so a clinical reviewer can
                // see exactly which two lines collided.
                String normalized = valor.trim().toLowerCase();
                String previous = seen.put(normalized, valor);
                if (previous != null) {
                    throw new VocabularyParseException(filename, "terms." + key,
                            "duplicate term within bucket (case-insensitive): '"
                                    + previous + "' and '" + valor + "'");
                }
                bucket.add(valor);
            }
            termsByType.put(tipo, List.copyOf(bucket));
        }

        return new ParsedVocabularyFile(slug, version, Map.copyOf(termsByType));
    }

    private static String requireText(JsonNode root, String field, String filename) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull() || !node.isTextual() || node.asText().isBlank()) {
            throw new VocabularyParseException(filename, field, "field is required and must be a non-blank string");
        }
        return node.asText();
    }
}
