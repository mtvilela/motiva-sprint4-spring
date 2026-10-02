package br.com.motiva.model;

// Uma linha do Relatorio de Prioridade: liga um trecho a sua prioridade
// e a intervencao recomendada pelo motor de regras.
public class ItemRelatorio {

    private final TrechoRodovia trecho;
    private final Prioridade prioridade;
    private final IntervencaoOperacional intervencaoRecomendada; // null = sem acao
    private final boolean atualizadoViaIoT;

    public ItemRelatorio(TrechoRodovia trecho, Prioridade prioridade,
                         IntervencaoOperacional intervencaoRecomendada,
                         boolean atualizadoViaIoT) {
        this.trecho = trecho;
        this.prioridade = prioridade;
        this.intervencaoRecomendada = intervencaoRecomendada;
        this.atualizadoViaIoT = atualizadoViaIoT;
    }

    public TrechoRodovia getTrecho() {
        return trecho;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public IntervencaoOperacional getIntervencaoRecomendada() {
        return intervencaoRecomendada;
    }

    public boolean isAtualizadoViaIoT() {
        return atualizadoViaIoT;
    }

    public String descricaoIntervencao() {
        return intervencaoRecomendada == null
                ? "Nenhuma (monitorar)"
                : intervencaoRecomendada.getNome();
    }
}
