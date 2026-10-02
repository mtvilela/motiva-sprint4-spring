package br.com.motiva.repository;

import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.RegistroIntervencao;
import br.com.motiva.model.RelatorioHistorico;
import br.com.motiva.model.RelatorioPrioridade;
import br.com.motiva.model.RocadaManual;
import br.com.motiva.model.RocadaMecanizada;
import br.com.motiva.model.TipoIntervencao;
import br.com.motiva.model.TipoTerreno;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.model.TrechoSeco;
import br.com.motiva.model.TrechoUmido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

// @DataJpaTest sobe SO a camada JPA (entidades + repositories), sem web.
// Prova que as derived queries funcionam sem nenhuma linha de SQL escrita
// a mao, inclusive a leitura POLIMORFICA da heranca SINGLE_TABLE.
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryDerivedQueriesTest {

    @Autowired
    private TrechoRodoviaRepository trechos;

    @Autowired
    private EquipeManutencaoRepository equipes;

    @Autowired
    private RegistroIntervencaoRepository intervencoes;

    @Autowired
    private RelatorioHistoricoRepository relatorios;

    @Test
    void sequenceGeraOIdNoSave() {
        TrechoRodovia salvo = trechos.save(new TrechoSeco("Q-ID", 1.0, 10.0));

        assertThat(salvo.getId()).isNotNull().isPositive();
    }

    @Test
    void findByAlturaVegetacaoGreaterThanEqual() {
        trechos.save(new TrechoSeco("Q-40", 1.0, 40.0));
        trechos.save(new TrechoSeco("Q-39", 2.0, 39.9));
        trechos.save(new TrechoUmido("Q-52", 3.0, 52.0, "IOT-Q"));

        assertThat(trechos.findByAlturaVegetacaoGreaterThanEqual(40.0))
                .extracting(TrechoRodovia::getCodigo)
                .containsExactlyInAnyOrder("Q-40", "Q-52");
    }

    @Test
    void findByTipoTerrenoDevolveASubclasseCerta() {
        trechos.save(new TrechoUmido("Q-U2", 20.0, 10.0, "IOT-2"));
        trechos.save(new TrechoSeco("Q-S1", 15.0, 10.0));
        trechos.save(new TrechoUmido("Q-U1", 10.0, 10.0, "IOT-1"));
        trechos.flush();

        assertThat(trechos.findByTipoTerrenoOrderByKmAsc(TipoTerreno.UMIDO))
                .extracting(TrechoRodovia::getCodigo)
                .containsExactly("Q-U1", "Q-U2");
        assertThat(trechos.findByTipoTerrenoOrderByKmAsc(TipoTerreno.UMIDO))
                .allSatisfy(t -> assertThat(t).isInstanceOf(TrechoUmido.class));
        assertThat(trechos.findByCodigo("Q-S1")).get().isInstanceOf(TrechoSeco.class);
        assertThat(trechos.existsByCodigo("Q-S1")).isTrue();
        assertThat(trechos.existsByCodigo("NAO-EXISTE")).isFalse();
    }

    @Test
    void findByTipoDasIntervencoes() {
        TrechoRodovia trecho = trechos.save(new TrechoSeco("Q-INT", 1.0, 50.0));
        EquipeManutencao equipe = equipes.save(new EquipeManutencao("Equipe Q"));
        intervencoes.save(RegistroIntervencao.executar(new RocadaMecanizada(), trecho, equipe));
        intervencoes.save(RegistroIntervencao.executar(new RocadaManual(), trecho, null));

        assertThat(intervencoes.findByTipo(TipoIntervencao.MECANIZADA)).hasSize(1)
                .first().satisfies(r -> assertThat(r.getEquipe().getNome()).isEqualTo("Equipe Q"));
        assertThat(intervencoes.findByTipo(TipoIntervencao.PULVERIZACAO)).isEmpty();
        assertThat(intervencoes.countByTrechoId(trecho.getId())).isEqualTo(2);
        assertThat(intervencoes.countByEquipeId(equipe.getId())).isEqualTo(1);
    }

    @Test
    void equipesLivresEBuscaPorNome() {
        TrechoRodovia trecho = trechos.save(new TrechoSeco("Q-EQ", 1.0, 50.0));
        EquipeManutencao norte = new EquipeManutencao("Equipe Norte Q");
        norte.associarTrecho(trecho);
        equipes.save(norte);
        equipes.save(new EquipeManutencao("Equipe Sul Q"));

        assertThat(equipes.findByTrechoResponsavelIsNullOrderByNomeAsc())
                .extracting(EquipeManutencao::getNome).containsExactly("Equipe Sul Q");
        assertThat(equipes.findByNomeContainingIgnoreCaseOrderByNomeAsc("NORTE"))
                .extracting(EquipeManutencao::getNome).containsExactly("Equipe Norte Q");
        assertThat(equipes.existsByTrechoResponsavelId(trecho.getId())).isTrue();
    }

    @Test
    void historicoPorPeriodoComColunaVirtual() {
        RelatorioHistorico salvo = relatorios.saveAndFlush(RelatorioHistorico.de(new RelatorioPrioridade()));

        assertThat(salvo.getDataGeracao()).isNotNull();   // @PrePersist
        assertThat(salvo.getTotalTrechos()).isZero();     // calculada pelo banco (@Generated)

        LocalDateTime agora = LocalDateTime.now();
        assertThat(relatorios.findByDataGeracaoBetweenOrderByDataGeracaoDesc(agora.minusHours(1), agora.plusHours(1)))
                .extracting(RelatorioHistorico::getId).containsExactly(salvo.getId());
        assertThat(relatorios.findByDataGeracaoBetweenOrderByDataGeracaoDesc(agora.minusDays(10), agora.minusDays(9)))
                .isEmpty();
    }
}
