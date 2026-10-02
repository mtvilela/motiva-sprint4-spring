package br.com.motiva.dto;

import br.com.motiva.model.RegistroIntervencao;
import br.com.motiva.model.TipoIntervencao;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// Contrato de ENTRADA de PUT /api/intervencoes/{id}: CORRIGE o registro
// historico (substituicao completa, semantica de PUT). Nao executa o servico
// de novo, entao nao mexe na altura atual do trecho.
public record IntervencaoUpdateRequest(
        @NotNull(message = "e obrigatorio (MECANIZADA, MANUAL ou PULVERIZACAO)")
        TipoIntervencao tipo,

        @NotNull(message = "e obrigatorio")
        @Positive(message = "deve ser um id valido")
        Long trechoId,

        @Positive(message = "deve ser um id valido")
        Long equipeId,

        @NotNull(message = "e obrigatoria")
        @PastOrPresent(message = "nao pode estar no futuro")
        LocalDateTime dataExecucao,

        @NotNull(message = "e obrigatoria")
        @PositiveOrZero(message = "nao pode ser negativa")
        @DecimalMax(value = "9999.99", message = "deve ser no maximo 9999.99 cm")
        Double alturaAntes,

        @NotNull(message = "e obrigatoria")
        @PositiveOrZero(message = "nao pode ser negativa")
        @DecimalMax(value = "9999.99", message = "deve ser no maximo 9999.99 cm")
        Double alturaDepois,

        @Size(max = RegistroIntervencao.TAMANHO_MAX_DESCRICAO, message = "deve ter no maximo 200 caracteres")
        String descricao) {
}
