package br.com.motiva.model;

import java.util.Locale;

// Intervencao com trator/rocadeira: indicada para vegetacao alta e densa.
// Corta rente ao chao (altura residual baixa) e rende muitos KM por dia.
public class RocadaMecanizada extends IntervencaoOperacional {

    private static final double ALTURA_RESIDUAL = 5.0; // cm

    public RocadaMecanizada() {
        super("Rocada Mecanizada", 12.0);
    }

    @Override
    public String executarServico(TrechoRodovia trecho) {
        double antes = trecho.getAlturaVegetacao();
        trecho.registrarRocada(ALTURA_RESIDUAL);
        return String.format(Locale.ROOT, "[%s] KM %.1f: trator cortou de %.1f cm para %.1f cm.",
                nome, trecho.getKm(), antes, trecho.getAlturaVegetacao());
    }

    @Override
    public TipoIntervencao getTipo() {
        return TipoIntervencao.MECANIZADA;
    }

    @Override
    public double getAlturaResidual() {
        return ALTURA_RESIDUAL;
    }
}
