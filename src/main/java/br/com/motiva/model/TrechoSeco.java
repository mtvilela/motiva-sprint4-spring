package br.com.motiva.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

// Trecho de terreno SECO: a vegetacao cresce devagar.
// NAO possui tecnologia instalada, entao NAO implementa MonitoravelViaIoT:
// a atualizacao depende de inspecao visual em campo.
//
// Sprint 4: linhas com TIPO_TERRENO = 'SECO' viram objetos desta classe.
@Entity
@DiscriminatorValue("SECO")
public class TrechoSeco extends TrechoRodovia {

    private static final double TAXA_BASE = 0.8; // cm por dia (cresce devagar)

    protected TrechoSeco() {
    }

    public TrechoSeco(String codigo, double km, double alturaInicial) {
        this(codigo, km, alturaInicial, 0);
    }

    public TrechoSeco(String codigo, double km, double alturaVegetacao, int diasSemRocada) {
        super(codigo, km, alturaVegetacao, diasSemRocada, TipoTerreno.SECO);
    }

    @Override
    public double taxaCrescimentoDiaria() {
        return TAXA_BASE;
    }
}
