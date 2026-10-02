package br.com.motiva.exception;

import br.com.motiva.dto.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// @ControllerAdvice: tratamento GLOBAL de erros. Nenhum Controller tem
// try/catch: qualquer excecao lancada nos Services (ou pelo proprio Spring)
// sobe ate aqui e vira um JSON padronizado (ErroResponse) com o status HTTP
// correto.
//
//   RecursoNaoEncontradoException / rota inexistente      -> 404
//   RegraNegocioException / Bean Validation / JSON ruim   -> 400
//   ConflitoException / violacao de constraint no banco   -> 409
//   qualquer outra coisa                                  -> 500 (com log)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ===== Excecoes do proprio sistema (lancadas pelos Services) =====

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarNaoEncontrado(RecursoNaoEncontradoException ex,
                                                            HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> tratarConflito(ConflitoException ex, HttpServletRequest request) {
        return responder(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // ===== Validacao e formato da requisicao =====

    // Bean Validation no corpo (@Valid @RequestBody): devolve cada campo invalido.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(MethodArgumentNotValidException ex,
                                                        HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.merge(erro.getField(), erro.getDefaultMessage(), (atual, nova) -> atual + "; " + nova);
        }
        return responder(HttpStatus.BAD_REQUEST, "Dados invalidos: corrija os campos indicados.", request, campos);
    }

    // Constraints direto em parametros de metodo (@RequestParam, @PathVariable).
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErroResponse> tratarValidacaoParametros(HandlerMethodValidationException ex,
                                                                  HttpServletRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(resultado -> {
            String parametro = resultado.getMethodParameter().getParameterName();
            resultado.getResolvableErrors().forEach(erro ->
                    campos.merge(parametro, erro.getDefaultMessage(), (atual, nova) -> atual + "; " + nova));
        });
        return responder(HttpStatus.BAD_REQUEST, "Parametros invalidos.", request, campos);
    }

    // JSON mal formado, tipo errado ("km": "abc") ou enum inexistente ("tipoTerreno": "BARRO").
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarJsonInvalido(HttpMessageNotReadableException ex,
                                                           HttpServletRequest request) {
        String mensagem = "Corpo da requisicao ausente ou com JSON mal formatado.";
        if (ex.getCause() instanceof MismatchedInputException erro && !erro.getPath().isEmpty()) {
            mensagem = "Valor invalido para o campo '" + caminho(erro) + "'"
                    + valoresAceitos(erro.getTargetType()) + ".";
        }
        return responder(HttpStatus.BAD_REQUEST, mensagem, request, null);
    }

    // Parametro de URL com tipo errado: /api/trechos/abc, ?tipo=XYZ, ?inicio=31-12-2026...
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoParametro(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest request) {
        String mensagem = "Valor '" + ex.getValue() + "' invalido para o parametro '" + ex.getName() + "'"
                + valoresAceitos(ex.getRequiredType()) + ".";
        return responder(HttpStatus.BAD_REQUEST, mensagem, request, null);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> tratarParametroAusente(MissingServletRequestParameterException ex,
                                                               HttpServletRequest request) {
        return responder(HttpStatus.BAD_REQUEST,
                "O parametro '" + ex.getParameterName() + "' e obrigatorio.", request, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaInexistente(NoResourceFoundException ex,
                                                              HttpServletRequest request) {
        return responder(HttpStatus.NOT_FOUND,
                "Rota nao encontrada: " + request.getMethod() + " " + request.getRequestURI(), request, null);
    }

    // ===== Banco de dados =====

    // Rede de seguranca: os Services ja checam codigo/nome unico e dependencias,
    // mas se o banco recusar (UNIQUE, FK, CHECK), devolvemos 409 em vez de 500.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(DataIntegrityViolationException ex,
                                                          HttpServletRequest request) {
        log.warn("Restricao do banco violada em {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT,
                "A operacao viola uma restricao do banco de dados (registro duplicado ou em uso).", request, null);
    }

    // ===== Qualquer outra excecao =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarInesperado(Exception ex, HttpServletRequest request) {
        // Excecoes do Spring MVC que ja carregam o status certo (405, 406, 415...).
        if (ex instanceof ErrorResponse erroSpring) {
            HttpStatus status = HttpStatus.valueOf(erroSpring.getStatusCode().value());
            String detalhe = erroSpring.getBody().getDetail();
            return responder(status, detalhe != null ? detalhe : status.getReasonPhrase(), request, null,
                    erroSpring.getHeaders());
        }
        log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno inesperado. Consulte o log da aplicacao.", request, null);
    }

    // ----- utilitarios -----

    private static ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem,
                                                          HttpServletRequest request, Map<String, String> campos) {
        return responder(status, mensagem, request, campos, HttpHeaders.EMPTY);
    }

    private static ResponseEntity<ErroResponse> responder(HttpStatus status, String mensagem,
                                                          HttpServletRequest request, Map<String, String> campos,
                                                          HttpHeaders headers) {
        ErroResponse corpo = new ErroResponse(
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                campos);
        return ResponseEntity.status(status).headers(headers).body(corpo);
    }

    // "itens[0].tipo" a partir do caminho que o Jackson percorreu no JSON.
    private static String caminho(JacksonException erro) {
        return erro.getPath().stream()
                .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."))
                .replace(".[", "[");
    }

    // Para enums, lista os valores aceitos: " (valores aceitos: [SECO, UMIDO])".
    private static String valoresAceitos(Class<?> tipo) {
        if (tipo != null && tipo.isEnum()) {
            return " (valores aceitos: " + Arrays.toString(tipo.getEnumConstants()) + ")";
        }
        return "";
    }
}
