package br.com.motiva.model;

// Tipos concretos de intervencao existentes no sistema. E o valor gravado na
// coluna TIPO da tabela T_MTV_INTERVENCAO e tambem a FABRICA que reconstroi
// a subclasse correta de IntervencaoOperacional a partir do registro.
public enum TipoIntervencao {
    MECANIZADA,
    MANUAL,
    PULVERIZACAO;

    // FABRICA: devolve a intervencao concreta (POLIMORFISMO) para este tipo.
    public IntervencaoOperacional criar() {
        return switch (this) {
            case MECANIZADA -> new RocadaMecanizada();
            case MANUAL -> new RocadaManual();
            case PULVERIZACAO -> new Pulverizacao();
        };
    }
}
