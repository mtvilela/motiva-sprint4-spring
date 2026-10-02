package br.com.motiva.repository;

import br.com.motiva.model.EquipeManutencao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Sprint 3: EquipeManutencaoDAO, com o LEFT JOIN no trecho escrito a mao.
// Sprint 4: interface; o @ManyToOne da entidade ja diz ao Hibernate como juntar.
public interface EquipeManutencaoRepository extends JpaRepository<EquipeManutencao, Long> {

    // @EntityGraph: traz o trecho da equipe no MESMO SELECT (LEFT JOIN), evitando
    // uma consulta extra por equipe ao montar o JSON (problema N+1).
    @EntityGraph(attributePaths = "trechoResponsavel")
    List<EquipeManutencao> findAllByOrderByNomeAsc();

    // ... WHERE UPPER(NOME) LIKE UPPER('%' || ? || '%') ORDER BY NOME
    @EntityGraph(attributePaths = "trechoResponsavel")
    List<EquipeManutencao> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    // Equipes livres: ... WHERE ID_TRECHO IS NULL ORDER BY NOME
    List<EquipeManutencao> findByTrechoResponsavelIsNullOrderByNomeAsc();

    // Regra de nome unico (UK_MTV_EQUIPE_NOME), sem diferenciar maiusculas.
    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    // Usado antes de apagar um trecho (FK_MTV_EQUIPE_TRECHO).
    boolean existsByTrechoResponsavelId(Long idTrecho);
}
