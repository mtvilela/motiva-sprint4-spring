package br.com.motiva.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Contrato de ENTRADA de POST/PUT /api/equipes.
// trechoId e opcional: null = equipe sem trecho (ou desassociar, no PUT).
public record EquipeRequest(
        @NotBlank(message = "e obrigatorio")
        @Size(max = 60, message = "deve ter no maximo 60 caracteres")
        String nome,

        @Positive(message = "deve ser um id valido")
        Long trechoId) {
}
