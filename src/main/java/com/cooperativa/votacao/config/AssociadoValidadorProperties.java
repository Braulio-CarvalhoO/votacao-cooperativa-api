package com.cooperativa.votacao.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "votacao.associado-validador")
public record AssociadoValidadorProperties(
        boolean habilitado,
        String baseUrl
) {
}