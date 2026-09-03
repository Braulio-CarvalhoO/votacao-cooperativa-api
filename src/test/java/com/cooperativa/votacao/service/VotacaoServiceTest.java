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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class VotacaoServiceTest {

    @Mock
    private PautaService pautaService;
    @Mock
    private SessaoVotacaoRepository sessaoRepository;
    @Mock
    private VotoRepository votoRepository;
    @Mock
    private AssociadoValidadorClient associadoValidadorClient;

    private VotacaoService votacaoService;

    private Pauta pauta;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        VotacaoSessaoProperties props = new VotacaoSessaoProperties(60);
        votacaoService = new VotacaoService(pautaService, sessaoRepository, votoRepository, associadoValidadorClient, props);

        pauta = new Pauta();
        pauta.setId(1L);
        pauta.setTitulo("Pauta de teste");

        when(pautaService.buscarPorId(1L)).thenReturn(pauta);
    }

    @Test
    void deveAbrirSessaoComDuracaoPadraoQuandoNaoInformada() {
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());
        when(sessaoRepository.save(any())).thenAnswer(inv -> {
            SessaoVotacao s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SessaoVotacao sessao = votacaoService.abrirSessao(1L, null);

        assertThat(sessao.getId()).isEqualTo(10L);
        long segundos = java.time.Duration.between(sessao.getDataAbertura(), sessao.getDataFechamento()).getSeconds();
        assertThat(segundos).isEqualTo(60);
    }

    @Test
    void naoDevePermitirAbrirDuasSessoesParaMesmaPauta() {
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(new SessaoVotacao()));

        assertThatThrownBy(() -> votacaoService.abrirSessao(1L, new AbrirSessaoRequest(30)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Ja existe uma sessao");
    }

    @Test
    void naoDevePermitirVotarSemSessaoAberta() {
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> votacaoService.votar(1L, new VotoRequest("12345678900", OpcaoVoto.SIM)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDevePermitirVotarQuandoSessaoFechada() {
        SessaoVotacao sessaoFechada = sessaoComJanela(-120, -60);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessaoFechada));

        assertThatThrownBy(() -> votacaoService.votar(1L, new VotoRequest("12345678900", OpcaoVoto.SIM)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("fechada");
    }

    @Test
    void naoDevePermitirVotoDuplicadoDoMesmoAssociado() {
        SessaoVotacao sessaoAberta = sessaoComJanela(-10, 50);
        sessaoAberta.setId(10L);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessaoAberta));
        when(votoRepository.existsBySessaoIdAndAssociadoId(10L, "12345678900")).thenReturn(true);

        assertThatThrownBy(() -> votacaoService.votar(1L, new VotoRequest("12345678900", OpcaoVoto.SIM)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("ja votou");
    }

    @Test
    void deveRegistrarVotoValido() {
        SessaoVotacao sessaoAberta = sessaoComJanela(-10, 50);
        sessaoAberta.setId(10L);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessaoAberta));
        when(votoRepository.existsBySessaoIdAndAssociadoId(10L, "12345678900")).thenReturn(false);
        when(associadoValidadorClient.isHabilitado()).thenReturn(false);
        when(votoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Voto voto = votacaoService.votar(1L, new VotoRequest("12345678900", OpcaoVoto.SIM));

        assertThat(voto.getAssociadoId()).isEqualTo("12345678900");
        assertThat(voto.getVoto()).isEqualTo(OpcaoVoto.SIM);
        verify(votoRepository).save(any());
    }

    @Test
    void deveApurarResultadoComVitoriaDoSim() {
        SessaoVotacao sessaoFechada = sessaoComJanela(-120, -60);
        sessaoFechada.setId(10L);
        when(sessaoRepository.findByPautaId(1L)).thenReturn(Optional.of(sessaoFechada));
        when(votoRepository.contarVotosPorSessao(10L)).thenReturn(List.of(
                contagem(OpcaoVoto.SIM, 7L),
                contagem(OpcaoVoto.NAO, 3L)
        ));

        ResultadoResponse resultado = votacaoService.apurar(1L);

        assertThat(resultado.totalVotosSim()).isEqualTo(7L);
        assertThat(resultado.totalVotosNao()).isEqualTo(3L);
        assertThat(resultado.resultado()).isEqualTo("APROVADA");
        assertThat(resultado.sessaoEncerrada()).isTrue();
    }

    private SessaoVotacao sessaoComJanela(long inicioOffsetSegundos, long fimOffsetSegundos) {
        SessaoVotacao sessao = new SessaoVotacao();
        LocalDateTime agora = LocalDateTime.now();
        sessao.setPauta(pauta);
        sessao.setDataAbertura(agora.plusSeconds(inicioOffsetSegundos));
        sessao.setDataFechamento(agora.plusSeconds(fimOffsetSegundos));
        return sessao;
    }

    private VotoRepository.ContagemVoto contagem(OpcaoVoto opcao, long total) {
        return new VotoRepository.ContagemVoto() {
            @Override
            public OpcaoVoto getOpcao() {
                return opcao;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}