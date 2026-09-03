package com.cooperativa.votacao.service;

import com.cooperativa.votacao.client.AssociadoValidadorClient;
import com.cooperativa.votacao.config.VotacaoSessaoProperties;
import com.cooperativa.votacao.dto.AbrirSessaoRequest;
import com.cooperativa.votacao.dto.ResultadoResponse;
import com.cooperativa.votacao.dto.VotoRequest;
import com.cooperativa.votacao.exception.NegocioException;
import com.cooperativa.votacao.exception.RecursoNaoEncontradoException;
import com.cooperativa.votacao.model.OpcaoVoto;
import com.cooperativa.votacao.model.Pauta;
import com.cooperativa.votacao.model.SessaoVotacao;
import com.cooperativa.votacao.model.Voto;
import com.cooperativa.votacao.repository.SessaoVotacaoRepository;
import com.cooperativa.votacao.repository.VotoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class VotacaoService {

    private static final Logger log = LoggerFactory.getLogger(VotacaoService.class);

    private final PautaService pautaService;
    private final SessaoVotacaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final AssociadoValidadorClient associadoValidadorClient;
    private final VotacaoSessaoProperties sessaoProperties;

    public VotacaoService(PautaService pautaService,
                          SessaoVotacaoRepository sessaoRepository,
                          VotoRepository votoRepository,
                          AssociadoValidadorClient associadoValidadorClient,
                          VotacaoSessaoProperties sessaoProperties) {
        this.pautaService = pautaService;
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.associadoValidadorClient = associadoValidadorClient;
        this.sessaoProperties = sessaoProperties;
    }

    @Transactional
    public SessaoVotacao abrirSessao(Long pautaId, AbrirSessaoRequest request) {
        Pauta pauta = pautaService.buscarPorId(pautaId);

        if (sessaoRepository.findByPautaId(pautaId).isPresent()) {
            throw new NegocioException("Ja existe uma sessao de votacao para a pauta " + pautaId);
        }

        int duracaoSegundos = (request != null && request.duracaoSegundos() != null)
                ? request.duracaoSegundos()
                : sessaoProperties.duracaoPadraoSegundos();

        LocalDateTime agora = LocalDateTime.now();
        SessaoVotacao sessao = new SessaoVotacao();
        sessao.setPauta(pauta);
        sessao.setDataAbertura(agora);
        sessao.setDataFechamento(agora.plusSeconds(duracaoSegundos));

        SessaoVotacao salva = sessaoRepository.save(sessao);
        log.info("Sessao aberta: pautaId={} sessaoId={} fechaEm={}", pautaId, salva.getId(), salva.getDataFechamento());
        return salva;
    }

    @Transactional
    public Voto votar(Long pautaId, VotoRequest request) {
        SessaoVotacao sessao = sessaoRepository.findByPautaId(pautaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nenhuma sessao de votacao foi aberta para a pauta " + pautaId));

        if (!sessao.isAberta()) {
            throw new NegocioException("A sessao de votacao da pauta " + pautaId + " esta fechada");
        }

        if (votoRepository.existsBySessaoIdAndAssociadoId(sessao.getId(), request.associadoId())) {
            throw new NegocioException("Associado " + request.associadoId() + " ja votou nesta pauta");
        }

        if (associadoValidadorClient.isHabilitado()) {
            associadoValidadorClient.validarPodeVotar(request.associadoId());
        }

        Voto voto = new Voto();
        voto.setSessao(sessao);
        voto.setAssociadoId(request.associadoId());
        voto.setVoto(request.voto());

        try {
            Voto salvo = votoRepository.save(voto);
            log.info("Voto registrado: pautaId={} associadoId={} voto={}", pautaId, request.associadoId(), request.voto());
            return salvo;
        } catch (DataIntegrityViolationException e) {
            // protege contra corrida em requisicoes concorrentes do mesmo associado
            throw new NegocioException("Associado " + request.associadoId() + " ja votou nesta pauta");
        }
    }

    @Transactional(readOnly = true)
    public ResultadoResponse apurar(Long pautaId) {
        Pauta pauta = pautaService.buscarPorId(pautaId);
        SessaoVotacao sessao = sessaoRepository.findByPautaId(pautaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Nenhuma sessao de votacao foi aberta para a pauta " + pautaId));

        Map<OpcaoVoto, Long> contagem = new java.util.EnumMap<>(OpcaoVoto.class);
        contagem.put(OpcaoVoto.SIM, 0L);
        contagem.put(OpcaoVoto.NAO, 0L);
        List<VotoRepository.ContagemVoto> resultado = votoRepository.contarVotosPorSessao(sessao.getId());
        resultado.forEach(c -> contagem.put(c.getOpcao(), c.getTotal()));

        long sim = contagem.get(OpcaoVoto.SIM);
        long nao = contagem.get(OpcaoVoto.NAO);
        long total = sim + nao;

        String resultadoTexto;
        if (sessao.isAberta()) {
            resultadoTexto = "VOTACAO_EM_ANDAMENTO";
        } else if (sim > nao) {
            resultadoTexto = "APROVADA";
        } else if (nao > sim) {
            resultadoTexto = "REJEITADA";
        } else {
            resultadoTexto = "EMPATE";
        }

        return new ResultadoResponse(
                pauta.getId(),
                pauta.getTitulo(),
                !sessao.isAberta(),
                sim,
                nao,
                total,
                resultadoTexto
        );
    }
}