package com.cooperativa.votacao.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "voto",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_voto_sessao_associado",
                columnNames = {"sessao_id", "associado_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Voto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoVotacao sessao;

    // id unico do associado .
    @Column(name = "associado_id", nullable = false, length = 20)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OpcaoVoto voto;

    @Column(name = "data_voto", nullable = false)
    private LocalDateTime dataVoto;

    @PrePersist
    public void prePersist() {
        this.dataVoto = LocalDateTime.now();
    }
}