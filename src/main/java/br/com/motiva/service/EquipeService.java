package br.com.motiva.service;

import br.com.motiva.dto.EquipeRequest;
import br.com.motiva.dto.EquipeResponse;
import br.com.motiva.exception.ConflitoException;
import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.EquipeManutencaoRepository;
import br.com.motiva.repository.RegistroIntervencaoRepository;
import br.com.motiva.repository.TrechoRodoviaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

// Regras de negocio das equipes de manutencao, incluindo a regra da Sprint 1:
// uma equipe so pode ser associada a um trecho CRITICO.
@Service
public class EquipeService {

    private final EquipeManutencaoRepository repository;
    private final TrechoRodoviaRepository trechoRepository;
    private final RegistroIntervencaoRepository intervencaoRepository;
    private final MotorDeRegras motor;

    public EquipeService(EquipeManutencaoRepository repository,
                         TrechoRodoviaRepository trechoRepository,
                         RegistroIntervencaoRepository intervencaoRepository,
                         MotorDeRegras motor) {
        this.repository = repository;
        this.trechoRepository = trechoRepository;
        this.intervencaoRepository = intervencaoRepository;
        this.motor = motor;
    }

    // GET /api/equipes (filtro opcional ?nome= por trecho do nome, sem diferenciar maiusculas).
    @Transactional(readOnly = true)
    public List<EquipeResponse> listar(String nome) {
        List<EquipeManutencao> equipes = (nome == null || nome.isBlank())
                ? repository.findAllByOrderByNomeAsc()
                : repository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome.trim());
        return equipes.stream().map(EquipeResponse::de).toList();
    }

    // Equipes sem trecho associado, prontas para assumir um trecho critico.
    @Transactional(readOnly = true)
    public List<EquipeResponse> listarDisponiveis() {
        return repository.findByTrechoResponsavelIsNullOrderByNomeAsc().stream()
                .map(EquipeResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public EquipeResponse buscarPorId(Long id) {
        return EquipeResponse.de(buscarEntidade(id));
    }

    @Transactional
    public EquipeResponse criar(EquipeRequest request) {
        String nome = validarNome(request.nome());
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("Ja existe uma equipe chamada '" + nome + "'.");
        }

        EquipeManutencao equipe = new EquipeManutencao(nome);
        if (request.trechoId() != null) {
            equipe.associarTrecho(buscarTrechoCritico(request.trechoId()));
        }
        return EquipeResponse.de(repository.save(equipe));
    }

    @Transactional
    public EquipeResponse atualizar(Long id, EquipeRequest request) {
        EquipeManutencao equipe = buscarEntidade(id);
        String nome = validarNome(request.nome());
        if (repository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new ConflitoException("Ja existe outra equipe chamada '" + nome + "'.");
        }

        equipe.renomear(nome);
        if (request.trechoId() == null) {
            equipe.desassociarTrecho();
        } else if (!jaResponsavelPor(equipe, request.trechoId())) {
            // A regra do trecho critico vale para uma NOVA associacao. Manter o
            // trecho atual e permitido: ele pode ter sido rocado depois da
            // associacao e nao estar mais critico (mesma decisao da Sprint 3).
            equipe.associarTrecho(buscarTrechoCritico(request.trechoId()));
        }
        return EquipeResponse.de(repository.save(equipe));
    }

    @Transactional
    public void remover(Long id) {
        EquipeManutencao equipe = buscarEntidade(id);
        long intervencoes = intervencaoRepository.countByEquipeId(id);
        if (intervencoes > 0) {
            throw new ConflitoException("A equipe '" + equipe.getNome() + "' possui " + intervencoes
                    + " intervencao(oes) registrada(s) e nao pode ser removida.");
        }
        repository.delete(equipe);
    }

    // ----- regras de negocio -----

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("O nome da equipe e obrigatorio.");
        }
        return nome.trim();
    }

    // Regra da Sprint 1: uma equipe so pode ser associada a um trecho CRITICO.
    private TrechoRodovia buscarTrechoCritico(Long trechoId) {
        TrechoRodovia trecho = trechoRepository.findById(trechoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trecho " + trechoId + " nao encontrado."));
        if (!motor.estaCritico(trecho)) {
            throw new RegraNegocioException(String.format(Locale.ROOT,
                    "A equipe so pode ser associada a um trecho CRITICO (>= %.0f cm). O trecho %s esta com %.1f cm (%s).",
                    Prioridade.CRITICA.getAlturaMinimaCm(), trecho.getCodigo(),
                    trecho.getAlturaVegetacao(), motor.classificar(trecho)));
        }
        return trecho;
    }

    private static boolean jaResponsavelPor(EquipeManutencao equipe, Long trechoId) {
        TrechoRodovia atual = equipe.getTrechoResponsavel();
        return atual != null && trechoId.equals(atual.getId());
    }

    private EquipeManutencao buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe " + id + " nao encontrada."));
    }
}
