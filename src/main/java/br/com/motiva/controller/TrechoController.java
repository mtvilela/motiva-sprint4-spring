package br.com.motiva.controller;

import br.com.motiva.dto.IntervencaoResponse;
import br.com.motiva.dto.TrechoRequest;
import br.com.motiva.dto.TrechoResponse;
import br.com.motiva.model.TipoTerreno;
import br.com.motiva.service.IntervencaoService;
import br.com.motiva.service.TrechoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// Interface HTTP dos trechos. O Controller NAO tem regra de negocio: recebe a
// requisicao, valida o formato (@Valid), chama o Service e devolve o status
// HTTP correto. Erros (404/400/409) viram JSON no GlobalExceptionHandler.
@RestController
@RequestMapping("/api/trechos")
public class TrechoController {

    private final TrechoService service;
    private final IntervencaoService intervencaoService;

    public TrechoController(TrechoService service, IntervencaoService intervencaoService) {
        this.service = service;
        this.intervencaoService = intervencaoService;
    }

    // GET /api/trechos  -> 200 (filtros opcionais: ?alturaMinima=25&tipoTerreno=UMIDO)
    @GetMapping
    public ResponseEntity<List<TrechoResponse>> listar(
            @RequestParam(required = false) Double alturaMinima,
            @RequestParam(required = false) TipoTerreno tipoTerreno) {
        return ResponseEntity.ok(service.listar(alturaMinima, tipoTerreno));
    }

    // GET /api/trechos/criticos  -> 200
    @GetMapping("/criticos")
    public ResponseEntity<List<TrechoResponse>> listarCriticos() {
        return ResponseEntity.ok(service.listarCriticos());
    }

    // GET /api/trechos/{id}  -> 200 | 404
    @GetMapping("/{id}")
    public ResponseEntity<TrechoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // GET /api/trechos/codigo/{codigo}  -> 200 | 404
    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<TrechoResponse> buscarPorCodigo(@PathVariable String codigo) {
        return ResponseEntity.ok(service.buscarPorCodigo(codigo));
    }

    // GET /api/trechos/{id}/intervencoes  -> 200 | 404
    @GetMapping("/{id}/intervencoes")
    public ResponseEntity<List<IntervencaoResponse>> listarIntervencoes(@PathVariable Long id) {
        return ResponseEntity.ok(intervencaoService.listarPorTrecho(id));
    }

    // POST /api/trechos  -> 201 + header Location + corpo criado | 400 | 409
    @PostMapping
    public ResponseEntity<TrechoResponse> criar(@Valid @RequestBody TrechoRequest request) {
        TrechoResponse criado = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criado.id())
                .toUri();
        return ResponseEntity.created(location).body(criado);
    }

    // PUT /api/trechos/{id}  -> 200 | 400 | 404 | 409
    @PutMapping("/{id}")
    public ResponseEntity<TrechoResponse> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody TrechoRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    // DELETE /api/trechos/{id}  -> 204 | 404 | 409
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/trechos/{id}/simular-crescimento?dias=10  -> 200 | 400 | 404
    @PostMapping("/{id}/simular-crescimento")
    public ResponseEntity<TrechoResponse> simularCrescimento(@PathVariable Long id, @RequestParam int dias) {
        return ResponseEntity.ok(service.simularCrescimento(id, dias));
    }
}
