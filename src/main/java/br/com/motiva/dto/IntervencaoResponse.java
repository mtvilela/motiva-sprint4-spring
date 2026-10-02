package br.com.motiva.dto;

import br.com.motiva.model.RegistroIntervencao;
import br.com.motiva.model.TipoIntervencao;

import java.time.LocalDateTime;

// Contrato de SAIDA de uma intervencao registrada.
// servico = nome da subclasse de IntervencaoOperacional recriada pela fabrica
// TipoIntervencao.criar() ("Rocada Mecanizada", "Pulverizacao"...).
public record IntervencaoResponse(
        Long id,
        TipoIntervencao tipo,
        String servico,
        TrechoResumo trecho,
        EquipeResumo equipe,
        LocalDateTime dataExecucao,
        double alturaAntes,
        double alturaDepois,
        String descricao) {

    public static IntervencaoResponse de(RegistroIntervencao registro) {
        return new IntervencaoResponse(
                registro.getId(),
                registro.getTipo(),
                registro.criarIntervencao().getNome(),
                TrechoResumo.de(registro.getTrecho()),
                EquipeResumo.de(registro.getEquipe()),
                registro.getDataExecucao(),
                registro.getAlturaAntes(),
                registro.getAlturaDepois(),
                registro.getDescricao());
    }
}
