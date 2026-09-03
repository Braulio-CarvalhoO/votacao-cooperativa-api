package com.cooperativa.votacao.repository;

import com.cooperativa.votacao.model.OpcaoVoto;
import com.cooperativa.votacao.model.Voto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VotoRepository extends JpaRepository<Voto, Long> {

    boolean existsBySessaoIdAndAssociadoId(Long sessaoId, String associadoId);

    Optional<Voto> findBySessaoIdAndAssociadoId(Long sessaoId, String associadoId);

    long countBySessaoIdAndVoto(Long sessaoId, OpcaoVoto voto);

    @Query("select v.voto as opcao, count(v) as total from Voto v where v.sessao.id = :sessaoId group by v.voto")
    List<ContagemVoto> contarVotosPorSessao(@Param("sessaoId") Long sessaoId);

    interface ContagemVoto {
        OpcaoVoto getOpcao();
        long getTotal();
    }
}