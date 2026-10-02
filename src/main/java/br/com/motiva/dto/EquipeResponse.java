package br.com.motiva.dto;

import br.com.motiva.model.EquipeManutencao;

// Contrato de SAIDA de uma equipe. trechoResponsavel vem null quando a
// equipe esta livre.
public record EquipeResponse(Long id, String nome, TrechoResumo trechoResponsavel) {

    public static EquipeResponse de(EquipeManutencao equipe) {
        return new EquipeResponse(equipe.getId(), equipe.getNome(), TrechoResumo.de(equipe.getTrechoResponsavel()));
    }
}
