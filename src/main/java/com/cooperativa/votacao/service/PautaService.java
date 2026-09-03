package com.cooperativa.votacao.service;

import com.cooperativa.votacao.dto.PautaRequest;
import com.cooperativa.votacao.exception.RecursoNaoEncontradoException;
import com.cooperativa.votacao.model.Pauta;
import com.cooperativa.votacao.repository.PautaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PautaService {

    private static final Logger log = LoggerFactory.getLogger(PautaService.class);

    private final PautaRepository pautaRepository;

    public PautaService(PautaRepository pautaRepository) {
        this.pautaRepository = pautaRepository;
    }

    @Transactional
    public Pauta cadastrar(PautaRequest request) {
        Pauta pauta = new Pauta();
        pauta.setTitulo(request.titulo());
        pauta.setDescricao(request.descricao());
        Pauta salva = pautaRepository.save(pauta);
        log.info("Pauta cadastrada: id={} titulo='{}'", salva.getId(), salva.getTitulo());
        return salva;
    }

    @Transactional(readOnly = true)
    public Pauta buscarPorId(Long id) {
        return pautaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pauta " + id + " nao encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Pauta> listar() {
        return pautaRepository.findAll();
    }
}