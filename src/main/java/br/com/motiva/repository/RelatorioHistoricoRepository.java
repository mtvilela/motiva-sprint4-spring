package br.com.motiva.repository;

import br.com.motiva.model.RelatorioHistorico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

// Sprint 3: RelatorioPrioridadeDAO.
// Sprint 4: interface com as consultas do historico.
public interface RelatorioHistoricoRepository extends JpaRepository<RelatorioHistorico, Long> {

    // ===== Derived queries =====

    // Historico completo, do mais recente para o mais antigo.
    // ... ORDER BY DATA_GERACAO DESC
    List<RelatorioHistorico> findAllByOrderByDataGeracaoDesc();

    // Consulta por periodo (GET /api/relatorios/periodo).
    // ... WHERE DATA_GERACAO BETWEEN ? AND ? ORDER BY DATA_GERACAO DESC
    List<RelatorioHistorico> findByDataGeracaoBetweenOrderByDataGeracaoDesc(LocalDateTime inicio, LocalDateTime fim);
}
