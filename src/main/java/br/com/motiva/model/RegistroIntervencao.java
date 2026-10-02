package br.com.motiva.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// Uma intervencao operacional EXECUTADA em um trecho, por uma equipe, em uma
// data: a linha da tabela T_MTV_INTERVENCAO.
//
// Sprint 3: era um record imutavel e o DAO guardava so os IDs (idTrecho, idEquipe).
// Sprint 4: virou @Entity (o JPA exige classe com construtor sem argumentos)
// e as FKs viraram associacoes @ManyToOne com os objetos de verdade.
@Entity
@Table(name = "T_MTV_INTERVENCAO")
public class RegistroIntervencao {

    // A coluna DESCRICAO e VARCHAR2(200).
    public static final int TAMANHO_MAX_DESCRICAO = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqIntervencao")
    @SequenceGenerator(name = "seqIntervencao", sequenceName = "SQ_MTV_INTERVENCAO", allocationSize = 1)
    @Column(name = "ID_INTERVENCAO")
    private Long id;

    // Grava o NOME da constante ('MECANIZADA'...), compativel com a CHECK do banco.
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO", nullable = false, length = 15)
    private TipoIntervencao tipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ID_TRECHO", nullable = false)
    private TrechoRodovia trecho;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_EQUIPE")
    private EquipeManutencao equipe;    // pode ser null

    @Column(name = "DATA_EXECUCAO", nullable = false)
    private LocalDateTime dataExecucao;

    @Column(name = "ALTURA_ANTES", nullable = false, columnDefinition = "NUMBER(6,2)")
    private double alturaAntes;

    @Column(name = "ALTURA_DEPOIS", nullable = false, columnDefinition = "NUMBER(6,2)")
    private double alturaDepois;

    @Column(name = "DESCRICAO", length = TAMANHO_MAX_DESCRICAO)
    private String descricao;

    protected RegistroIntervencao() {
    }

    // Executa de fato o servico no trecho (POLIMORFISMO em executarServico) e
    // devolve o registro pronto para ser salvo pelo repository.
    public static RegistroIntervencao executar(IntervencaoOperacional intervencao,
                                               TrechoRodovia trecho,
                                               EquipeManutencao equipe) {
        RegistroIntervencao registro = new RegistroIntervencao();
        registro.tipo = intervencao.getTipo();
        registro.trecho = trecho;
        registro.equipe = equipe;
        registro.dataExecucao = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        registro.alturaAntes = trecho.getAlturaVegetacao();
        registro.descricao = limitar(intervencao.executarServico(trecho));
        registro.alturaDepois = trecho.getAlturaVegetacao();
        return registro;
    }

    // Correcao do registro historico (PUT /api/intervencoes/{id}): substitui
    // todos os dados, mas NAO executa o servico de novo no trecho.
    public void corrigir(TipoIntervencao tipo, TrechoRodovia trecho, EquipeManutencao equipe,
                         LocalDateTime dataExecucao, double alturaAntes, double alturaDepois,
                         String descricao) {
        this.tipo = tipo;
        this.trecho = trecho;
        this.equipe = equipe;
        this.dataExecucao = dataExecucao.truncatedTo(ChronoUnit.SECONDS);
        this.alturaAntes = alturaAntes;
        this.alturaDepois = alturaDepois;
        this.descricao = limitar(descricao);
    }

    // Reconstroi o objeto de comportamento correspondente a este registro.
    public IntervencaoOperacional criarIntervencao() {
        return tipo.criar();
    }

    private static String limitar(String texto) {
        if (texto == null || texto.length() <= TAMANHO_MAX_DESCRICAO) {
            return texto;
        }
        return texto.substring(0, TAMANHO_MAX_DESCRICAO - 3) + "...";
    }

    public Long getId() {
        return id;
    }

    public TipoIntervencao getTipo() {
        return tipo;
    }

    public TrechoRodovia getTrecho() {
        return trecho;
    }

    public EquipeManutencao getEquipe() {
        return equipe;
    }

    public LocalDateTime getDataExecucao() {
        return dataExecucao;
    }

    public double getAlturaAntes() {
        return alturaAntes;
    }

    public double getAlturaDepois() {
        return alturaDepois;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return "Intervencao #" + id + " " + tipo + " em " + dataExecucao;
    }
}
