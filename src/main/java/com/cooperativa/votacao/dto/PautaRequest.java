package com.cooperativa.votacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PautaRequest(
        @NotBlank(message = "titulo e obrigatorio")
        @Size(max = 120, message = "titulo deve ter no maximo 120 caracteres")
        String titulo,

        @Size(max = 1000, message = "descricao deve ter no maximo 1000 caracteres")
        String descricao
) {
}