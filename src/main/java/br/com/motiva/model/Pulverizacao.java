package br.com.motiva.model;

import java.util.Locale;

// Intervencao quimica (herbicida): nao corta, mas retarda o rebrote.
// Indicada para trechos que crescem rapido (terreno umido), onde so cortar
// faria a vegetacao voltar logo. Deixa a vegetacao seca/baixa.
public class Pulverizacao extends IntervencaoOperacional {

    private static final double ALTURA_RESIDUAL = 10.0; // cm (vegetacao seca em pe)

    public Pulverizacao() {
        super("Pulverizacao", 20.0);
    }

    @Override
    public String executarServico(TrechoRodovia trecho) {
        double antes = trecho.getAlturaVegetacao();
        trecho.registrarRocada(ALTURA_RESIDUAL);
        return String.format(Locale.ROOT,
                "[%s] KM %.1f: herbicida aplicado, vegetacao de %.1f cm sera dessecada (rebrote retardado).",
                nome, trecho.getKm(), antes);
    }

    @Override
    public TipoIntervencao getTipo() {
        return TipoIntervencao.PULVERIZACAO;
    }

    @Override
    public double getAlturaResidual() {
        return ALTURA_RESIDUAL;
    }
}
