package br.com.motiva.dto;

import br.com.motiva.model.TrechoRodovia;

// Versao curta de um trecho, usada dentro das respostas de equipe e intervencao.
public record TrechoResumo(Long id, String codigo, double km, double alturaVegetacao) {

    public static TrechoResumo de(TrechoRodovia trecho) {
        if (trecho == null) {
            return null;
        }
        return new TrechoResumo(trecho.getId(), trecho.getCodigo(), trecho.getKm(), trecho.getAlturaVegetacao());
    }
}
