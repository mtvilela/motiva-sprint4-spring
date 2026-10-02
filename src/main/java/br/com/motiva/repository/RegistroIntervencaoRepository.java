package br.com.motiva.repository;

import br.com.motiva.model.RegistroIntervencao;
import br.com.motiva.model.TipoIntervencao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Sprint 3: IntervencaoOperacionalDAO (SQL_INSERIR, SQL_LISTAR_POR_TRECHO, ...).
// Sprint 4: interface. Nenhuma linha de SQL: o Hibernate gera tudo.
public interface RegistroIntervencaoRepository extends JpaRepository<RegistroIntervencao, Long> {

    // ===== Derived queries =====

    // ... WHERE TIPO = ?   (o enum vira o texto 'MECANIZADA', 'MANUAL'...)
    @EntityGraph(attributePaths = {"trecho", "equipe"})
    List<RegistroIntervencao> findByTipo(TipoIntervencao tipo);

    // ... ORDER BY DATA_EXECUCAO DESC
    @EntityGraph(attributePaths = {"trecho", "equipe"})
    List<RegistroIntervencao> findAllByOrderByDataExecucaoDesc();

    // ... WHERE ID_TRECHO = ? ORDER BY DATA_EXECUCAO DESC  (substitui o SQL_LISTAR_POR_TRECHO)
    @EntityGraph(attributePaths = {"trecho", "equipe"})
    List<RegistroIntervencao> findByTrechoIdOrderByDataExecucaoDesc(Long idTrecho);

    // Usados antes de apagar trecho/equipe (FK_MTV_INTERV_TRECHO / FK_MTV_INTERV_EQUIPE).
    long countByTrechoId(Long idTrecho);

    long countByEquipeId(Long idEquipe);
}
