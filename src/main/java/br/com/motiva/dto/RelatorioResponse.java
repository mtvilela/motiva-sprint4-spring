package br.com.motiva.dto;

import br.com.motiva.model.RelatorioHistorico;
import br.com.motiva.model.RelatorioPrioridade;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

// Contrato de SAIDA de um relatorio de prioridade.
// itens so aparece na resposta do POST (relatorio recem-gerado, em memoria):
// o historico no banco guarda apenas as contagens e o resumo textual.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RelatorioResponse(
        Long id,
        LocalDateTime dataGeracao,
        int qtCritica,
        int qtAlta,
        int qtMedia,
        int qtBaixa,
        Integer totalTrechos,
        int qtComIntervencao,
        String resumo,
        List<ItemRelatorioResponse> itens) {

    // Linha do historico (GET /api/relatorios, /periodo, /{id}).
    public static RelatorioResponse de(RelatorioHistorico historico) {
        return de(historico, null);
    }

    // Relatorio recem-gerado (POST /api/relatorios): historico salvo + itens detalhados.
    public static RelatorioResponse de(RelatorioHistorico historico, RelatorioPrioridade relatorio) {
        List<ItemRelatorioResponse> itens = relatorio == null
                ? null
                : relatorio.getItens().stream().map(ItemRelatorioResponse::de).toList();
        return new RelatorioResponse(
                historico.getId(),
                historico.getDataGeracao(),
                historico.getQtCritica(),
                historico.getQtAlta(),
                historico.getQtMedia(),
                historico.getQtBaixa(),
                historico.getTotalTrechos(),
                historico.getQtComIntervencao(),
                historico.getResumo(),
                itens);
    }
}
