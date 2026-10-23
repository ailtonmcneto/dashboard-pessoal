package com.vida.common;

/**
 * Recurso inexistente <em>ou de outro usuário</em> — os dois casos respondem 404,
 * para não revelar que o id existe.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource) {
        super(resource + " não encontrado");
    }
}
