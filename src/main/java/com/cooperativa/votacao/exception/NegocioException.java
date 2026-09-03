package com.cooperativa.votacao.exception;

/**
 * Excecao generica para violacoes de regra de negocio (ex: sessao ja aberta,
 * sessao fechada, voto duplicado, associado nao habilitado a votar).
 */
public class NegocioException extends RuntimeException {
    public NegocioException(String message) {
        super(message);
    }
}