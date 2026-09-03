package com.cooperativa.votacao.client;

import com.cooperativa.votacao.exception.AssociadoNaoAptoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Cliente de integração para validação de aptidão de voto do associado.
 *
 * NOTA DE ARQUITETURA:
 * O serviço externo original de validação (https://user-info.herokuapp.com/users/{cpf})
 * encontra-se descontinuado/fora do ar. Para garantir a execução contínua da aplicação
 * e permitir o funcionamento dos testes e regras de negócio, este componente atua
 * como um Mock/Simulador local de integração.
 */
@Component
public class AssociadoValidadorClient {

    private static final Logger log = LoggerFactory.getLogger(AssociadoValidadorClient.class);

    /**
     * Indica se a validação externa está ativa na aplicação.
     * Mantido como true para exercitar a regra de aptidão via simulador local.
     */
    public boolean isHabilitado() {
        return true;
    }

    /**
     * Valida se o associado tem permissão para votar.
     * Caso não esteja apto, lança AssociadoNaoAptoException.
     *
     * @param associadoId Identificador/CPF do associado
     */
    public void validarPodeVotar(String associadoId) {
        AssociadoStatusResponse response = validarCpf(associadoId);

        if (response == null || response.status() == StatusAssociado.UNABLE_TO_VOTE) {
            log.warn("Associado {} não está apto a votar (UNABLE_TO_VOTE).", associadoId);
            throw new AssociadoNaoAptoException("Associado " + associadoId + " não está apto para votar nesta pauta.");
        }
    }

    /**
     * Executa a simulação determinística da validação por CPF.
     */
    public AssociadoStatusResponse validarCpf(String cpf) {
        log.info("Simulando validação externa de aptidão de voto para o CPF/Associado: {}", cpf);

        String cleanCpf = cpf != null ? cpf.replaceAll("\\D", "") : "";

        // Se o identificador não tiver padrão numérico de CPF válido (11 dígitos)
        if (cleanCpf.length() != 11) {
            log.warn("Identificador/CPF com formato inválido: {}", cpf);
            throw new AssociadoNaoAptoException("CPF/Associado inválido ou malformatado para consulta de aptidão.");
        }

        // Regra de mock determinístico por último dígito:
        // - Último dígito PAR (0, 2, 4, 6, 8) -> ABLE_TO_VOTE (Pode votar)
        // - Último dígito ÍMPAR exceto 9 (1, 3, 5, 7) -> UNABLE_TO_VOTE (Não pode votar)
        // - Último dígito 9 -> Simula CPF Inexistente/Não Encontrado
        char lastDigitChar = cleanCpf.charAt(cleanCpf.length() - 1);
        int lastDigit = Character.getNumericValue(lastDigitChar);

        if (lastDigit == 9) {
            log.warn("CPF {} simulado como não encontrado no cadastro.", cleanCpf);
            throw new AssociadoNaoAptoException("CPF/Associado não encontrado no cadastro nacional de eleitores.");
        }

        StatusAssociado status = (lastDigit % 2 == 0)
                ? StatusAssociado.ABLE_TO_VOTE
                : StatusAssociado.UNABLE_TO_VOTE;

        log.info("Validação simulada finalizada para o CPF {}: Status = {}", cleanCpf, status);

        return new AssociadoStatusResponse(status);
    }
}