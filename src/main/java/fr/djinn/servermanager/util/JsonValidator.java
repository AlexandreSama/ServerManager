package fr.djinn.servermanager.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonValidator {

    private final Map<String, String> data;
    private final List<String> errors = new ArrayList<>();
    private String lastKey;

    public JsonValidator(Map<String, String> data) {
        this.data = data != null ? data : new HashMap<>();
    }

    public JsonValidator require(String key) {
        this.lastKey = key;
        if (!data.containsKey(key) || data.get(key).trim().isEmpty()) {
            errors.add("Champ requis manquant ou vide : '" + key + "'");
        }
        return this;
    }

    public JsonValidator optional(String key) {
        this.lastKey = key;
        return this;
    }

    public JsonValidator asBoolean() {
        if (lastKey != null && data.containsKey(lastKey)) {
            String value = data.get(lastKey).toLowerCase();
            if (!value.equals("true") && !value.equals("false")) {
                errors.add("Le champ '" + lastKey + "' doit être un booléen (true/false)");
            }
        }
        return this;
    }

    public JsonValidator matches(String regex) {
        if (lastKey != null && data.containsKey(lastKey)) {
            String value = data.get(lastKey);
            if (!value.matches(regex)) {
                errors.add("Le champ '" + lastKey + "' ne respecte pas le format attendu");
            }
        }
        return this;
    }

    public JsonValidator require(String key, boolean asBoolean) {
        require(key);
        if (asBoolean) asBoolean();
        return this;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> getErrors() {
        return errors;
    }

    public String getErrorSummary() {
        return String.join("\n", errors);
    }

    public JsonValidator optional(String key, boolean asBoolean) {
        this.lastKey = key;
        if (asBoolean && data.containsKey(key)) {
            String value = data.get(key).toLowerCase();
            if (!value.equals("true") && !value.equals("false")) {
                errors.add("Le champ optionnel '" + key + "' doit être un booléen (true/false)");
            }
        }
        return this;
    }

    public JsonValidator optional(String key, String type) {
        if (type.equalsIgnoreCase("boolean")) {
            return this.optional(key, true);
        }
        return this;
    }
}
