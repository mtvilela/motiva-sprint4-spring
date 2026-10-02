package br.com.motiva.service;

import br.com.motiva.dto.RelatorioResponse;
import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.RelatorioHistorico;
import br.com.motiva.model.RelatorioPrioridade;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.RelatorioHistoricoRepository;
import br.com.motiva.repository.TrechoRodoviaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

// Servico que gera o Relatorio de Prioridade.
//
// Sprint 2: o relatorio era apenas impresso no console.
// Sprint 3: apos imprimir, o resultado era SALVO pelo RelatorioPrioridadeDAO (JDBC).
// Sprint 4: virou @Service e endpoint REST; o historico e salvo com repository.save().
@Service
public class GeradorRelatorio {

    private final MotorDeRegras motor;
    private final TrechoRodoviaRepository trechoRepository;
    private final RelatorioHistoricoRepository repository;

    public GeradorRelatorio(MotorDeRegras motor,
                            TrechoRodoviaRepository trechoRepository,
                            RelatorioHistoricoRepository repository) {
        this.motor = motor;
        this.trechoRepository = trechoRepository;
        this.repository = repository;
    }

    // POST /api/relatorios: gera o relatorio a partir dos trechos ATUAIS do banco.
    @Transactional
    public RelatorioResponse gerarRelatorio() {
        List<TrechoRodovia> trechos = trechoRepository.findAllByOrderByKmAsc();

        // 1. Logica da Sprint 2: o motor classifica e recomenda. Os trechos com IoT
        //    leem o proprio sensor durante a varredura; como sao entidades gerenciadas,
        //    as novas alturas vao para o banco no commit (dirty checking), sem save().
        RelatorioPrioridade relatorio = motor.gerarRelatorio(trechos);

        // 2. Sprint 3 -> 4: salvar o historico. saveAndFlush forca o INSERT agora,
        //    para o Hibernate ja ler a coluna virtual TOTAL_TRECHOS calculada pelo Oracle.
        RelatorioHistorico historico = repository.saveAndFlush(RelatorioHistorico.de(relatorio));

        return RelatorioResponse.de(historico, relatorio);
    }

    // GET /api/relatorios: historico completo, do mais recente para o mais antigo.
    @Transactional(readOnly = true)
    public List<RelatorioResponse> listarHistorico() {
        return repository.findAllByOrderByDataGeracaoDesc().stream()
                .map(RelatorioResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public RelatorioResponse buscarPorId(Long id) {
        return repository.findById(id)
                .map(RelatorioResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Relatorio " + id + " nao encontrado."));
    }

    // GET /api/relatorios/periodo?inicio=2026-09-01&fim=2026-09-30 (datas inclusivas).
    @Transactional(readOnly = true)
    public List<RelatorioResponse> consultarPorPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new RegraNegocioException("Informe as datas de inicio e fim do periodo (formato yyyy-MM-dd).");
        }
        if (inicio.isAfter(fim)) {
            throw new RegraNegocioException("A data de inicio (" + inicio + ") nao pode ser depois da data de fim ("
                    + fim + ").");
        }
        return repository.findByDataGeracaoBetweenOrderByDataGeracaoDesc(inicio.atStartOfDay(), fim.atTime(LocalTime.MAX))
                .stream()
                .map(RelatorioResponse::de)
                .toList();
    }
}
