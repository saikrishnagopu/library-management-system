package com.example.library.util;

public final class IsbnUtil {

    private IsbnUtil() {}

    /** Normalizes ISBN for storage and uniqueness (strip hyphens/spaces, uppercase). */
    public static String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.replace("-", "").replace(" ", "").trim().toUpperCase();
    }
}
