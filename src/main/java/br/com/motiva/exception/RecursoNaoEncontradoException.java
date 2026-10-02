package br.com.motiva.exception;

// Lancada pelos Services quando o ID pedido nao existe no banco.
// O GlobalExceptionHandler transforma em HTTP 404 Not Found.
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
