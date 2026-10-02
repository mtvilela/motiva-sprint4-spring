# MOTIVA · Sprint 4 · API REST com Spring Boot + JPA

Sistema de **monitoramento e priorização de roçada de vegetação nas rodovias** da Motiva.
Nesta Sprint, o protótipo de console com JDBC puro da Sprint 3 virou uma **API REST** em
**Spring Boot 4.1 + Spring Data JPA**, usando o **mesmo banco Oracle e as mesmas tabelas `T_MTV_*`**
que modelamos na Sprint 3.

```java
// Sprint 3 (JDBC puro): TrechoRodoviaDAO.inserir()
try (PreparedStatement stmt = conexao().prepareStatement(SQL_INSERIR, COLUNA_CHAVE)) {
    preencherParametros(stmt, trecho);          // 6 parametros, um a um
    stmt.executeUpdate();
    try (ResultSet chaves = stmt.getGeneratedKeys()) {
        if (chaves.next()) { trecho.setId(chaves.getInt(1)); }
    }
}

// Sprint 4 (Spring Data JPA): TrechoService.criar()
return paraResponse(repository.save(trecho));
```

---

## Sumário

1. [Tecnologias](#tecnologias)
2. [Como executar](#como-executar)
3. [Arquitetura em camadas](#arquitetura-em-camadas)
4. [Da Sprint 3 para a Sprint 4](#da-sprint-3-para-a-sprint-4)
5. [Mapeamento JPA das tabelas da Sprint 3](#mapeamento-jpa-das-tabelas-da-sprint-3)
6. [Endpoints](#endpoints)
7. [Exemplos cURL](#exemplos-curl)
8. [Códigos HTTP e formato de erro](#códigos-http-e-formato-de-erro)
9. [Regras de negócio (camada Service)](#regras-de-negócio-camada-service)
10. [Queries Spring Data](#queries-spring-data)
11. [Testes](#testes)
12. [Evidências: requisições funcionando](#evidências-requisições-funcionando)
13. [Git e boas práticas](#git-e-boas-práticas)
14. [Perguntas de reflexão](#perguntas-de-reflexão)
15. [Checklist da entrega](#checklist-da-entrega)
16. [Problemas comuns](#problemas-comuns)

---

## Tecnologias

| Item | Versão / detalhe |
|---|---|
| Java | **17** (`<java.version>17</java.version>`, compila com `--release 17`; executado e testado com o JDK 26) |
| Framework | **Spring Boot 4.1.1** (Spring Framework 7, Hibernate ORM 7.4, Jackson 3) |
| Build | **Maven** via Maven Wrapper (`./mvnw`): não precisa ter o Maven instalado |
| Starters | `spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation` |
| Banco | **Oracle 19c da FIAP** (`oracle.fiap.com.br:1521:ORCL`), tabelas da Sprint 3 |
| Driver | `com.oracle.database.jdbc:ojdbc17` como dependência Maven (sem JAR manual) |
| Testes | JUnit 5, MockMvc (`@SpringBootTest` + `@AutoConfigureMockMvc`), `@DataJpaTest`, H2 em memória |

> **Sobre o starter web:** no Spring Boot 4 o antigo `spring-boot-starter-web` foi renomeado para
> `spring-boot-starter-webmvc` (o nome antigo ainda existe, mas está *deprecated*). Usamos o nome novo,
> que é o que o Spring Initializr gera para o Boot 4.1. O conteúdo é o mesmo: Spring MVC + Tomcat + JSON.

---

## Como executar

### 1. Pré-requisitos

- **JDK 17 ou superior** (`java -version`).
- Acesso de rede ao Oracle da FIAP: `oracle.fiap.com.br:1521`.
- Maven **não** é necessário: o projeto traz o Maven Wrapper (`mvnw` / `mvnw.cmd`).
  Quem tem Maven instalado pode trocar `./mvnw` por `mvn` em todos os comandos.

### 2. Preparar o banco (uma vez)

Os scripts estão em [sql/](sql/) e rodam no **SQL Developer** (abrir o arquivo e *Run Script*, F5).

| Situação | Script |
|---|---|
| **Já tenho as tabelas da Sprint 3** (com dados) | [`sql/03-migracao-sprint3.sql`](sql/03-migracao-sprint3.sql): troca as PKs `IDENTITY` por sequences nomeadas **sem apagar nenhum dado** (idempotente) |
| Instalação do zero | [`sql/01-criacao.sql`](sql/01-criacao.sql) e depois [`sql/02-dados.sql`](sql/02-dados.sql) |

> Por que a migração é necessária? Explicado em [IDENTITY → SEQUENCE](#identity--sequence-por-que-mudamos-as-pks).
> No schema `RM564989` ela já foi executada.

### 3. Configurar as credenciais (fora do Git)

O [`application.properties`](src/main/resources/application.properties) **não tem senha**: ele lê
`DB_USERNAME` e `DB_PASSWORD` de variáveis de ambiente ou de um arquivo `.env` na raiz do projeto.
O `.env` está no `.gitignore`.

```bash
cp .env.example .env      # depois edite o .env com o seu RM e senha
```

```properties
# .env
DB_USERNAME=RMxxxxxx
DB_PASSWORD=sua_senha
```

Alternativa sem arquivo: `export DB_USERNAME=RMxxxxxx DB_PASSWORD=...` (Windows PowerShell:
`$env:DB_USERNAME="RMxxxxxx"`).

### 4. Subir a API

```bash
./mvnw spring-boot:run          # macOS / Linux
mvnw.cmd spring-boot:run        # Windows
```

A API sobe em **http://localhost:8080**. Na subida, o Hibernate roda em modo
`ddl-auto=validate`: confere se cada `@Entity` bate com as tabelas e sequences do Oracle e
**não altera nada**. Se faltar a migração, a aplicação já avisa na largada (ver
[Problemas comuns](#problemas-comuns)).

Teste rápido:

```bash
curl http://localhost:8080/api/trechos
```

### 5. Rodar os testes e a demonstração

```bash
./mvnw test                 # 56 testes automatizados (H2 em memória, não precisam do Oracle)
./docs/demo-api.sh          # com a API rodando: roteiro de 27 requisições (CRUD + relatório)
```

O `demo-api.sh` é um script bash: no Windows, rode pelo Git Bash.

Também dá para importar no Postman a coleção [`docs/motiva-api.postman_collection.json`](docs/motiva-api.postman_collection.json)
(35 requisições organizadas em pastas, que guardam os IDs criados em variáveis).

---

## Arquitetura em camadas

```
 Cliente (cURL / Postman)
        │  HTTP + JSON
        ▼
 ┌──────────────┐   DTOs (records) + @Valid
 │  controller  │   só HTTP: rota, status code, Location. Zero regra de negócio.
 └──────┬───────┘
        ▼
 ┌──────────────┐   @Service + @Transactional
 │   service    │   motor de prioridade, validações da Sprint 1, regras de exclusão...
 └──────┬───────┘
        ▼
 ┌──────────────┐   interfaces JpaRepository (zero SQL)
 │  repository  │   implementação gerada pelo Spring Data em tempo de execução
 └──────┬───────┘
        ▼
   Oracle FIAP  (tabelas T_MTV_* + sequences SQ_MTV_*)
```

Cada camada só conversa com a vizinha: nenhum controller injeta repository, e nenhum service
devolve entidade para o controller (sempre DTO).

```
src/main/java/br/com/motiva/
├── MotivaApplication.java            # @SpringBootApplication (main)
├── model/                            # @Entity + domínio das Sprints 1 e 2
│   ├── TrechoRodovia.java            # abstrata, herança SINGLE_TABLE
│   ├── TrechoSeco.java / TrechoUmido.java (implements MonitoravelViaIoT)
│   ├── EquipeManutencao.java         # @ManyToOne -> trecho
│   ├── RegistroIntervencao.java      # @ManyToOne -> trecho e equipe
│   ├── RelatorioHistorico.java       # coluna virtual TOTAL_TRECHOS (@Generated)
│   ├── IntervencaoOperacional.java   # abstrata (comportamento, não é tabela)
│   ├── RocadaMecanizada / RocadaManual / Pulverizacao
│   └── Prioridade, TipoTerreno, TipoIntervencao, RelatorioPrioridade, ItemRelatorio
├── repository/                       # interfaces JpaRepository com derived queries
├── service/                          # MotorDeRegras, GeradorRelatorio, TrechoService, EquipeService, IntervencaoService
├── controller/                       # TrechoController, EquipeController, IntervencaoController, RelatorioController
├── dto/                              # contratos da API (request/response), separados das entidades
└── exception/                        # exceções de negócio + GlobalExceptionHandler (@RestControllerAdvice)
```

---

## Da Sprint 3 para a Sprint 4

| Sprint 3 (JDBC puro) | Sprint 4 (Spring Boot) | O que mudou |
|---|---|---|
| `model/` + records internos dos DAOs | `@Entity` em `model/` | Classes mapeadas para as tabelas via anotações |
| `TrechoRodoviaDAO` (165 linhas) | `TrechoRodoviaRepository` (interface) | `switch` em `TIPO_TERRENO` virou `@DiscriminatorColumn` |
| `EquipeManutencaoDAO` (124 linhas) | `EquipeManutencaoRepository` | `LEFT JOIN` escrito à mão virou `@ManyToOne` + `@EntityGraph` |
| `IntervencaoOperacionalDAO` (155 linhas) | `RegistroIntervencaoRepository` | IDs soltos (`idTrecho`) viraram associações com os objetos |
| `RelatorioPrioridadeDAO` (143 linhas) | `RelatorioHistoricoRepository` | `salvarRelatorio(...)` virou `save()` |
| `db/ConexaoBD` (Singleton, 97 linhas) | `application.properties` + pool HikariCP | conexões abertas, reaproveitadas e fechadas pelo Spring |
| `service/MotorDeRegras` | `@Service MotorDeRegras` | mesma lógica, agora injetada onde precisa |
| `service/GeradorRelatorio` | `@Service GeradorRelatorio` | imprime nada: persiste e devolve JSON |
| `main.Main` com `System.out.println` | `@RestController` + JSON | a interface do sistema agora é HTTP |
| `test.Testes` (Java puro) | JUnit 5 + MockMvc | 56 testes, incluindo os cenários da Sprint 3 |
| teste manual no console | cURL / Postman / `docs/demo-api.sh` | qualquer cliente HTTP consome a API |

São **684 linhas** de DAOs + conexão substituídas por **4 interfaces (127 linhas, quase tudo
comentário)**, e nenhuma linha de SQL no código Java.

---

## Mapeamento JPA das tabelas da Sprint 3

| Tabela (Sprint 3) | Entidade | PK / sequence | Destaques |
|---|---|---|---|
| `T_MTV_TRECHO_RODOVIA` | `TrechoRodovia` (abstrata) → `TrechoSeco`, `TrechoUmido` | `ID_TRECHO` / `SQ_MTV_TRECHO` | Herança `SINGLE_TABLE`, discriminador `TIPO_TERRENO` |
| `T_MTV_EQUIPE_MANUTENCAO` | `EquipeManutencao` | `ID_EQUIPE` / `SQ_MTV_EQUIPE` | `@ManyToOne(fetch = LAZY)` para o trecho crítico |
| `T_MTV_INTERVENCAO` | `RegistroIntervencao` | `ID_INTERVENCAO` / `SQ_MTV_INTERVENCAO` | `@Enumerated(STRING)` no `TIPO`, `@ManyToOne` trecho/equipe |
| `T_MTV_RELATORIO_PRIORIDADE` | `RelatorioHistorico` | `ID_RELATORIO` / `SQ_MTV_RELATORIO` | Coluna virtual `TOTAL_TRECHOS` lida com `@Generated` |

```java
@Entity
@Table(name = "T_MTV_TRECHO_RODOVIA")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "TIPO_TERRENO", discriminatorType = DiscriminatorType.STRING, length = 10)
public abstract class TrechoRodovia {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seqTrecho")
    @SequenceGenerator(name = "seqTrecho", sequenceName = "SQ_MTV_TRECHO", allocationSize = 1)
    @Column(name = "ID_TRECHO")
    private Long id;
    // ...
    public abstract double taxaCrescimentoDiaria();   // polimorfismo da Sprint 2 intacto
}

@Entity @DiscriminatorValue("UMIDO")
public class TrechoUmido extends TrechoRodovia implements MonitoravelViaIoT { /* ... */ }
```

**Herança `SINGLE_TABLE`.** Na Sprint 3 já guardávamos `TrechoSeco` e `TrechoUmido` na mesma
tabela, com `TIPO_TERRENO` dizendo qual subclasse recriar (o `switch` do `TrechoRodoviaDAO.mapear()`).
Isso é exatamente a estratégia `SINGLE_TABLE` do JPA: o Hibernate lê o discriminador e devolve a
subclasse certa, então `trechoRepository.findAll()` já entrega objetos polimórficos para o motor.

**`allocationSize = 1`.** As sequences são `INCREMENT BY 1` e também são usadas pelos scripts SQL
(`DEFAULT SQ_MTV_TRECHO.NEXTVAL`). Com o padrão do JPA (50), o Hibernate reservaria blocos de 50 IDs
e entraria em conflito com os INSERTs manuais; e o `validate` acusaria a diferença de incremento.

**Coluna virtual.** `TOTAL_TRECHOS` é calculada pelo Oracle (`GENERATED ALWAYS AS ... VIRTUAL`). A
entidade nunca grava nela (`insertable = false, updatable = false`) e o `@Generated` do Hibernate faz
o valor calculado voltar para o objeto logo depois do INSERT.

### IDENTITY → SEQUENCE: por que mudamos as PKs

Na Sprint 3 as chaves eram `GENERATED ALWAYS AS IDENTITY`. No Oracle, uma coluna IDENTITY é
implementada com uma **sequence oculta com nome gerado pelo banco**; no nosso schema eram
`ISEQ$$_4205327`, `ISEQ$$_4205331`, `ISEQ$$_4205335` e `ISEQ$$_4205340`. A Sprint 4 pede
`@SequenceGenerator` ligado às sequences do projeto, e isso não funciona com IDENTITY:

1. o nome `ISEQ$$_...` muda toda vez que a tabela é recriada, então não dá para colocar no código;
2. o `ALWAYS` **proíbe** gravar um valor no ID, mas o Hibernate busca o próximo número na sequence e
   manda esse valor no INSERT (`ORA-32795`).

O script [`03-migracao-sprint3.sql`](sql/03-migracao-sprint3.sql) resolve isso **sem perder dados**.
Para cada tabela ele remove o IDENTITY, cria a sequence nomeada começando no maior ID existente + 1
e define `DEFAULT <sequence>.NEXTVAL` na coluna, para que INSERTs manuais (e o próprio JDBC da
Sprint 3) continuem funcionando. Tabelas, colunas, constraints e dados continuam os mesmos.

---

## Endpoints

Base: `http://localhost:8080`. Todos os corpos são JSON (`Content-Type: application/json`).

### Trechos: `/api/trechos`

| Método | Rota | Ação | Sucesso | Erros |
|---|---|---|---|---|
| GET | `/api/trechos` | Listar todos (por KM). Filtros opcionais e combináveis: `?alturaMinima=40`, `?tipoTerreno=UMIDO` | 200 | 400 |
| GET | `/api/trechos/{id}` | Buscar por ID | 200 | 404 |
| POST | `/api/trechos` | Criar | **201** + `Location` + corpo | 400, 409 |
| PUT | `/api/trechos/{id}` | Atualizar | 200 | 400, 404, 409 |
| DELETE | `/api/trechos/{id}` | Remover | **204** | 404, 409 |
| GET | `/api/trechos/criticos` | Trechos com prioridade CRÍTICA, mais urgentes primeiro | 200 | |
| GET | `/api/trechos/codigo/{codigo}` | Buscar pelo código de negócio (ex.: `BR101-A`) | 200 | 404 |
| GET | `/api/trechos/{id}/intervencoes` | Histórico de intervenções do trecho | 200 | 404 |
| POST | `/api/trechos/{id}/simular-crescimento?dias=10` | Simula N dias de crescimento (polimorfismo) | 200 | 400, 404 |

Corpo de POST/PUT:

| Campo | Tipo | Regras |
|---|---|---|
| `codigo` | texto | obrigatório, até 20 caracteres, único |
| `km` | número | obrigatório, ≥ 0 |
| `alturaVegetacao` | número (cm) | obrigatório, ≥ 0 |
| `diasSemRocada` | inteiro | opcional (padrão 0), ≥ 0 |
| `tipoTerreno` | `SECO` \| `UMIDO` | obrigatório; não muda no PUT (é a subclasse do objeto) |
| `idSensor` | texto | **obrigatório para `UMIDO`** e proibido para `SECO` |

A resposta traz também o que o domínio calcula: `taxaCrescimentoDiaria` (2.5 cm/dia úmido,
0.8 cm/dia seco), `monitoradoViaIoT` e a `prioridade` atribuída pelo `MotorDeRegras`.

### Equipes: `/api/equipes`

| Método | Rota | Ação | Sucesso | Erros |
|---|---|---|---|---|
| GET | `/api/equipes` | Listar (filtro opcional `?nome=norte`) | 200 | |
| GET | `/api/equipes/{id}` | Buscar por ID | 200 | 404 |
| POST | `/api/equipes` | Criar | **201** | 400, 404 (trecho), 409 |
| PUT | `/api/equipes/{id}` | Atualizar (renomear, associar ou desassociar trecho) | 200 | 400, 404, 409 |
| DELETE | `/api/equipes/{id}` | Remover | **204** | 404, 409 |
| GET | `/api/equipes/disponiveis` | Equipes sem trecho associado | 200 | |

Corpo: `{"nome": "Equipe Norte", "trechoId": 3}`. O `trechoId` é opcional (`null` = sem trecho).
Regra da Sprint 1: **só dá para associar a um trecho CRÍTICO** (≥ 40 cm).

### Intervenções: `/api/intervencoes`

| Método | Rota | Ação | Sucesso | Erros |
|---|---|---|---|---|
| GET | `/api/intervencoes` | Listar, mais recentes primeiro (filtro opcional `?tipo=MECANIZADA`) | 200 | 400 |
| GET | `/api/intervencoes/{id}` | Buscar por ID | 200 | 404 |
| POST | `/api/intervencoes` | **Executa** o serviço no trecho e registra | **201** | 400, 404 |
| PUT | `/api/intervencoes/{id}` | **Corrige** o registro histórico | 200 | 400, 404 |
| DELETE | `/api/intervencoes/{id}` | Remover o registro | **204** | 404 |

- **POST** `{"trechoId": 3, "equipeId": 1, "tipo": "MECANIZADA"}`: `tipo` é opcional. Sem ele,
  o **MotorDeRegras escolhe** a intervenção pela prioridade do trecho. O serviço é executado de
  verdade (`executarServico()`, polimórfico): a altura do trecho cai para a residual do tipo
  (5 / 8 / 10 cm) e o registro guarda altura antes/depois, data e descrição.
- **PUT** substitui o registro inteiro (`tipo`, `trechoId`, `equipeId`, `dataExecucao`,
  `alturaAntes`, `alturaDepois`, `descricao`). Serve para corrigir um lançamento e **não** executa o
  serviço de novo.

### Relatórios: `/api/relatorios`

| Método | Rota | Ação | Sucesso | Erros |
|---|---|---|---|---|
| POST | `/api/relatorios` | Gera o relatório a partir dos trechos atuais e **persiste no histórico** | **201** | |
| GET | `/api/relatorios` | Lista o histórico (mais recente primeiro) | 200 | |
| GET | `/api/relatorios/periodo?inicio=2026-09-01&fim=2026-09-30` | Históricos do período (datas `yyyy-MM-dd`, inclusivas) | 200 | 400 |
| GET | `/api/relatorios/{id}` | Buscar um relatório do histórico | 200 | 404 |

O POST devolve o cabeçalho salvo (contagens, total, resumo) **e** os `itens` detalhados, ordenados
do mais urgente para o menos urgente. O histórico guarda só o cabeçalho e o resumo, como na Sprint 3.

---

## Exemplos cURL

Exemplos baseados na execução registrada em [`docs/evidencias-requisicoes.md`](docs/evidencias-requisicoes.md),
onde estão as saídas completas e sem edição. Aqui o código do trecho e os nomes foram simplificados
(lá aparecem como `DEMO-114435`).

**Criar trecho (201 Created):**

```bash
curl -i -X POST http://localhost:8080/api/trechos \
  -H "Content-Type: application/json" \
  -d '{"codigo":"BR101-T","km":120.5,"alturaVegetacao":20.0,"diasSemRocada":5,"tipoTerreno":"UMIDO","idSensor":"IOT-999"}'
```

```json
HTTP/1.1 201
Location: http://localhost:8080/api/trechos/9

{
  "id": 9, "codigo": "BR101-T", "km": 120.5, "alturaVegetacao": 20.0, "diasSemRocada": 5,
  "tipoTerreno": "UMIDO", "idSensor": "IOT-999", "monitoradoViaIoT": true,
  "taxaCrescimentoDiaria": 2.5, "prioridade": "MEDIA"
}
```

**Buscar, atualizar, remover:**

```bash
curl http://localhost:8080/api/trechos/9                                   # 200 (ou 404)
curl -X PUT http://localhost:8080/api/trechos/9 -H "Content-Type: application/json" \
  -d '{"codigo":"BR101-T","km":120.5,"alturaVegetacao":22.0,"diasSemRocada":8,"tipoTerreno":"UMIDO","idSensor":"IOT-999-B"}'   # 200
curl -X POST "http://localhost:8080/api/trechos/9/simular-crescimento?dias=10"   # 200: 22 + 2.5 x 10 = 47 cm (CRITICA)
curl -i -X DELETE http://localhost:8080/api/trechos/9                      # 204 (409 se tiver intervenções)
```

**Filtros (derived queries):**

```bash
curl "http://localhost:8080/api/trechos?alturaMinima=40"                   # findByAlturaVegetacaoGreaterThanEqual
curl "http://localhost:8080/api/trechos?tipoTerreno=UMIDO"                 # findByTipoTerrenoOrderByKmAsc
curl "http://localhost:8080/api/intervencoes?tipo=MECANIZADA"              # findByTipo
curl "http://localhost:8080/api/equipes?nome=norte"                        # findByNomeContainingIgnoreCase...
```

**Equipe (regra da Sprint 1):**

```bash
curl -X POST http://localhost:8080/api/equipes -H "Content-Type: application/json" \
  -d '{"nome":"Equipe Oeste","trechoId":4}'
```

```json
{
  "status": 400, "erro": "Bad Request",
  "mensagem": "A equipe so pode ser associada a um trecho CRITICO (>= 40 cm). O trecho BR101-D esta com 9.0 cm (BAIXA).",
  "caminho": "/api/equipes"
}
```

**Intervenção escolhida pelo motor de regras (201):**

```bash
curl -X POST http://localhost:8080/api/intervencoes -H "Content-Type: application/json" \
  -d '{"trechoId":9,"equipeId":4}'
```

```json
{
  "id": 5, "tipo": "MECANIZADA", "servico": "Rocada Mecanizada",
  "trecho": { "id": 9, "codigo": "BR101-T", "km": 120.5, "alturaVegetacao": 5.0 },
  "equipe": { "id": 4, "nome": "Equipe Oeste" },
  "dataExecucao": "2026-10-02T11:44:36", "alturaAntes": 47.0, "alturaDepois": 5.0,
  "descricao": "[Rocada Mecanizada] KM 120.5: trator cortou de 47.0 cm para 5.0 cm."
}
```

**Corrigir o registro (PUT):**

```bash
curl -X PUT http://localhost:8080/api/intervencoes/5 -H "Content-Type: application/json" \
  -d '{"tipo":"MECANIZADA","trechoId":9,"equipeId":4,"dataExecucao":"2026-10-02T08:00:00","alturaAntes":47.0,"alturaDepois":5.0,"descricao":"Conferido em campo."}'
```

**Relatório de prioridade:**

```bash
curl -i -X POST http://localhost:8080/api/relatorios                       # 201: gera e grava no histórico
curl http://localhost:8080/api/relatorios                                  # 200: histórico
curl "http://localhost:8080/api/relatorios/periodo?inicio=2026-08-01&fim=2026-10-31"   # 200: por período
```

```json
{
  "id": 3, "dataGeracao": "2026-10-02T11:44:37",
  "qtCritica": 2, "qtAlta": 2, "qtMedia": 2, "qtBaixa": 1, "totalTrechos": 7, "qtComIntervencao": 6,
  "resumo": "CRITICA BR101-C KM 40.0 44.6cm -> Rocada Mecanizada | CRITICA BR101-G KM 96.5 52.6cm -> ...",
  "itens": [
    { "prioridade": "CRITICA", "trechoId": 3, "codigo": "BR101-C", "km": 40.0, "alturaVegetacao": 44.58,
      "tipoTerreno": "UMIDO", "fonte": "IoT", "tipoIntervencao": "MECANIZADA",
      "intervencaoRecomendada": "Rocada Mecanizada" }
  ]
}
```

---

## Códigos HTTP e formato de erro

| Código | Quando |
|---|---|
| **200 OK** | GET/PUT bem-sucedidos |
| **201 Created** | POST que cria recurso: corpo criado + header `Location` com a URL nova |
| **204 No Content** | DELETE bem-sucedido |
| **400 Bad Request** | Bean Validation (`@Valid`), regra de negócio do Service, JSON mal formado, parâmetro com tipo/enum inválido ou ausente |
| **404 Not Found** | ID inexistente (no caminho ou referenciado no corpo) ou rota inexistente |
| **405 Method Not Allowed** | Método HTTP não suportado na rota (com header `Allow`) |
| **409 Conflict** | Código/nome duplicado (UNIQUE) ou exclusão de registro que ainda tem dependentes (FK) |
| **500** | Erro inesperado (registrado no log) |

Todos os erros saem no mesmo formato, montado pelo `GlobalExceptionHandler` (`@RestControllerAdvice`).
Nenhum controller tem `try/catch`.

```json
{
  "timestamp": "2026-10-02T11:44:36",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados invalidos: corrija os campos indicados.",
  "caminho": "/api/trechos",
  "campos": {
    "codigo": "e obrigatorio",
    "km": "nao pode ser negativo",
    "alturaVegetacao": "nao pode ser negativa"
  }
}
```

Enums inválidos listam os valores aceitos, por exemplo
`"Valor invalido para o campo 'tipoTerreno' (valores aceitos: [SECO, UMIDO])."`.

---

## Regras de negócio (camada Service)

### Motor de prioridade: `MotorDeRegras` (`@Service`)

Mantivemos os limiares da nossa Sprint 2 (o enunciado permite), que equivalem a
urgente / crítico / atenção / normal:

| Prioridade | Altura da vegetação | Intervenção recomendada |
|---|---|---|
| `CRITICA` | ≥ 40 cm | Roçada Mecanizada |
| `ALTA` | 25 a 40 cm | Pulverização se o trecho tem rebrote rápido (≥ 2 cm/dia, úmido); senão Roçada Mecanizada |
| `MEDIA` | 15 a 25 cm | Roçada Manual |
| `BAIXA` | < 15 cm | Nenhuma (monitorar) |

Os limiares ficam no enum `Prioridade`. Quem aplica a classificação é o `MotorDeRegras.classificar()`,
e nenhum controller conhece esses números. O `GeradorRelatorio` busca os trechos atuais, roda o motor
(os trechos úmidos leem o sensor IoT durante a varredura), ordena por urgência e salva o histórico.

### Validações e regras por service

| Service | Regras |
|---|---|
| `TrechoService` | código obrigatório e único; **KM ≥ 0; altura da vegetação ≥ 0**; dias ≥ 0; `UMIDO` exige sensor e `SECO` não tem sensor; tipo de terreno não muda no PUT; não remove trecho com intervenções ou equipe associada; simulação exige dias > 0 |
| `EquipeService` | nome obrigatório e único (sem diferenciar maiúsculas); **só associa a trecho CRÍTICO** (Sprint 1), mas manter o trecho atual é permitido mesmo que ele tenha sido roçado depois; não remove equipe com intervenções |
| `IntervencaoService` | sem `tipo`, o motor escolhe, e trecho `BAIXA` sem tipo é recusado (o motor não recomenda nada); execução polimórfica do serviço; na correção, data não pode ser futura e a altura depois não pode ser maior que a altura antes |
| `GeradorRelatorio` | relatório sobre os trechos atuais do banco; período com `inicio` ≤ `fim` |

Dois detalhes de JPA que aparecem nas regras:

- **Dirty checking.** Quando uma intervenção é executada, o `IntervencaoService` **não** chama
  `trechoRepository.save(trecho)`. O trecho é uma entidade gerenciada dentro do `@Transactional`, e o
  Hibernate grava a nova altura sozinho no commit. O mesmo vale para as leituras IoT do relatório.
  Na Sprint 3, o `Main` precisava chamar `daoTrecho.atualizar(trecho)` na mão.
- **Uma intervenção nunca aumenta a vegetação.** Uma pulverização (residual de 10 cm) num trecho com
  6 cm deixa o trecho em 6 cm. Na Sprint 3 ele "subia" para 10 cm.

---

## Queries Spring Data

Nenhum repository tem SQL. O Spring Data monta cada consulta a partir do **nome do método**:

| Repository | Derived queries |
|---|---|
| `TrechoRodoviaRepository` | `findByAlturaVegetacaoGreaterThanEqual(Double minimo)`, `findByTipoTerrenoOrderByKmAsc(TipoTerreno)`, `findByTipoTerrenoAndAlturaVegetacaoGreaterThanEqual(...)`, `findAllByOrderByKmAsc()`, `findByCodigo(String)`, `existsByCodigo(...)`, `existsByCodigoAndIdNot(...)` |
| `EquipeManutencaoRepository` | `findAllByOrderByNomeAsc()`, `findByNomeContainingIgnoreCaseOrderByNomeAsc(String)`, `findByTrechoResponsavelIsNullOrderByNomeAsc()`, `existsByNomeIgnoreCase(...)`, `existsByTrechoResponsavelId(Long)` |
| `RegistroIntervencaoRepository` | **`findByTipo(TipoIntervencao tipo)`**, `findAllByOrderByDataExecucaoDesc()`, `findByTrechoIdOrderByDataExecucaoDesc(Long)`, `countByTrechoId(Long)`, `countByEquipeId(Long)` |
| `RelatorioHistoricoRepository` | `findAllByOrderByDataGeracaoDesc()`, `findByDataGeracaoBetweenOrderByDataGeracaoDesc(LocalDateTime, LocalDateTime)` |

SQL que o Hibernate gerou para `findByAlturaVegetacaoGreaterThanEqual(40.0)`, copiado do log da
aplicação (`logging.level.org.hibernate.SQL=DEBUG` no `application.properties`):

```sql
select tr1_0.id_trecho, tr1_0.tipo_terreno, tr1_0.altura_vegetacao, tr1_0.codigo,
       tr1_0.dias_sem_rocada, tr1_0.km, tr1_0.tipo_terreno, tr1_0.id_sensor
  from t_mtv_trecho_rodovia tr1_0
 where tr1_0.altura_vegetacao>=?
```

e o ID de um trecho novo, antes do INSERT: `select sq_mtv_trecho.nextval from dual`.

---

## Testes

```bash
./mvnw test
```

**56 testes, todos passando**, rodando num H2 em memória (perfil `test`): não precisam de rede nem
mexem nos dados do Oracle. O Hibernate cria o schema a partir das próprias `@Entity`, incluindo as
sequences e a coluna virtual.

| Classe | Tipo | O que cobre |
|---|---|---|
| `TrechoControllerTest` (13) | `@SpringBootTest` + MockMvc | 201 + `Location`, 200, 404, 400 (Bean Validation e regras), 409, PUT, DELETE 204/404, filtros, simulação polimórfica |
| `EquipeControllerTest` (8) | `@SpringBootTest` + MockMvc | regra do trecho crítico, nome duplicado, desassociar, equipes disponíveis, 409 com intervenções |
| `IntervencaoControllerTest` (7) | `@SpringBootTest` + MockMvc | motor escolhe o tipo, dirty checking no trecho, PUT de correção, `findByTipo`, FK no DELETE |
| `RelatorioControllerTest` (4) | `@SpringBootTest` + MockMvc | geração ordenada, persistência, coluna virtual, consulta por período, 400 de período |
| `RepositoryDerivedQueriesTest` (6) | `@DataJpaTest` | derived queries, sequence, leitura polimórfica, `@PrePersist`, `@Generated` |
| `MotorDeRegrasTest` (18) | JUnit puro | os cenários do `test.Testes` da Sprint 3: limiares, recomendação, crescimento, fábrica, mock IoT |

---

## Evidências: requisições funcionando

[`docs/evidencias-requisicoes.md`](docs/evidencias-requisicoes.md) é o registro de **27 requisições
reais contra o Oracle da FIAP**, geradas pelo roteiro [`docs/demo-api.sh`](docs/demo-api.sh), com o
comando cURL, o status HTTP e o JSON de cada uma:

| Parte | Requisições | Status |
|---|---|---|
| Trechos | listar, criar, buscar, atualizar, simular crescimento, filtro `alturaMinima`, 404, 400 (validação), 400 (regra) | 200 · 201 · 200 · 200 · 200 · 200 · 404 · 400 · 400 |
| Equipes | criar no trecho crítico, trecho não crítico, listar, atualizar | 201 · 400 · 200 · 200 |
| Intervenções | registrar (motor escolhe), trecho atualizado, `findByTipo`, buscar, corrigir (PUT), apagar trecho com FK | 201 · 200 · 200 · 200 · 200 · 409 |
| Limpeza | remover intervenção, equipe e trecho, buscar removido | 204 · 204 · 204 · 404 |
| Relatórios | gerar e salvar, histórico, por período, período inválido | 201 · 200 · 200 · 400 |

Para gerar de novo: suba a API e rode `./docs/demo-api.sh`. Os registros temporários são removidos
no final (como o `Main` da Sprint 3 fazia). O relatório gerado continua no histórico, que é o
comportamento esperado.

---

## Git e boas práticas

- **`.gitignore`** cobre `target/`, arquivos de IDE (`.idea/`, `*.iml`, `.vscode/`, `.settings/`...),
  `.DS_Store` e, principalmente, o **`.env`**.
- **Sem senha no repositório**: o `application.properties` usa `${DB_USERNAME}` e `${DB_PASSWORD}`
  e só existe o modelo [`.env.example`](.env.example) com placeholders. O código Java não tem nenhuma
  credencial (na Sprint 3 elas ficavam no `ConexaoBD.java`).
- **Commits incrementais por camada**: estrutura → scripts SQL → model → repositories → services/DTOs
  → controllers → testes → documentação (`git log --oneline`).
- **Sem JDBC manual**: nenhum `Connection`, `PreparedStatement`, `ResultSet` ou `System.out` em
  `src/main`. O SQL é todo gerado pelo Hibernate.

---

## Perguntas de reflexão

### 1. Por que o Repository é uma interface e não uma classe? Quem escreve a implementação e quando?

Porque a gente só precisa **declarar o que quer**, e o "como" é sempre igual: abrir conexão, montar
o SQL, preencher parâmetros, percorrer o `ResultSet` e montar o objeto. Era isso que os nossos quatro
DAOs repetiam em ~590 linhas. Na interface fica só o contrato
(`TrechoRodoviaRepository extends JpaRepository<TrechoRodovia, Long>` + métodos como
`findByCodigo`).

Quem escreve a implementação é o **Spring Data JPA**, **na subida da aplicação**. Para cada interface
que estende `JpaRepository`, ele cria um **proxy dinâmico** (por baixo, um `SimpleJpaRepository`
com `save`, `findById`, `findAll`, `deleteById`...), interpreta o nome de cada método extra
(`findByTipo`, `existsByCodigo`...) e registra o resultado como um bean do Spring. Esse bean é
injetado no construtor dos services. Como a análise dos nomes acontece na subida, um método com
nome errado (propriedade que não existe) **impede a aplicação de iniciar**, em vez de quebrar só
quando alguém chamar. Por ser interface, o service também fica desacoplado da tecnologia e é fácil
de substituir num teste.

### 2. O pattern DAO "morreu" na migração ou só mudou de forma?

**Só mudou de forma.** A responsabilidade do DAO continua existindo: isolar o acesso a dados atrás de
uma interface de objetos, para que o resto do sistema nunca veja SQL. O `TrechoService` chama
`repository.findById(id)` exatamente como o `Main` chamava `dao.buscarPorId(id)`, e os dois devolvem
`Optional`. Os métodos têm correspondência quase um para um:

| DAO (Sprint 3) | Repository (Sprint 4) |
|---|---|
| `inserir(trecho)` | `save(trecho)` |
| `buscarPorId(id)` | `findById(id)` |
| `listarTodas()` | `findAll()` / `findAllByOrderByKmAsc()` |
| `atualizar(trecho)` | `save(trecho)` ou dirty checking |
| `deletar(id)` | `delete(trecho)` / `deleteById(id)` |
| `buscarPorCodigo(codigo)` | `findByCodigo(codigo)` |
| `listarPorTrecho(idTrecho)` | `findByTrechoIdOrderByDataExecucaoDesc(idTrecho)` |

O que morreu foi o **código repetitivo que escrevíamos à mão**: as constantes `SQL_*`, o
`PreparedStatement`, o `mapear(ResultSet)`, o Singleton `ConexaoBD` (agora pool HikariCP) e o
`switch` no `TIPO_TERRENO` (agora `@DiscriminatorColumn`). O Repository é um DAO que o framework
gera para nós.

### 3. Por que a validação de `alturaVegetacao >= 0` (Sprint 1) deve ficar no Service e não no Controller?

Porque é **regra de negócio, não detalhe de HTTP**. O controller é só **uma** das portas de entrada.
A mesma regra precisa valer para qualquer caminho que chegue ao domínio: outro controller, um teste,
um job futuro que importe leituras de sensores, uma fila de mensagens. Se a regra ficasse no
controller, todos esses caminhos aceitariam altura negativa. No service ela fica num lugar só, roda
dentro da transação e é testável sem HTTP.

No nosso projeto existem três barreiras, cada uma com seu papel:

1. **DTO + Bean Validation** (`@PositiveOrZero` no `TrechoRequest`): valida o **contrato HTTP** e
   devolve 400 com o nome do campo antes de qualquer processamento;
2. **`TrechoService.validar()`**: a **regra de negócio** propriamente dita, que vale para qualquer
   chamador, junto com regras que o Bean Validation não expressa (por exemplo, `UMIDO` exige sensor);
3. **`CHECK (ALTURA_VEGETACAO >= 0)`** no Oracle: a última linha de defesa.

### 4. No JDBC vocês escreviam SQL. Onde está o SQL do `findByTipo()`? Quem o gerou?

**Não está em lugar nenhum do nosso código.** Na subida, o Spring Data lê o nome
`findByTipo(TipoIntervencao tipo)` e o divide em partes: `find` (consulta), `By` e a propriedade
`tipo` da entidade `RegistroIntervencao`. Com isso ele monta uma consulta JPQL equivalente a
`select r from RegistroIntervencao r where r.tipo = ?1`. Na execução, o **Hibernate** traduz essa
JPQL para o SQL do dialeto Oracle, usando as anotações da entidade: `@Table(name = "T_MTV_INTERVENCAO")`,
`@Column(name = "TIPO")`, e `@Enumerated(STRING)`, que transforma o enum em `'MECANIZADA'`. Quem
executa no banco é o driver `ojdbc17`: o JDBC continua existindo, mas quem usa é o Hibernate, não nós.

SQL real que o Hibernate gerou para `GET /api/intervencoes?tipo=MECANIZADA`, copiado do log. Os
`join` vêm do `@EntityGraph`, que traz trecho e equipe na mesma consulta:

```sql
select ri1_0.id_intervencao, ri1_0.altura_antes, ri1_0.altura_depois, ri1_0.data_execucao,
       ri1_0.descricao, e1_0.id_equipe, e1_0.nome, e1_0.id_trecho, ri1_0.tipo, ri1_0.id_trecho,
       t1_0.id_trecho, t1_0.tipo_terreno, t1_0.altura_vegetacao, t1_0.codigo,
       t1_0.dias_sem_rocada, t1_0.km, t1_0.tipo_terreno, t1_0.id_sensor
  from t_mtv_intervencao ri1_0
  left join t_mtv_equipe_manutencao e1_0 on e1_0.id_equipe = ri1_0.id_equipe
  join t_mtv_trecho_rodovia t1_0 on t1_0.id_trecho = ri1_0.id_trecho
 where ri1_0.tipo = ?
```

---

## Checklist da entrega

**Requisitos**

- [x] Projeto Spring Boot 4.1.1 + Maven rodando com `./mvnw spring-boot:run` (Java 17)
- [x] Camadas `model` / `repository` / `service` / `controller` em `br.com.motiva`
- [x] Entities com `@Entity`, `@Table`, `@Id`, `@GeneratedValue` + `@SequenceGenerator` ligado às sequences do projeto
- [x] Reaproveitamento das tabelas `T_MTV_*` da Sprint 3 no Oracle da FIAP (`ddl-auto=validate`)
- [x] Repositories `JpaRepository` com derived queries (`findByAlturaVegetacaoGreaterThanEqual`, `findByTipo`...)
- [x] Services com as validações da Sprint 1 e o motor de prioridade da Sprint 2
- [x] CRUD REST completo de Trecho, Equipe e Intervenção com 200/201/204/400/404 (+ 409)
- [x] Relatório: `POST /api/relatorios`, `GET /api/relatorios`, `GET /api/relatorios/periodo`, persistindo no histórico
- [x] Driver `ojdbc17` via Maven, credenciais só no ambiente (`.env` fora do Git)
- [x] Nenhum `Connection` / `PreparedStatement` / `ResultSet` / SQL manual / `System.out` como interface
- [x] README com execução, endpoints, cURL e perguntas de reflexão
- [x] Registro de 27 requisições funcionando ([`docs/evidencias-requisicoes.md`](docs/evidencias-requisicoes.md))
- [x] `.gitignore` correto e commits incrementais

**Bônus**

- [x] DTOs separando entidade de contrato de API (`dto/`, records)
- [x] Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`, `@PositiveOrZero`, `@PastOrPresent`...)
- [x] `@RestControllerAdvice` para tratamento global de erros (`GlobalExceptionHandler`)
- [x] Testes de API com `@SpringBootTest` + MockMvc (e `@DataJpaTest` nos repositories)

---

## Problemas comuns

| Erro | Causa / solução |
|---|---|
| `Could not resolve placeholder 'DB_USERNAME'` | Falta o `.env` (copie o `.env.example`) ou as variáveis de ambiente |
| `ORA-01017: invalid username/password` | Usuário/senha errados no `.env` |
| `Schema-validation: missing sequence [SQ_MTV_TRECHO]` | O schema ainda está como na Sprint 3: rode `sql/03-migracao-sprint3.sql` |
| `Schema-validation: missing table [T_MTV_...]` | Tabelas não existem nesse usuário: rode `sql/01-criacao.sql` e `sql/02-dados.sql` |
| `IO Error: The Network Adapter could not establish the connection` | Sem acesso a `oracle.fiap.com.br:1521` (rede/VPN) |
| `Port 8080 was already in use` | Outra aplicação na porta: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |
| Erro 409 ao apagar trecho ou equipe | Ele ainda tem intervenções (ou equipe associada): remova as dependências antes |
