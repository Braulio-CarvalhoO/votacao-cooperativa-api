package com.cooperativa.votacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "votacao.sessao")
public record VotacaoSessaoProperties(
        int duracaoPadraoSegundos
) {
}