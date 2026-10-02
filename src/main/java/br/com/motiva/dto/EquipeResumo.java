package br.com.motiva.dto;

import br.com.motiva.model.EquipeManutencao;

// Versao curta de uma equipe, usada dentro da resposta de intervencao.
public record EquipeResumo(Long id, String nome) {

    public static EquipeResumo de(EquipeManutencao equipe) {
        return equipe == null ? null : new EquipeResumo(equipe.getId(), equipe.getNome());
    }
}
