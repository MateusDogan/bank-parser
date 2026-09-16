package com.bankparser.dto;

import jakarta.validation.constraints.NotBlank;

/** Entrada do cadastro de cliente. */
public record ClientRequest(
        @NotBlank(message = "Nome e obrigatorio") String name,
        @NotBlank(message = "Documento e obrigatorio") String document
) {
}
