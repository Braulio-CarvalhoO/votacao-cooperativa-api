package com.cooperativa.votacao.controller;

import com.cooperativa.votacao.dto.*;
import com.cooperativa.votacao.model.SessaoVotacao;
import com.cooperativa.votacao.service.VotacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}")
@Tag(name = "Votacao", description = "Abertura de sessao, votos e apuracao de resultado")
public class VotacaoController {

    private final VotacaoService votacaoService;

    public VotacaoController(VotacaoService votacaoService) {
        this.votacaoService = votacaoService;
    }

    @PostMapping("/sessoes")
    @Operation(summary = "Abre uma sessao de votacao para a pauta (default: 1 minuto)")
    public ResponseEntity<SessaoResponse> abrirSessao(
            @PathVariable Long pautaId,
            @RequestBody(required = false) @Valid AbrirSessaoRequest request) {
        SessaoVotacao sessao = votacaoService.abrirSessao(pautaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(SessaoResponse.from(sessao));
    }

    @PostMapping("/votos")
    @Operation(summary = "Recebe o voto (SIM/NAO) de um associado na pauta")
    public ResponseEntity<Void> votar(@PathVariable Long pautaId, @Valid @RequestBody VotoRequest request) {
        votacaoService.votar(pautaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/resultado")
    @Operation(summary = "Contabiliza os votos e retorna o resultado da pauta")
    public ResponseEntity<ResultadoResponse> resultado(@PathVariable Long pautaId) {
        return ResponseEntity.ok(votacaoService.apurar(pautaId));
    }
}