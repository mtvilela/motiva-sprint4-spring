package br.com.motiva.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.util.concurrent.ThreadLocalRandom;

// Trecho de terreno UMIDO: a vegetacao cresce rapido.
// Alem de ser um TrechoRodovia (HERANCA), ele POSSUI tecnologia instalada,
// entao implementa a interface MonitoravelViaIoT (CONTRATO de comportamento).
//
// Sprint 4: linhas com TIPO_TERRENO = 'UMIDO' viram objetos desta classe.
@Entity
@DiscriminatorValue("UMIDO")
public class TrechoUmido extends TrechoRodovia implements MonitoravelViaIoT {

    private static final double TAXA_BASE = 2.5; // cm por dia (cresce rapido)

    // Coluna exclusiva da subclasse: na tabela unica ela fica NULL nas linhas SECO
    // (a CHECK CK_MTV_TRECHO_SENSOR do banco garante essa regra).
    @Column(name = "ID_SENSOR", length = 20)
    private String idSensor;

    protected TrechoUmido() {
    }

    public TrechoUmido(String codigo, double km, double alturaInicial, String idSensor) {
        this(codigo, km, alturaInicial, 0, idSensor);
    }

    public TrechoUmido(String codigo, double km, double alturaVegetacao, int diasSemRocada, String idSensor) {
        super(codigo, km, alturaVegetacao, diasSemRocada, TipoTerreno.UMIDO);
        this.idSensor = idSensor;
    }

    @Override
    public double taxaCrescimentoDiaria() {
        return TAXA_BASE;
    }

    @Override
    public double transmitirDadosSensor() {
        // Simula a telemetria: o sensor mede uma pequena variacao (+-1 cm) em
        // torno da altura atual e ja atualiza o trecho automaticamente.
        double medicao = getAlturaVegetacao() + ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
        registrarMedicao(medicao);
        return getAlturaVegetacao();
    }

    @Override
    public String getIdSensor() {
        return idSensor;
    }

    // Substituicao do sensor instalado no trecho (PUT /api/trechos/{id}).
    public void trocarSensor(String novoIdSensor) {
        this.idSensor = novoIdSensor;
    }

    @Override
    public String toString() {
        return super.toString() + " [IoT: " + idSensor + "]";
    }
}
