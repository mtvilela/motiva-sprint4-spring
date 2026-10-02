package br.com.motiva.dto;

import br.com.motiva.model.TipoTerreno;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// Contrato de ENTRADA de POST/PUT /api/trechos. A entidade nunca e exposta na
// API: o cliente manda so o que pode definir (sem id, sem prioridade).
//
// O Bean Validation barra o JSON fora do contrato ja na entrada (@Valid no
// Controller -> 400 com o nome do campo). As regras de negocio (ex.: altura
// >= 0, sensor obrigatorio para UMIDO) sao garantidas de novo no TrechoService,
// que vale para qualquer chamador. Os maximos seguem a precisao das colunas
// (NUMBER(8,2), NUMBER(6,2), NUMBER(5)).
public record TrechoRequest(
        @NotBlank(message = "e obrigatorio")
        @Size(max = 20, message = "deve ter no maximo 20 caracteres")
        String codigo,

        @NotNull(message = "e obrigatorio")
        @PositiveOrZero(message = "nao pode ser negativo")
        @DecimalMax(value = "999999.99", message = "deve ser no maximo 999999.99")
        Double km,

        @NotNull(message = "e obrigatoria")
        @PositiveOrZero(message = "nao pode ser negativa")
        @DecimalMax(value = "9999.99", message = "deve ser no maximo 9999.99 cm")
        Double alturaVegetacao,

        // Opcional: se nao vier, o trecho comeca com 0 dias sem rocada.
        @PositiveOrZero(message = "nao pode ser negativo")
        @Max(value = 99999, message = "deve ser no maximo 99999")
        Integer diasSemRocada,

        @NotNull(message = "e obrigatorio (SECO ou UMIDO)")
        TipoTerreno tipoTerreno,

        // Obrigatorio para UMIDO e proibido para SECO (regra checada no Service).
        @Size(max = 20, message = "deve ter no maximo 20 caracteres")
        String idSensor) {
}
