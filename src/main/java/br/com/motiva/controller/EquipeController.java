package br.com.motiva.controller;

import br.com.motiva.dto.EquipeRequest;
import br.com.motiva.dto.EquipeResponse;
import br.com.motiva.service.EquipeService;
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

// Interface HTTP das equipes de manutencao.
@RestController
@RequestMapping("/api/equipes")
public class EquipeController {

    private final EquipeService service;

    public EquipeController(EquipeService service) {
        this.service = service;
    }

    // GET /api/equipes  -> 200 (filtro opcional: ?nome=norte)
    @GetMapping
    public ResponseEntity<List<EquipeResponse>> listar(@RequestParam(required = false) String nome) {
        return ResponseEntity.ok(service.listar(nome));
    }

    // GET /api/equipes/disponiveis  -> 200 (equipes sem trecho)
    @GetMapping("/disponiveis")
    public ResponseEntity<List<EquipeResponse>> listarDisponiveis() {
        return ResponseEntity.ok(service.listarDisponiveis());
    }

    // GET /api/equipes/{id}  -> 200 | 404
    @GetMapping("/{id}")
    public ResponseEntity<EquipeResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    // POST /api/equipes  -> 201 | 400 | 404 (trecho) | 409
    @PostMapping
    public ResponseEntity<EquipeResponse> criar(@Valid @RequestBody EquipeRequest request) {
        EquipeResponse criada = service.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.id())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    // PUT /api/equipes/{id}  -> 200 | 400 | 404 | 409
    @PutMapping("/{id}")
    public ResponseEntity<EquipeResponse> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody EquipeRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    // DELETE /api/equipes/{id}  -> 204 | 404 | 409
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
