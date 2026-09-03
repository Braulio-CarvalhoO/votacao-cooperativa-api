package com.cooperativa.votacao.dto;

import jakarta.validation.constraints.Positive;

/**
 * Tempo de duracao da sessao em segundos. Se nao informado (ou nulo),
 * o servico aplica o valor padrao configurado (1 minuto por default).
 */
public record AbrirSessaoRequest(
        @Positive(message = "duracaoSegundos deve ser positivo")
        Integer duracaoSegundos
) {
}