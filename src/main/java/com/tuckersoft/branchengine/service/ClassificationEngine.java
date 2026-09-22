package com.tuckersoft.branchengine.service;

import com.tuckersoft.branchengine.model.BranchType;

import java.text.Normalizer;

/**
 * Clasifica la decisión del jugador con reglas propias, deterministas: minúsculas y sin tildes,
 * y la primera regla que se cumple gana (el orden importa).
 */
public final class ClassificationEngine {

    private ClassificationEngine() {
    }

    public static String normalize(String rawInput) {
        if (rawInput == null) {
            return "";
        }
        return Normalizer.normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    public static ClassificationResult classify(String rawInput) {
        String texto = normalize(rawInput);

        if (!texto.matches(".*[a-z].*")) {
            return resultFor(BranchType.ENTRADA_CORRUPTA);
        }
        if (contains(texto, "netflix", "camara", "espectador", "videojuego")) {
            return resultFor(BranchType.RUPTURA_CUARTA_PARED);
        }
        if (contains(texto, "vigilan", "simbolo", "conspiracion")) {
            return resultFor(BranchType.SOSPECHA);
        }
        if (contains(texto, "rechaza", "destruye", "desobedece", "renuncia")) {
            return resultFor(BranchType.REBELDIA);
        }
        return resultFor(BranchType.OBEDIENCIA);
    }

    private static boolean contains(String texto, String... needles) {
        for (String needle : needles) {
            if (texto.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static ClassificationResult resultFor(String branchType) {
        return switch (branchType) {
            case BranchType.OBEDIENCIA -> new ClassificationResult(branchType, "Mesa de Guion", "ADVANCE_MAIN_PATH");
            case BranchType.REBELDIA -> new ClassificationResult(branchType, "Control de Continuidad", "FORK_TIMELINE");
            case BranchType.SOSPECHA -> new ClassificationResult(branchType, "Oficina de Seguridad", "INJECT_WHITE_BEAR_SYMBOL");
            case BranchType.RUPTURA_CUARTA_PARED -> new ClassificationResult(branchType, "Departamento Netflix", "BREAK_FOURTH_WALL");
            case BranchType.ENTRADA_CORRUPTA -> new ClassificationResult(branchType, "Archivo de Errores", "DISCARD_INPUT");
            default -> throw new IllegalStateException("branchType desconocido: " + branchType);
        };
    }

    public record ClassificationResult(String branchType, String handlerUnit, String outcomeCode) {
    }
}
