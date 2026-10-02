package br.com.motiva.exception;

// Lancada pelos Services quando a operacao conflita com o estado atual do
// banco: codigo/nome duplicado (UNIQUE) ou exclusao de registro que ainda
// tem dependentes (FK). O GlobalExceptionHandler transforma em HTTP 409 Conflict.
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
