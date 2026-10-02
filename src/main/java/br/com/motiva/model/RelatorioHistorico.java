package br.com.motiva.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

// O "cabecalho" de um Relatorio de Prioridade ja gerado, guardado para
// historico na tabela T_MTV_RELATORIO_PRIORIDADE. Enquanto RelatorioPrioridade
// e o relatorio vivo em memoria (com os itens), esta entidade e a fotografia
// que fica no banco: data, contagem por prioridade e um resumo textual.
//
// Sprint 3: era um record montado pelo RelatorioPrioridadeDAO.
// Sprint 4: @Entity salva com repository.save().
@Entity
@Table(name = "T_MTV_RELATORIO_PRIORIDADE")
public class RelatorioHistorico {

    // A coluna RESUMO e VARCHAR2(2000); textos maiores sao cortados para nao estourar ORA-12899.
    private static final int TAMANHO_MAX_RESUMO = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqRelatorio")
    @SequenceGenerator(name = "seqRelatorio", sequenceName = "SQ_MTV_RELATORIO", allocationSize = 1)
    @Column(name = "ID_RELATORIO")
    private Long id;

    @Column(name = "DATA_GERACAO", nullable = false)
    private LocalDateTime dataGeracao;

    @Column(name = "QT_CRITICA", nullable = false)
    private int qtCritica;

    @Column(name = "QT_ALTA", nullable = false)
    private int qtAlta;

    @Column(name = "QT_MEDIA", nullable = false)
    private int qtMedia;

    @Column(name = "QT_BAIXA", nullable = false)
    private int qtBaixa;

    // Coluna VIRTUAL do Oracle: quem calcula a soma e o proprio banco.
    // O Hibernate nunca grava nela (insertable/updatable = false) e, por causa
    // do @Generated, le o valor calculado logo depois do INSERT.
    @Generated
    @Column(name = "TOTAL_TRECHOS", insertable = false, updatable = false,
            columnDefinition = "NUMBER(6) GENERATED ALWAYS AS (QT_CRITICA + QT_ALTA + QT_MEDIA + QT_BAIXA)")
    private Integer totalTrechos;

    @Column(name = "RESUMO", length = TAMANHO_MAX_RESUMO)
    private String resumo;

    protected RelatorioHistorico() {
    }

    // Tira a "fotografia" do relatorio vivo que o motor acabou de gerar.
    public static RelatorioHistorico de(RelatorioPrioridade relatorio) {
        RelatorioHistorico historico = new RelatorioHistorico();
        historico.qtCritica = relatorio.contar(Prioridade.CRITICA);
        historico.qtAlta = relatorio.contar(Prioridade.ALTA);
        historico.qtMedia = relatorio.contar(Prioridade.MEDIA);
        historico.qtBaixa = relatorio.contar(Prioridade.BAIXA);
        historico.resumo = limitarResumo(relatorio.gerarResumo());
        return historico;
    }

    // Callback do JPA: roda automaticamente antes do INSERT.
    @PrePersist
    void antesDeInserir() {
        if (dataGeracao == null) {
            dataGeracao = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        }
    }

    // Todo trecho que nao e BAIXA recebe alguma intervencao recomendada.
    public int getQtComIntervencao() {
        return qtCritica + qtAlta + qtMedia;
    }

    private static String limitarResumo(String resumo) {
        if (resumo == null || resumo.length() <= TAMANHO_MAX_RESUMO) {
            return resumo;
        }
        return resumo.substring(0, TAMANHO_MAX_RESUMO - 3) + "...";
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataGeracao() {
        return dataGeracao;
    }

    public int getQtCritica() {
        return qtCritica;
    }

    public int getQtAlta() {
        return qtAlta;
    }

    public int getQtMedia() {
        return qtMedia;
    }

    public int getQtBaixa() {
        return qtBaixa;
    }

    public Integer getTotalTrechos() {
        return totalTrechos;
    }

    public String getResumo() {
        return resumo;
    }

    @Override
    public String toString() {
        return "Relatorio #" + id + " de " + dataGeracao + " (" + totalTrechos + " trechos)";
    }
}
