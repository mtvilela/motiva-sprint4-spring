package br.com.motiva.service;

import br.com.motiva.dto.IntervencaoRequest;
import br.com.motiva.dto.IntervencaoResponse;
import br.com.motiva.dto.IntervencaoUpdateRequest;
import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.RegistroIntervencao;
import br.com.motiva.model.TipoIntervencao;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.EquipeManutencaoRepository;
import br.com.motiva.repository.RegistroIntervencaoRepository;
import br.com.motiva.repository.TrechoRodoviaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Regras de negocio das intervencoes operacionais: escolher o servico (pelo
// cliente ou pelo MotorDeRegras), executa-lo no trecho (polimorfismo) e
// registrar o historico.
@Service
public class IntervencaoService {

    private static final Comparator<RegistroIntervencao> MAIS_RECENTE_PRIMEIRO =
            Comparator.comparing(RegistroIntervencao::getDataExecucao).reversed();

    private final RegistroIntervencaoRepository repository;
    private final TrechoRodoviaRepository trechoRepository;
    private final EquipeManutencaoRepository equipeRepository;
    private final MotorDeRegras motor;

    public IntervencaoService(RegistroIntervencaoRepository repository,
                              TrechoRodoviaRepository trechoRepository,
                              EquipeManutencaoRepository equipeRepository,
                              MotorDeRegras motor) {
        this.repository = repository;
        this.trechoRepository = trechoRepository;
        this.equipeRepository = equipeRepository;
        this.motor = motor;
    }

    // GET /api/intervencoes (filtro opcional ?tipo= usa a derived query findByTipo).
    @Transactional(readOnly = true)
    public List<IntervencaoResponse> listar(TipoIntervencao tipo) {
        List<RegistroIntervencao> registros = tipo == null
                ? repository.findAllByOrderByDataExecucaoDesc()
                : repository.findByTipo(tipo).stream().sorted(MAIS_RECENTE_PRIMEIRO).toList();
        return registros.stream().map(IntervencaoResponse::de).toList();
    }

    // GET /api/trechos/{id}/intervencoes: historico de servicos de um trecho.
    @Transactional(readOnly = true)
    public List<IntervencaoResponse> listarPorTrecho(Long trechoId) {
        buscarTrecho(trechoId); // 404 se o trecho nao existir
        return repository.findByTrechoIdOrderByDataExecucaoDesc(trechoId).stream()
                .map(IntervencaoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public IntervencaoResponse buscarPorId(Long id) {
        return IntervencaoResponse.de(buscarEntidade(id));
    }

    // POST /api/intervencoes: EXECUTA o servico no trecho e registra o resultado.
    @Transactional
    public IntervencaoResponse registrar(IntervencaoRequest request) {
        TrechoRodovia trecho = buscarTrecho(request.trechoId());
        EquipeManutencao equipe = request.equipeId() == null ? null : buscarEquipe(request.equipeId());

        IntervencaoOperacional servico;
        if (request.tipo() != null) {
            servico = request.tipo().criar(); // fabrica -> subclasse concreta
        } else {
            // Sem tipo informado, quem decide e o motor de regras (Sprint 2).
            Prioridade prioridade = motor.classificar(trecho);
            servico = motor.recomendarIntervencao(trecho, prioridade);
            if (servico == null) {
                throw new RegraNegocioException(String.format(Locale.ROOT,
                        "O trecho %s esta com prioridade %s (%.1f cm): o motor de regras nao recomenda"
                                + " intervencao. Informe o 'tipo' para registrar mesmo assim.",
                        trecho.getCodigo(), prioridade, trecho.getAlturaVegetacao()));
            }
        }

        // POLIMORFISMO: cada subclasse executa o servico do seu jeito e deixa a
        // vegetacao em uma altura residual diferente.
        RegistroIntervencao registro = RegistroIntervencao.executar(servico, trecho, equipe);

        // Nao precisa chamar trechoRepository.save(trecho): o trecho e uma
        // entidade GERENCIADA dentro desta transacao, entao o Hibernate grava a
        // nova altura no commit (dirty checking). Na Sprint 3 o Main precisava
        // chamar daoTrecho.atualizar(trecho) na mao.
        return IntervencaoResponse.de(repository.save(registro));
    }

    // PUT /api/intervencoes/{id}: corrige o registro historico (nao re-executa o servico).
    @Transactional
    public IntervencaoResponse atualizar(Long id, IntervencaoUpdateRequest request) {
        RegistroIntervencao registro = buscarEntidade(id);
        validarCorrecao(request);

        TrechoRodovia trecho = buscarTrecho(request.trechoId());
        EquipeManutencao equipe = request.equipeId() == null ? null : buscarEquipe(request.equipeId());
        registro.corrigir(request.tipo(), trecho, equipe, request.dataExecucao(),
                request.alturaAntes(), request.alturaDepois(), request.descricao());
        return IntervencaoResponse.de(repository.save(registro));
    }

    @Transactional
    public void remover(Long id) {
        repository.delete(buscarEntidade(id));
    }

    // ----- regras de negocio -----

    private static void validarCorrecao(IntervencaoUpdateRequest request) {
        if (request.tipo() == null) {
            throw new RegraNegocioException("O tipo da intervencao e obrigatorio.");
        }
        if (request.dataExecucao() == null || request.dataExecucao().isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A data de execucao e obrigatoria e nao pode estar no futuro.");
        }
        if (request.alturaAntes() == null || request.alturaDepois() == null
                || request.alturaAntes() < 0 || request.alturaDepois() < 0) {
            throw new RegraNegocioException("As alturas antes e depois sao obrigatorias e nao podem ser negativas.");
        }
        // Uma intervencao corta ou resseca a vegetacao: nunca a deixa mais alta.
        if (request.alturaDepois() > request.alturaAntes()) {
            throw new RegraNegocioException("A altura depois da intervencao nao pode ser maior que a altura antes.");
        }
    }

    private RegistroIntervencao buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Intervencao " + id + " nao encontrada."));
    }

    private TrechoRodovia buscarTrecho(Long trechoId) {
        return trechoRepository.findById(trechoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trecho " + trechoId + " nao encontrado."));
    }

    private EquipeManutencao buscarEquipe(Long equipeId) {
        return equipeRepository.findById(equipeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe " + equipeId + " nao encontrada."));
    }
}
