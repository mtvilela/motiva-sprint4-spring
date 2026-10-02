package br.com.motiva.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Resultado do motor de regras: a lista de itens ja ordenada por prioridade
// (CRITICA primeiro). Gera as contagens e o resumo que vao para o historico.
//
// Sprint 4: o antigo imprimir() (console) saiu - a "interface" agora e o JSON
// devolvido pelo RelatorioController.
public class RelatorioPrioridade {

    private final List<ItemRelatorio> itens = new ArrayList<>();

    public void adicionar(ItemRelatorio item) {
        itens.add(item);
    }

    public List<ItemRelatorio> getItens() {
        return itens;
    }

    public int getTotalTrechos() {
        return itens.size();
    }

    // Quantos trechos realmente precisam de alguma intervencao.
    public long totalComIntervencao() {
        return itens.stream()
                .filter(i -> i.getIntervencaoRecomendada() != null)
                .count();
    }

    // Quantos trechos cairam em uma determinada prioridade.
    public int contar(Prioridade prioridade) {
        return (int) itens.stream()
                .filter(i -> i.getPrioridade() == prioridade)
                .count();
    }

    // Resumo textual (um trecho por item) gravado na coluna RESUMO do historico.
    public String gerarResumo() {
        StringBuilder resumo = new StringBuilder();
        for (ItemRelatorio item : itens) {
            if (resumo.length() > 0) {
                resumo.append(" | ");
            }
            TrechoRodovia t = item.getTrecho();
            resumo.append(String.format(Locale.ROOT, "%s %s KM %.1f %.1fcm -> %s",
                    item.getPrioridade(), t.getCodigo(), t.getKm(),
                    t.getAlturaVegetacao(), item.descricaoIntervencao()));
        }
        return resumo.toString();
    }
}
