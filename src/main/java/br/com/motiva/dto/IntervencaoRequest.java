package br.com.motiva.dto;

import br.com.motiva.model.TipoIntervencao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Contrato de ENTRADA de POST /api/intervencoes: EXECUTA um servico no trecho.
// Alturas, data e descricao NAO vem do cliente: sao o resultado do
// executarServico() da subclasse escolhida.
//
// tipo e opcional: se nao vier, o MotorDeRegras recomenda a intervencao a
// partir da prioridade do trecho (regra 2 da Sprint 2).
public record IntervencaoRequest(
        @NotNull(message = "e obrigatorio")
        @Positive(message = "deve ser um id valido")
        Long trechoId,

        @Positive(message = "deve ser um id valido")
        Long equipeId,

        TipoIntervencao tipo) {
}
