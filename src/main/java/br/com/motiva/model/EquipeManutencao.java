package br.com.motiva.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

// Equipe de manutencao (classe da Sprint 1), mapeada para a tabela
// T_MTV_EQUIPE_MANUTENCAO.
//
// Sprint 3: o DAO fazia LEFT JOIN com o trecho e remontava o objeto na mao.
// Sprint 4: o @ManyToOne faz o Hibernate resolver a FK ID_TRECHO sozinho.
// A regra "so pode ser associada a um trecho CRITICO" (Sprint 1) e aplicada
// pelo EquipeService antes de chamar associarTrecho().
@Entity
@Table(name = "T_MTV_EQUIPE_MANUTENCAO")
public class EquipeManutencao {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqEquipe")
    @SequenceGenerator(name = "seqEquipe", sequenceName = "SQ_MTV_EQUIPE", allocationSize = 1)
    @Column(name = "ID_EQUIPE")
    private Long id;

    @Column(name = "NOME", nullable = false, unique = true, length = 60)
    private String nome;

    // FK ID_TRECHO (opcional): trecho critico pelo qual a equipe responde.
    // LAZY: o trecho so e buscado no banco se alguem realmente usar.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_TRECHO")
    private TrechoRodovia trechoResponsavel;

    protected EquipeManutencao() {
    }

    public EquipeManutencao(String nome) {
        this.nome = nome;
    }

    public void renomear(String novoNome) {
        this.nome = novoNome;
    }

    public void associarTrecho(TrechoRodovia trecho) {
        this.trechoResponsavel = trecho;
    }

    public void desassociarTrecho() {
        this.trechoResponsavel = null;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public TrechoRodovia getTrechoResponsavel() {
        return trechoResponsavel;
    }

    @Override
    public String toString() {
        return "Equipe #" + id + " " + nome;
    }
}
