package br.com.motiva.service;

import br.com.motiva.dto.TrechoRequest;
import br.com.motiva.dto.TrechoResponse;
import br.com.motiva.exception.ConflitoException;
import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.Prioridade;
import br.com.motiva.model.TipoTerreno;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.model.TrechoSeco;
import br.com.motiva.model.TrechoUmido;
import br.com.motiva.repository.EquipeManutencaoRepository;
import br.com.motiva.repository.RegistroIntervencaoRepository;
import br.com.motiva.repository.TrechoRodoviaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

// Regras de negocio dos trechos. O Controller so traduz HTTP <-> DTO; tudo o
// que e regra (validacoes da Sprint 1, unicidade do codigo, dependencias antes
// de excluir, simulacao de crescimento) fica aqui, valendo para qualquer
// chamador: REST, testes ou um job futuro.
@Service
public class TrechoService {

    // Os mais urgentes (vegetacao mais alta) primeiro.
    private static final Comparator<TrechoRodovia> MAIS_URGENTE_PRIMEIRO =
            Comparator.comparingDouble(TrechoRodovia::getAlturaVegetacao).reversed()
                    .thenComparingDouble(TrechoRodovia::getKm);

    private final TrechoRodoviaRepository repository;
    private final EquipeManutencaoRepository equipeRepository;
    private final RegistroIntervencaoRepository intervencaoRepository;
    private final MotorDeRegras motor;

    // Injecao de dependencia pelo construtor: o Spring entrega os repositories
    // (proxies gerados pelo Spring Data) e o MotorDeRegras.
    public TrechoService(TrechoRodoviaRepository repository,
                         EquipeManutencaoRepository equipeRepository,
                         RegistroIntervencaoRepository intervencaoRepository,
                         MotorDeRegras motor) {
        this.repository = repository;
        this.equipeRepository = equipeRepository;
        this.intervencaoRepository = intervencaoRepository;
        this.motor = motor;
    }

    // GET /api/trechos com filtros opcionais. Cada combinacao usa uma derived query.
    @Transactional(readOnly = true)
    public List<TrechoResponse> listar(Double alturaMinima, TipoTerreno tipoTerreno) {
        if (alturaMinima != null && alturaMinima < 0) {
            throw new RegraNegocioException("A altura minima do filtro nao pode ser negativa.");
        }
        List<TrechoRodovia> trechos;
        if (alturaMinima != null && tipoTerreno != null) {
            trechos = ordenarPorUrgencia(
                    repository.findByTipoTerrenoAndAlturaVegetacaoGreaterThanEqual(tipoTerreno, alturaMinima));
        } else if (alturaMinima != null) {
            trechos = ordenarPorUrgencia(repository.findByAlturaVegetacaoGreaterThanEqual(alturaMinima));
        } else if (tipoTerreno != null) {
            trechos = repository.findByTipoTerrenoOrderByKmAsc(tipoTerreno);
        } else {
            trechos = repository.findAllByOrderByKmAsc();
        }
        return trechos.stream().map(this::paraResponse).toList();
    }

    // Trechos que ja atingiram a prioridade CRITICA: o limiar vem do dominio,
    // nao de um numero fixo no Controller.
    @Transactional(readOnly = true)
    public List<TrechoResponse> listarCriticos() {
        double limiar = Prioridade.CRITICA.getAlturaMinimaCm();
        return ordenarPorUrgencia(repository.findByAlturaVegetacaoGreaterThanEqual(limiar)).stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrechoResponse buscarPorId(Long id) {
        return paraResponse(buscarEntidade(id));
    }

    @Transactional(readOnly = true)
    public TrechoResponse buscarPorCodigo(String codigo) {
        return repository.findByCodigo(codigo.trim())
                .map(this::paraResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trecho com codigo " + codigo + " nao encontrado."));
    }

    @Transactional
    public TrechoResponse criar(TrechoRequest request) {
        validar(request);
        String codigo = request.codigo().trim();
        if (repository.existsByCodigo(codigo)) {
            throw new ConflitoException("Ja existe um trecho com o codigo " + codigo + ".");
        }

        // A subclasse concreta depende do tipo de terreno (HERANCA + POLIMORFISMO).
        int dias = diasSemRocada(request);
        TrechoRodovia trecho = switch (request.tipoTerreno()) {
            case SECO -> new TrechoSeco(codigo, request.km(), request.alturaVegetacao(), dias);
            case UMIDO -> new TrechoUmido(codigo, request.km(), request.alturaVegetacao(), dias,
                    request.idSensor().trim());
        };

        // Sprint 3: ~40 linhas de JDBC no TrechoRodoviaDAO.inserir(). Sprint 4: 1 linha.
        return paraResponse(repository.save(trecho));
    }

    @Transactional
    public TrechoResponse atualizar(Long id, TrechoRequest request) {
        TrechoRodovia trecho = buscarEntidade(id);
        validar(request);

        // Na heranca SINGLE_TABLE o tipo e a propria classe do objeto: um
        // TrechoSeco nao "vira" TrechoUmido. Para mudar, remove e cadastra de novo.
        if (request.tipoTerreno() != trecho.getTipoTerreno()) {
            throw new RegraNegocioException("O tipo de terreno nao pode ser alterado: o trecho "
                    + trecho.getCodigo() + " e " + trecho.getTipoTerreno()
                    + ". Remova e cadastre novamente se o terreno mudou.");
        }
        String codigo = request.codigo().trim();
        if (repository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ConflitoException("Ja existe outro trecho com o codigo " + codigo + ".");
        }

        trecho.atualizarDados(codigo, request.km(), request.alturaVegetacao(), diasSemRocada(request));
        if (trecho instanceof TrechoUmido umido) {
            umido.trocarSensor(request.idSensor().trim());
        }
        return paraResponse(repository.save(trecho));
    }

    @Transactional
    public void remover(Long id) {
        TrechoRodovia trecho = buscarEntidade(id);

        // Checa as FKs antes, para devolver uma mensagem clara (409) em vez de ORA-02292.
        long intervencoes = intervencaoRepository.countByTrechoId(id);
        if (intervencoes > 0) {
            throw new ConflitoException("O trecho " + trecho.getCodigo() + " possui " + intervencoes
                    + " intervencao(oes) registrada(s) e nao pode ser removido.");
        }
        if (equipeRepository.existsByTrechoResponsavelId(id)) {
            throw new ConflitoException("O trecho " + trecho.getCodigo()
                    + " esta associado a uma equipe. Desassocie a equipe antes de remover.");
        }
        repository.delete(trecho);
    }

    // Sprint 2 via REST: simula N dias de crescimento. POLIMORFISMO: o mesmo
    // metodo, mas cada terreno cresce na sua propria taxa (umido 2.5 cm/dia, seco 0.8).
    @Transactional
    public TrechoResponse simularCrescimento(Long id, int dias) {
        if (dias <= 0) {
            throw new RegraNegocioException("A quantidade de dias deve ser maior que zero.");
        }
        TrechoRodovia trecho = buscarEntidade(id);
        trecho.simularCrescimento(dias);
        return paraResponse(repository.save(trecho));
    }

    // ----- regras de validacao herdadas da Sprint 1 -----

    private void validar(TrechoRequest request) {
        if (request.codigo() == null || request.codigo().isBlank()) {
            throw new RegraNegocioException("O codigo do trecho e obrigatorio.");
        }
        if (request.km() == null || request.km() < 0) {
            throw new RegraNegocioException("O KM do trecho nao pode ser negativo.");
        }
        if (request.alturaVegetacao() == null || request.alturaVegetacao() < 0) {
            throw new RegraNegocioException("A altura da vegetacao nao pode ser negativa.");
        }
        if (request.diasSemRocada() != null && request.diasSemRocada() < 0) {
            throw new RegraNegocioException("Os dias sem rocada nao podem ser negativos.");
        }
        if (request.tipoTerreno() == null) {
            throw new RegraNegocioException("O tipo de terreno e obrigatorio (SECO ou UMIDO).");
        }
        boolean temSensor = request.idSensor() != null && !request.idSensor().isBlank();
        if (request.tipoTerreno() == TipoTerreno.UMIDO && !temSensor) {
            throw new RegraNegocioException("Um trecho UMIDO precisa de um sensor IoT instalado (idSensor).");
        }
        if (request.tipoTerreno() == TipoTerreno.SECO && temSensor) {
            throw new RegraNegocioException("Trechos SECOS nao possuem sensor IoT: remova o idSensor.");
        }
    }

    private static int diasSemRocada(TrechoRequest request) {
        return request.diasSemRocada() == null ? 0 : request.diasSemRocada();
    }

    private static List<TrechoRodovia> ordenarPorUrgencia(List<TrechoRodovia> trechos) {
        return trechos.stream().sorted(MAIS_URGENTE_PRIMEIRO).toList();
    }

    private TrechoRodovia buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Trecho " + id + " nao encontrado."));
    }

    private TrechoResponse paraResponse(TrechoRodovia trecho) {
        return TrechoResponse.de(trecho, motor.classificar(trecho));
    }
}
