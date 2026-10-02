package br.com.motiva.dto;

import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.ItemRelatorio;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.TipoIntervencao;
import br.com.motiva.model.TipoTerreno;
import br.com.motiva.model.TrechoRodovia;

// Uma linha do relatorio gerado (a antiga linha impressa no console pela Sprint 2).
// fonte = "IoT" quando o trecho leu o proprio sensor, "Visual" quando depende de inspecao.
public record ItemRelatorioResponse(
        Prioridade prioridade,
        Long trechoId,
        String codigo,
        double km,
        double alturaVegetacao,
        TipoTerreno tipoTerreno,
        String fonte,
        TipoIntervencao tipoIntervencao,
        String intervencaoRecomendada) {

    public static ItemRelatorioResponse de(ItemRelatorio item) {
        TrechoRodovia trecho = item.getTrecho();
        IntervencaoOperacional recomendada = item.getIntervencaoRecomendada();
        return new ItemRelatorioResponse(
                item.getPrioridade(),
                trecho.getId(),
                trecho.getCodigo(),
                trecho.getKm(),
                trecho.getAlturaVegetacao(),
                trecho.getTipoTerreno(),
                item.isAtualizadoViaIoT() ? "IoT" : "Visual",
                recomendada == null ? null : recomendada.getTipo(),
                item.descricaoIntervencao());
    }
}
