package com.cooperativa.votacao.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class VotacaoFluxoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void devePermitirCicloCompletoDeVotacao() throws Exception {
        // 1. cadastra a pauta
        String pautaJson = """
                {"titulo": "Aprovar novo estatuto", "descricao": "Votacao sobre alteracao do estatuto social"}
                """;

        String responseCriacao = mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pautaJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo", is("Aprovar novo estatuto")))
                .andReturn().getResponse().getContentAsString();

        Long pautaId = objectMapper.readTree(responseCriacao).get("id").asLong();

        // 2. abre a sessao com 1 segundo de duracao para o teste ser rapido
        mockMvc.perform(post("/api/v1/pautas/" + pautaId + "/sessoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoSegundos\": 1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.aberta", is(true)));

        // 3. dois associados votam
        mockMvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\": \"11111111111\", \"voto\": \"SIM\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\": \"22222222222\", \"voto\": \"NAO\"}"))
                .andExpect(status().isCreated());

        // 4. o mesmo associado nao pode votar de novo
        mockMvc.perform(post("/api/v1/pautas/" + pautaId + "/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\": \"11111111111\", \"voto\": \"NAO\"}"))
                .andExpect(status().isUnprocessableEntity());

        // 5. aguarda a sessao fechar e apura o resultado
        Thread.sleep(1200);

        mockMvc.perform(get("/api/v1/pautas/" + pautaId + "/resultado"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotosSim", is(1)))
                .andExpect(jsonPath("$.totalVotosNao", is(1)))
                .andExpect(jsonPath("$.sessaoEncerrada", is(true)))
                .andExpect(jsonPath("$.resultado", is("EMPATE")));
    }

    @Test
    void deveRetornar404AoVotarEmPautaInexistente() throws Exception {
        mockMvc.perform(post("/api/v1/pautas/99999/votos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\": \"11111111111\", \"voto\": \"SIM\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveRetornar400ParaPautaSemTitulo() throws Exception {
        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\": \"sem titulo\"}"))
                .andExpect(status().isBadRequest());
    }
}