package com.tuckersoft.branchengine.service;
import java.text.Normalizer;

public class ClassificationEngine {

    public static String normalize(String input) {
        if (input == null) return "";
        String clean = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return clean.toLowerCase().trim();
    }

    public static ClassificationResult classify(String rawInput) {
        String norm = normalize(rawInput);

        // Regla 1: Aceptación
        if (norm.contains("si") || norm.contains("aceptar") || norm.contains("ok")) {
            return new ClassificationResult("ACCEPTANCE", "UNIT_ALPHA");
        }
        // Regla 2: Rechazo
        if (norm.contains("no") || norm.contains("rechazar") || norm.contains("jamás") || norm.contains("jamas")) {
            return new ClassificationResult("REJECTION", "UNIT_BETA");
        }
        // Regla 3: Violencia/Conflicto
        if (norm.contains("luchar") || norm.contains("atacar") || norm.contains("romper") || norm.contains("gritar")) {
            return new ClassificationResult("CONFLICT", "UNIT_GAMMA");
        }
        // Regla 4: Resignación/Rendición
        if (norm.contains("rendirse") || norm.contains("sumisión") || norm.contains("sumision") || norm.contains("ceder")) {
            return new ClassificationResult("SUBMISSION", "UNIT_DELTA");
        }
        // Regla 5: Fallback / Indeterminado
        return new ClassificationResult("UNKNOWN", "UNIT_OMEGA");
    }

    public record ClassificationResult(String outcomeCode, String handlerUnit) {}
}