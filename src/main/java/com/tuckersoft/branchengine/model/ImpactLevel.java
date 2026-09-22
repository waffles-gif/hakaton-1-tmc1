package com.tuckersoft.branchengine.model;

import java.util.Set;

public final class ImpactLevel {

    public static final String LEVE = "LEVE";
    public static final String MODERADO = "MODERADO";
    public static final String GRAVE = "GRAVE";
    public static final String CRITICO = "CRITICO";

    private static final Set<String> VALID = Set.of(LEVE, MODERADO, GRAVE, CRITICO);

    private ImpactLevel() {
    }

    public static boolean isValid(String value) {
        return value != null && VALID.contains(value);
    }
}
