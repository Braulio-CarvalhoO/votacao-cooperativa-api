package com.cooperativa.votacao.dto;

import com.cooperativa.votacao.model.SessaoVotacao;

import java.time.LocalDateTime;

public record SessaoResponse(
        Long id,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        boolean aberta
) {
    public static SessaoResponse from(SessaoVotacao sessao) {
        return new SessaoResponse(
                sessao.getId(),
                sessao.getDataAbertura(),
                sessao.getDataFechamento(),
                sessao.isAberta()
        );
    }
}