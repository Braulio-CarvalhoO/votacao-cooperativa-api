package com.cooperativa.votacao.dto;

import com.cooperativa.votacao.model.OpcaoVoto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VotoRequest(
        @NotBlank(message = "associadoId e obrigatorio")
        String associadoId,

        @NotNull(message = "voto e obrigatorio (SIM ou NAO)")
        OpcaoVoto voto
) {
}