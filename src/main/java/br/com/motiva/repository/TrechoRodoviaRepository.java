package br.com.motiva.repository;

import br.com.motiva.model.TipoTerreno;
import br.com.motiva.model.TrechoRodovia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// Sprint 3: TrechoRodoviaDAO (~165 linhas de JDBC: SQL escrito a mao, parametros e mapear()).
// Sprint 4: so esta INTERFACE. Quem escreve a implementacao e o Spring Data,
// em tempo de execucao (um proxy criado na subida da aplicacao); o SQL e
// gerado pelo Hibernate.
//
// Herdados de JpaRepository: save(), findById(), findAll(), deleteById(),
// existsById(), count()... A consulta e POLIMORFICA: cada linha volta como
// TrechoSeco ou TrechoUmido conforme a coluna TIPO_TERRENO.
public interface TrechoRodoviaRepository extends JpaRepository<TrechoRodovia, Long> {

    // ===== Derived queries: o Spring le o NOME do metodo e monta a consulta =====

    // ... WHERE ALTURA_VEGETACAO >= ?
    List<TrechoRodovia> findByAlturaVegetacaoGreaterThanEqual(Double minimo);

    // ... WHERE TIPO_TERRENO = ? ORDER BY KM
    List<TrechoRodovia> findByTipoTerrenoOrderByKmAsc(TipoTerreno tipo);

    // ... WHERE TIPO_TERRENO = ? AND ALTURA_VEGETACAO >= ?
    List<TrechoRodovia> findByTipoTerrenoAndAlturaVegetacaoGreaterThanEqual(TipoTerreno tipo, Double minimo);

    // ... ORDER BY KM
    List<TrechoRodovia> findAllByOrderByKmAsc();

    // ... WHERE CODIGO = ?
    Optional<TrechoRodovia> findByCodigo(String codigo);

    // Regra de codigo unico (UK_MTV_TRECHO_CODIGO) checada antes do INSERT/UPDATE.
    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);
}
