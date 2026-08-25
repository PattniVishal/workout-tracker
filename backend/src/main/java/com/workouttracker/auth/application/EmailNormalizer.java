package com.workouttracker.auth.application;

import java.util.Locale;

/**
 * Normalizes email addresses before persistence and lookup.
 * The database stores lowercase emails per database-design.md.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
