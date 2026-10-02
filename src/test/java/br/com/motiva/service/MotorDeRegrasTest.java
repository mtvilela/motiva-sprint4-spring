package br.com.motiva.service;

import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.MonitoravelViaIoT;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.Pulverizacao;
import br.com.motiva.model.RelatorioPrioridade;
import br.com.motiva.model.RocadaManual;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TipoIntervencao;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.model.TrechoSeco;
import br.com.motiva.model.TrechoUmido;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Modifier;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Testes UNITARIOS do dominio e do motor de regras (sem Spring e sem banco).
// Sao os mesmos cenarios do test.Testes da Sprint 3, agora com JUnit 5 + AssertJ.
class MotorDeRegrasTest {

    private final MotorDeRegras motor = new MotorDeRegras();

    @ParameterizedTest(name = "{0} cm -> {1}")
    @CsvSource({
            "52.0, CRITICA",
            "40.0, CRITICA",
            "39.9, ALTA",
            "25.0, ALTA",
            "18.0, MEDIA",
            "15.0, MEDIA",
            "14.9, BAIXA",
            "0.0,  BAIXA"
    })
    void classificaPelaAlturaDaVegetacao(double altura, Prioridade esperada) {
        assertThat(motor.classificar(altura)).isEqualTo(esperada);
    }

    @Test
    void trechoCriticoComecaEm40cm() {
        assertThat(motor.estaCritico(new TrechoSeco("C1", 0, 40.0))).isTrue();
        assertThat(motor.estaCritico(new TrechoSeco("C2", 0, 39.9))).isFalse();
    }

    @Test
    void recomendaIntervencaoPelaPrioridadeERebrote() {
        TrechoRodovia critico = new TrechoSeco("CRIT", 0, 50);
        TrechoRodovia altoUmido = new TrechoUmido("AU", 0, 30, "S-1");
        TrechoRodovia altoSeco = new TrechoSeco("AS", 0, 30);
        TrechoRodovia medio = new TrechoSeco("MED", 0, 18);
        TrechoRodovia baixo = new TrechoSeco("BX", 0, 5);

        assertThat(recomendar(critico)).isInstanceOf(RocadaMecanizada.class);
        assertThat(recomendar(altoUmido)).isInstanceOf(Pulverizacao.class);   // rebrote rapido
        assertThat(recomendar(altoSeco)).isInstanceOf(RocadaMecanizada.class);
        assertThat(recomendar(medio)).isInstanceOf(RocadaManual.class);
        assertThat(recomendar(baixo)).isNull();                               // so monitorar
    }

    @Test
    void relatorioOrdenaDoMaisUrgenteParaOMenosUrgente() {
        List<TrechoRodovia> trechos = List.of(
                new TrechoSeco("BAIXO", 1, 5),
                new TrechoSeco("CRITICO", 2, 50),
                new TrechoSeco("MEDIO", 3, 18),
                new TrechoSeco("ALTO", 4, 30));

        RelatorioPrioridade relatorio = motor.gerarRelatorio(trechos);

        assertThat(relatorio.getItens()).extracting(i -> i.getPrioridade())
                .containsExactly(Prioridade.CRITICA, Prioridade.ALTA, Prioridade.MEDIA, Prioridade.BAIXA);
        assertThat(relatorio.totalComIntervencao()).isEqualTo(3);
        assertThat(relatorio.gerarResumo()).startsWith("CRITICA CRITICO KM 2.0 50.0cm -> Rocada Mecanizada");
    }

    @Test
    void trechoUmidoLeOSensorDuranteOMotor() {
        TrechoUmido umido = new TrechoUmido("U", 1, 30, "IOT-9");

        RelatorioPrioridade relatorio = motor.gerarRelatorio(List.of(umido, new TrechoSeco("S", 2, 30)));

        assertThat(relatorio.getItens()).filteredOn(i -> i.getTrecho() == umido)
                .singleElement()
                .satisfies(item -> assertThat(item.isAtualizadoViaIoT()).isTrue());
        assertThat(umido.getAlturaVegetacao()).isBetween(29.0, 31.0); // leitura de +-1 cm
    }

    // ----- dominio (Sprints 1 e 2) -----

    @Test
    void classesBaseSaoAbstratas() {
        assertThat(Modifier.isAbstract(TrechoRodovia.class.getModifiers())).isTrue();
        assertThat(Modifier.isAbstract(IntervencaoOperacional.class.getModifiers())).isTrue();
    }

    @Test
    void trechoUmidoCresceMaisRapidoQueOSeco() {
        TrechoRodovia umido = new TrechoUmido("U", 1, 0, "S-U");
        TrechoRodovia seco = new TrechoSeco("S", 2, 0);

        umido.simularCrescimento(10); // 2.5 cm/dia
        seco.simularCrescimento(10);  // 0.8 cm/dia

        assertThat(umido.getAlturaVegetacao()).isEqualTo(25.0);
        assertThat(seco.getAlturaVegetacao()).isEqualTo(8.0);
        assertThat(umido.getDiasSemRocada()).isEqualTo(10);
    }

    @Test
    void executarServicoDeixaAAlturaResidualDeCadaTipo() {
        TrechoRodovia mecanizada = new TrechoSeco("M", 0, 60);
        TrechoRodovia manual = new TrechoSeco("N", 0, 60);
        TrechoRodovia pulverizada = new TrechoSeco("P", 0, 60);

        new RocadaMecanizada().executarServico(mecanizada);
        new RocadaManual().executarServico(manual);
        new Pulverizacao().executarServico(pulverizada);

        assertThat(mecanizada.getAlturaVegetacao()).isEqualTo(5.0);
        assertThat(manual.getAlturaVegetacao()).isEqualTo(8.0);
        assertThat(pulverizada.getAlturaVegetacao()).isEqualTo(10.0);
        assertThat(mecanizada.getDiasSemRocada()).isZero();
    }

    @Test
    void intervencaoNuncaAumentaAVegetacao() {
        TrechoRodovia baixo = new TrechoSeco("B", 0, 6);

        new Pulverizacao().executarServico(baixo); // residual 10 cm > 6 cm

        assertThat(baixo.getAlturaVegetacao()).isEqualTo(6.0);
    }

    @Test
    void fabricaRecriaASubclasseCertaDeCadaTipo() {
        assertThat(TipoIntervencao.MECANIZADA.criar()).isInstanceOf(RocadaMecanizada.class);
        assertThat(TipoIntervencao.MANUAL.criar()).isInstanceOf(RocadaManual.class);
        assertThat(TipoIntervencao.PULVERIZACAO.criar()).isInstanceOf(Pulverizacao.class);
        for (TipoIntervencao tipo : TipoIntervencao.values()) {
            assertThat(tipo.criar().getTipo()).isEqualTo(tipo); // ida e volta tipo -> objeto -> tipo
        }
    }

    @Test
    void mockDeSensorCumpreOContratoMonitoravelViaIoT() {
        MonitoravelViaIoT sensor = new SensorMock("MOCK-01", 10.0, 23.5, 41.0);

        assertThat(sensor.transmitirDadosSensor()).isEqualTo(10.0);
        assertThat(sensor.transmitirDadosSensor()).isEqualTo(23.5);
        assertThat(sensor.transmitirDadosSensor()).isEqualTo(41.0);
        assertThat(sensor.getIdSensor()).isEqualTo("MOCK-01");
    }

    private IntervencaoOperacional recomendar(TrechoRodovia trecho) {
        return motor.recomendarIntervencao(trecho, motor.classificar(trecho));
    }

    // MOCK: implementa o contrato MonitoravelViaIoT com leituras pre-programadas.
    private static final class SensorMock implements MonitoravelViaIoT {
        private final String idSensor;
        private final double[] leituras;
        private int indice;

        SensorMock(String idSensor, double... leituras) {
            this.idSensor = idSensor;
            this.leituras = leituras;
        }

        @Override
        public double transmitirDadosSensor() {
            double valor = leituras[indice];
            if (indice < leituras.length - 1) {
                indice++;
            }
            return valor;
        }

        @Override
        public String getIdSensor() {
            return idSensor;
        }
    }
}
