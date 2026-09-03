package com.cooperativa.votacao.dto;

public record ResultadoResponse(
        Long pautaId,
        String titulo,
        boolean sessaoEncerrada,
        long totalVotosSim,
        long totalVotosNao,
        long totalVotos,
        String resultado
) {
}