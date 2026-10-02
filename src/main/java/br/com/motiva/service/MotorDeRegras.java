package br.com.motiva.service;

import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.ItemRelatorio;
import br.com.motiva.model.MonitoravelViaIoT;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.Pulverizacao;
import br.com.motiva.model.RelatorioPrioridade;
import br.com.motiva.model.RocadaManual;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TrechoRodovia;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

// O MOTOR DE REGRAS (inteligencia da Sprint 2), agora um @Service: o Spring
// cria uma unica instancia e injeta onde precisar (GeradorRelatorio,
// TrechoService, EquipeService, IntervencaoService).
//
// E AQUI que vive a logica de classificacao. Os controllers nunca conhecem
// limiares: eles so recebem a prioridade ja calculada.
@Service
public class MotorDeRegras {

    // Acima desta taxa de crescimento (cm/dia) o trecho e considerado
    // "de rebrote rapido" e a pulverizacao passa a fazer mais sentido.
    private static final double TAXA_REBROTE_RAPIDO = 2.0;

    // Algoritmo principal: recebe os trechos, atualiza o que for IoT,
    // classifica cada trecho e devolve o relatorio ordenado por urgencia.
    public RelatorioPrioridade gerarRelatorio(List<? extends TrechoRodovia> trechos) {
        RelatorioPrioridade relatorio = new RelatorioPrioridade();

        for (TrechoRodovia trecho : trechos) {
            if (trecho == null) {
                continue;
            }

            // Trechos com IoT se atualizam sozinhos, sem inspecao visual.
            boolean viaIoT = false;
            if (trecho instanceof MonitoravelViaIoT sensor) {
                sensor.transmitirDadosSensor();
                viaIoT = true;
            }

            Prioridade prioridade = classificar(trecho);
            IntervencaoOperacional intervencao = recomendarIntervencao(trecho, prioridade);
            relatorio.adicionar(new ItemRelatorio(trecho, prioridade, intervencao, viaIoT));
        }

        // Ordena pela urgencia (CRITICA tem o menor ordinal). A ordenacao e
        // estavel: dentro da mesma prioridade, fica a ordem de KM recebida.
        relatorio.getItens().sort(Comparator.comparingInt(i -> i.getPrioridade().ordinal()));
        return relatorio;
    }

    // REGRA 1: a prioridade depende da altura da vegetacao.
    //   >= 40 cm CRITICA | >= 25 cm ALTA | >= 15 cm MEDIA | < 15 cm BAIXA
    public Prioridade classificar(double alturaCm) {
        for (Prioridade prioridade : Prioridade.values()) { // do mais urgente para o menos urgente
            if (alturaCm >= prioridade.getAlturaMinimaCm()) {
                return prioridade;
            }
        }
        return Prioridade.BAIXA;
    }

    public Prioridade classificar(TrechoRodovia trecho) {
        return classificar(trecho.getAlturaVegetacao());
    }

    // Regra da Sprint 1: um trecho e critico quando atinge a prioridade CRITICA.
    public boolean estaCritico(TrechoRodovia trecho) {
        return classificar(trecho) == Prioridade.CRITICA;
    }

    // REGRA 2: o tipo de intervencao depende da prioridade e do rebrote.
    public IntervencaoOperacional recomendarIntervencao(TrechoRodovia trecho, Prioridade prioridade) {
        boolean rebroteRapido = trecho.taxaCrescimentoDiaria() >= TAXA_REBROTE_RAPIDO;

        return switch (prioridade) {
            // Vegetacao alta e densa: so o trator da conta.
            case CRITICA -> new RocadaMecanizada();
            // Rebrote rapido -> pulveriza para nao voltar logo; senao, mecaniza.
            case ALTA -> rebroteRapido ? new Pulverizacao() : new RocadaMecanizada();
            // Volume menor: equipe a pe resolve.
            case MEDIA -> new RocadaManual();
            // Sem necessidade de acao agora, apenas monitorar.
            case BAIXA -> null;
        };
    }
}
