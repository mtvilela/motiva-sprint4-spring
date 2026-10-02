package br.com.motiva.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.util.Locale;

// Classe ABSTRATA: molde base de um trecho de rodovia monitorado.
// Nao pode ser instanciada diretamente; cada terreno (umido, seco, ...)
// define seu PROPRIO comportamento de crescimento da vegetacao.
//
// Sprint 4: virou @Entity. A heranca das Sprints 2/3 e mapeada com
// SINGLE_TABLE: TrechoSeco e TrechoUmido moram na MESMA tabela
// T_MTV_TRECHO_RODOVIA e a coluna TIPO_TERRENO (discriminador) diz ao
// Hibernate qual subclasse instanciar na leitura - exatamente o que o
// switch do TrechoRodoviaDAO fazia na mao na Sprint 3.
//
// As validacoes de entrada (Sprint 1) ficam no TrechoService; aqui ficam o
// estado encapsulado e o comportamento do dominio.
@Entity
@Table(name = "T_MTV_TRECHO_RODOVIA")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TIPO_TERRENO", discriminatorType = DiscriminatorType.STRING, length = 10)
public abstract class TrechoRodovia {

    // PK gerada pela sequence SQ_MTV_TRECHO (allocationSize = 1 casa com o INCREMENT BY 1 do Oracle).
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqTrecho")
    @SequenceGenerator(name = "seqTrecho", sequenceName = "SQ_MTV_TRECHO", allocationSize = 1)
    @Column(name = "ID_TRECHO")
    private Long id;

    @Column(name = "CODIGO", nullable = false, unique = true, length = 20)
    private String codigo;              // identificador de negocio (ex.: "BR101-A")

    @Column(name = "KM", nullable = false, columnDefinition = "NUMBER(8,2)")
    private double km;                  // marco quilometrico do trecho

    @Column(name = "ALTURA_VEGETACAO", nullable = false, columnDefinition = "NUMBER(6,2)")
    private double alturaVegetacao;     // altura atual da vegetacao em cm

    @Column(name = "DIAS_SEM_ROCADA", nullable = false)
    private int diasSemRocada;          // dias desde a ultima intervencao

    // A MESMA coluna do discriminador, mapeada tambem como atributo somente
    // leitura (quem grava o valor e o @DiscriminatorValue da subclasse).
    // Existe para permitir a derived query findByTipoTerreno(...).
    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_TERRENO", length = 10, insertable = false, updatable = false)
    private TipoTerreno tipoTerreno;

    // Construtor exigido pelo JPA: o Hibernate instancia a subclasse por reflexao.
    protected TrechoRodovia() {
    }

    protected TrechoRodovia(String codigo, double km, double alturaVegetacao, int diasSemRocada,
                            TipoTerreno tipoTerreno) {
        this.codigo = codigo;
        this.km = km;
        this.alturaVegetacao = arredondar(alturaVegetacao);
        this.diasSemRocada = diasSemRocada;
        this.tipoTerreno = tipoTerreno;
    }

    // Metodo ABSTRATO: o coracao do motor de regras.
    // Cada tipo de terreno cresce a uma taxa diferente (cm por dia).
    public abstract double taxaCrescimentoDiaria();

    // POLIMORFISMO: a simulacao e a mesma, mas o resultado depende
    // da taxa de cada subclasse.
    public void simularCrescimento(int dias) {
        if (dias <= 0) {
            return;
        }
        this.alturaVegetacao = arredondar(alturaVegetacao + taxaCrescimentoDiaria() * dias);
        this.diasSemRocada += dias;
    }

    // Chamado por uma IntervencaoOperacional apos o servico ser executado.
    // Uma intervencao nunca AUMENTA a vegetacao: se o trecho ja estava abaixo
    // da altura residual do servico, ele continua como estava.
    public void registrarRocada(double alturaResidual) {
        this.alturaVegetacao = arredondar(Math.max(0, Math.min(alturaVegetacao, alturaResidual)));
        this.diasSemRocada = 0;
    }

    // Atualizacao completa dos dados cadastrais (PUT /api/trechos/{id}).
    public void atualizarDados(String codigo, double km, double alturaVegetacao, int diasSemRocada) {
        this.codigo = codigo;
        this.km = km;
        this.alturaVegetacao = arredondar(alturaVegetacao);
        this.diasSemRocada = diasSemRocada;
    }

    // Nova medicao da altura (inspecao visual ou sensor IoT).
    protected void registrarMedicao(double alturaMedida) {
        this.alturaVegetacao = arredondar(Math.max(0, alturaMedida));
    }

    // NUMBER(6,2) no Oracle: mantem 2 casas para o objeto em memoria ficar igual ao banco.
    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public double getKm() {
        return km;
    }

    public double getAlturaVegetacao() {
        return alturaVegetacao;
    }

    public int getDiasSemRocada() {
        return diasSemRocada;
    }

    public TipoTerreno getTipoTerreno() {
        return tipoTerreno;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "Trecho %s #%s (KM %.1f) | %s | %.1f cm | %d dias sem rocada",
                codigo, id, km, tipoTerreno, alturaVegetacao, diasSemRocada);
    }
}
