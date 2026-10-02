package br.com.motiva.model;

import java.util.Locale;

// Classe ABSTRATA: representa o conceito GENERICO de uma intervencao de
// manutencao da vegetacao. Nao existe "executar uma intervencao qualquer":
// e sempre preciso saber QUAL servico sera prestado (rocada mecanizada,
// manual, pulverizacao, ...). Por isso a classe e abstrata e executarServico()
// e um metodo abstrato, obrigando cada filha a definir seu comportamento.
//
// Separacao de papeis (igual a Sprint 3):
//   - IntervencaoOperacional (esta hierarquia) = COMO o servico e feito (comportamento, nao e tabela);
//   - RegistroIntervencao (@Entity)           = O QUE aconteceu (linha de T_MTV_INTERVENCAO).
public abstract class IntervencaoOperacional {

    protected final String nome;
    protected final double rendimentoKmPorDia; // produtividade da equipe

    protected IntervencaoOperacional(String nome, double rendimentoKmPorDia) {
        this.nome = nome;
        this.rendimentoKmPorDia = rendimentoKmPorDia;
    }

    // Metodo ABSTRATO: cada tipo de intervencao executa o servico de um jeito
    // e deixa a vegetacao em uma altura residual diferente.
    public abstract String executarServico(TrechoRodovia trecho);

    // Tipo persistido no banco (coluna TIPO).
    public abstract TipoIntervencao getTipo();

    // Altura (cm) em que a vegetacao fica apos o servico.
    public abstract double getAlturaResidual();

    public String getNome() {
        return nome;
    }

    public double getRendimentoKmPorDia() {
        return rendimentoKmPorDia;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%s (%.1f km/dia, residual %.1f cm)", nome, rendimentoKmPorDia, getAlturaResidual());
    }
}
