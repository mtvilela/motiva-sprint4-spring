package br.com.motiva.dto;

import br.com.motiva.model.MonitoravelViaIoT;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.TipoTerreno;
import br.com.motiva.model.TrechoRodovia;

// Contrato de SAIDA de um trecho. Alem dos dados da tabela, expoe o que o
// dominio calcula: a taxa de crescimento (polimorfismo) e a prioridade
// atribuida pelo MotorDeRegras.
public record TrechoResponse(
        Long id,
        String codigo,
        double km,
        double alturaVegetacao,
        int diasSemRocada,
        TipoTerreno tipoTerreno,
        String idSensor,
        boolean monitoradoViaIoT,
        double taxaCrescimentoDiaria,
        Prioridade prioridade) {

    public static TrechoResponse de(TrechoRodovia trecho, Prioridade prioridade) {
        String idSensor = trecho instanceof MonitoravelViaIoT iot ? iot.getIdSensor() : null;
        return new TrechoResponse(
                trecho.getId(),
                trecho.getCodigo(),
                trecho.getKm(),
                trecho.getAlturaVegetacao(),
                trecho.getDiasSemRocada(),
                trecho.getTipoTerreno(),
                idSensor,
                idSensor != null,
                trecho.taxaCrescimentoDiaria(),
                prioridade);
    }
}
