package com.cooperativa.votacao.dto;

import com.cooperativa.votacao.model.Pauta;

import java.time.LocalDateTime;

public record PautaResponse(
        Long id,
        String titulo,
        String descricao,
        LocalDateTime dataCriacao,
        SessaoResponse sessaoVotacao
) {
    public static PautaResponse from(Pauta pauta) {
        SessaoResponse sessao = pauta.getSessaoVotacao() == null
                ? null
                : SessaoResponse.from(pauta.getSessaoVotacao());
        return new PautaResponse(
                pauta.getId(),
                pauta.getTitulo(),
                pauta.getDescricao(),
                pauta.getDataCriacao(),
                sessao
        );
    }
}