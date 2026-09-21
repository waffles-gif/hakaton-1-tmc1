package com.tuckersoft.branchengine.model;

public final class Roles {

    public static final String USER = "ROLE_USER";
    public static final String ADMIN = "ROLE_ADMIN";

    private Roles() {
    }

    public static boolean isValid(String role) {
        return USER.equals(role) || ADMIN.equals(role);
    }
}
