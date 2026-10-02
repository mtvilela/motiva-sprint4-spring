package br.com.motiva.exception;

// Lancada pelos Services quando uma regra de negocio e violada
// (ex.: altura negativa, equipe em trecho que nao e critico).
// O GlobalExceptionHandler transforma em HTTP 400 Bad Request.
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
