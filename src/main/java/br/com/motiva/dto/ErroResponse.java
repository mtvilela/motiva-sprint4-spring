package br.com.motiva.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

// Corpo padrao de TODAS as respostas de erro da API (montado pelo
// GlobalExceptionHandler). campos so aparece em erros de validacao:
// { "nomeDoCampo": "mensagem" }.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        LocalDateTime timestamp,
        int status,
        String erro,
        String mensagem,
        String caminho,
        Map<String, String> campos) {
}
