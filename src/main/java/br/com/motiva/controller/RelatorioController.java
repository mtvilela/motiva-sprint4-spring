package br.com.motiva.controller;

import br.com.motiva.dto.RelatorioResponse;
import br.com.motiva.service.GeradorRelatorio;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

// O motor de prioridade das Sprints 2/3 exposto como servico REST.
// Toda a classificacao acontece no GeradorRelatorio/MotorDeRegras (@Service);
// este Controller so conhece HTTP.
@RestController
@RequestMapping("/api/relatorios")
public class RelatorioController {

    private final GeradorRelatorio gerador;

    public RelatorioController(GeradorRelatorio gerador) {
        this.gerador = gerador;
    }

    // POST /api/relatorios  -> 201: gera a partir dos trechos atuais e persiste no historico
    @PostMapping
    public ResponseEntity<RelatorioResponse> gerar() {
        RelatorioResponse gerado = gerador.gerarRelatorio();
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(gerado.id())
                .toUri();
        return ResponseEntity.created(location).body(gerado);
    }

    // GET /api/relatorios  -> 200: historico completo (mais recente primeiro)
    @GetMapping
    public ResponseEntity<List<RelatorioResponse>> listarHistorico() {
        return ResponseEntity.ok(gerador.listarHistorico());
    }

    // GET /api/relatorios/periodo?inicio=2026-09-01&fim=2026-09-30  -> 200 | 400
    @GetMapping("/periodo")
    public ResponseEntity<List<RelatorioResponse>> consultarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return ResponseEntity.ok(gerador.consultarPorPeriodo(inicio, fim));
    }

    // GET /api/relatorios/{id}  -> 200 | 404
    @GetMapping("/{id}")
    public ResponseEntity<RelatorioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(gerador.buscarPorId(id));
    }
}
