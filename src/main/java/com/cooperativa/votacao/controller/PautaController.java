package com.cooperativa.votacao.controller;

import com.cooperativa.votacao.dto.PautaRequest;
import com.cooperativa.votacao.dto.PautaResponse;
import com.cooperativa.votacao.model.Pauta;
import com.cooperativa.votacao.service.PautaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pautas")
@Tag(name = "Pautas", description = "Cadastro e consulta de pautas de votacao")
public class PautaController {

    private final PautaService pautaService;

    public PautaController(PautaService pautaService) {
        this.pautaService = pautaService;
    }

    @PostMapping
    @Operation(summary = "Cadastra uma nova pauta")
    public ResponseEntity<PautaResponse> cadastrar(@Valid @RequestBody PautaRequest request) {
        Pauta pauta = pautaService.cadastrar(request);
        return ResponseEntity
                .created(URI.create("/api/v1/pautas/" + pauta.getId()))
                .body(PautaResponse.from(pauta));
    }

    @GetMapping
    @Operation(summary = "Lista todas as pautas cadastradas")
    public ResponseEntity<List<PautaResponse>> listar() {
        List<PautaResponse> pautas = pautaService.listar().stream()
                .map(PautaResponse::from)
                .toList();
        return ResponseEntity.ok(pautas);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma pauta pelo id")
    public ResponseEntity<PautaResponse> buscarPorId(@PathVariable Long id) {
        Pauta pauta = pautaService.buscarPorId(id);
        return ResponseEntity.ok(PautaResponse.from(pauta));
    }
}