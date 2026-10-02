package br.com.motiva.controller;

import br.com.motiva.dto.IntervencaoRequest;
import br.com.motiva.dto.IntervencaoResponse;
import br.com.motiva.dto.IntervencaoUpdateRequest;
import br.com.motiva.model.TipoIntervencao;
import br.com.motiva.service.IntervencaoService;
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

// Interface HTTP das intervencoes operacionais.
@RestController
@RequestMapping("/api/intervencoes")
public class IntervencaoController {

    private final IntervencaoService service;

    public IntervencaoController(IntervencaoService service) {
        this.service = service;
    }

    // GET /api/intervencoes  -> 200 (filtro opcional: ?tipo=MECANIZADA)
    @GetMapping
    public ResponseEntity<List<IntervencaoResponse>> listar(@RequestParam(required = false) TipoIntervencao tipo) {
        return ResponseEntity.ok(service.listar(tipo));
    }

    // GET /api/intervencoes/{id}  -> 200 | 404
    @GetMapping("/{id}")
    public ResponseEntity<IntervencaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // POST /api/intervencoes  -> 201 | 400 | 404 (trecho/equipe)
    // Executa o servico no trecho (a altura do trecho cai para a residual) e registra.
    @PostMapping
    public ResponseEntity<IntervencaoResponse> registrar(@Valid @RequestBody IntervencaoRequest request) {
        IntervencaoResponse criada = service.registrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    // PUT /api/intervencoes/{id}  -> 200 | 400 | 404
    // Corrige o registro historico (nao executa o servico de novo).
    @PutMapping("/{id}")
    public ResponseEntity<IntervencaoResponse> atualizar(@PathVariable Long id,
                                                         @Valid @RequestBody IntervencaoUpdateRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    // DELETE /api/intervencoes/{id}  -> 204 | 404
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
