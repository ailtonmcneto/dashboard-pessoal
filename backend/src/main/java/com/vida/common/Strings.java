package com.vida.common;

import org.jspecify.annotations.Nullable;

public final class Strings {

    private Strings() {
    }

    /** Texto opcional vindo do usuário: espaços em volta somem e vazio vira null. */
    public static @Nullable String clean(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
