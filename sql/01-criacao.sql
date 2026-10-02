-- =====================================================================
--  MOTIVA - Sistema de Priorizacao de Rocada | Sprint 4 (POO / FIAP)
--  Script de CRIACAO (instalacao do zero) - Oracle 19c
--
--  Sao as MESMAS 4 tabelas da Sprint 3 (mesmos nomes, colunas e constraints).
--  Unica mudanca: as chaves primarias deixaram de ser IDENTITY e passaram a
--  usar SEQUENCES NOMEADAS, que o JPA referencia com @SequenceGenerator:
--
--    SQ_MTV_TRECHO       -> T_MTV_TRECHO_RODOVIA.ID_TRECHO        (TrechoRodovia)
--    SQ_MTV_EQUIPE       -> T_MTV_EQUIPE_MANUTENCAO.ID_EQUIPE     (EquipeManutencao)
--    SQ_MTV_INTERVENCAO  -> T_MTV_INTERVENCAO.ID_INTERVENCAO      (RegistroIntervencao)
--    SQ_MTV_RELATORIO    -> T_MTV_RELATORIO_PRIORIDADE.ID_RELATORIO (RelatorioHistorico)
--
--  Cada ID tambem tem DEFAULT <sequence>.NEXTVAL, entao INSERTs manuais sem
--  ID (SQL Developer, script de dados) continuam funcionando.
--
--  Ja tem as tabelas da Sprint 3 com dados? Use 03-migracao-sprint3.sql
--  (converte IDENTITY -> SEQUENCE sem apagar nada) em vez deste script.
--
--  Execucao: SQL Developer -> abrir o arquivo -> Run Script (F5).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. Limpeza (idempotente): remove tabelas e sequences do projeto se ja
--    existirem, para o script poder ser executado quantas vezes for preciso.
-- ---------------------------------------------------------------------
BEGIN
    FOR t IN (SELECT table_name
                FROM user_tables
               WHERE table_name IN ('T_MTV_INTERVENCAO',
                                    'T_MTV_EQUIPE_MANUTENCAO',
                                    'T_MTV_RELATORIO_PRIORIDADE',
                                    'T_MTV_TRECHO_RODOVIA')) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
    END LOOP;
    FOR s IN (SELECT sequence_name
                FROM user_sequences
               WHERE sequence_name IN ('SQ_MTV_TRECHO',
                                       'SQ_MTV_EQUIPE',
                                       'SQ_MTV_INTERVENCAO',
                                       'SQ_MTV_RELATORIO')) LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
    END LOOP;
END;
/

-- ---------------------------------------------------------------------
-- 1. SEQUENCES (uma por tabela). INCREMENT BY 1 casa com o
--    allocationSize = 1 do @SequenceGenerator nas entidades.
-- ---------------------------------------------------------------------
CREATE SEQUENCE SQ_MTV_TRECHO      START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SQ_MTV_EQUIPE      START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SQ_MTV_INTERVENCAO START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SQ_MTV_RELATORIO   START WITH 1 INCREMENT BY 1 NOCACHE;

-- ---------------------------------------------------------------------
-- 2. TRECHO DE RODOVIA  (classe abstrata TrechoRodovia)
--    TIPO_TERRENO guarda qual subclasse o objeto e (SECO / UMIDO).
--    No JPA ela e a @DiscriminatorColumn da heranca SINGLE_TABLE.
--    ID_SENSOR so existe para trechos UMIDOS (implementam MonitoravelViaIoT).
-- ---------------------------------------------------------------------
CREATE TABLE T_MTV_TRECHO_RODOVIA (
    ID_TRECHO         NUMBER(10)     DEFAULT SQ_MTV_TRECHO.NEXTVAL NOT NULL,
    CODIGO            VARCHAR2(20)   NOT NULL,
    KM                NUMBER(8,2)    NOT NULL,
    ALTURA_VEGETACAO  NUMBER(6,2)    DEFAULT 0 NOT NULL,
    DIAS_SEM_ROCADA   NUMBER(5)      DEFAULT 0 NOT NULL,
    TIPO_TERRENO      VARCHAR2(10)   NOT NULL,
    ID_SENSOR         VARCHAR2(20),
    CONSTRAINT PK_MTV_TRECHO          PRIMARY KEY (ID_TRECHO),
    CONSTRAINT UK_MTV_TRECHO_CODIGO   UNIQUE (CODIGO),
    CONSTRAINT CK_MTV_TRECHO_KM       CHECK (KM >= 0),
    CONSTRAINT CK_MTV_TRECHO_ALTURA   CHECK (ALTURA_VEGETACAO >= 0),
    CONSTRAINT CK_MTV_TRECHO_DIAS     CHECK (DIAS_SEM_ROCADA >= 0),
    CONSTRAINT CK_MTV_TRECHO_TIPO     CHECK (TIPO_TERRENO IN ('SECO', 'UMIDO')),
    CONSTRAINT CK_MTV_TRECHO_SENSOR   CHECK (
           (TIPO_TERRENO = 'UMIDO' AND ID_SENSOR IS NOT NULL)
        OR (TIPO_TERRENO = 'SECO'  AND ID_SENSOR IS NULL))
);

COMMENT ON TABLE  T_MTV_TRECHO_RODOVIA                  IS 'Trechos de rodovia monitorados (classe abstrata TrechoRodovia)';
COMMENT ON COLUMN T_MTV_TRECHO_RODOVIA.ID_TRECHO        IS 'PK gerada pela sequence SQ_MTV_TRECHO';
COMMENT ON COLUMN T_MTV_TRECHO_RODOVIA.CODIGO           IS 'Identificador de negocio do trecho, ex.: BR101-A';
COMMENT ON COLUMN T_MTV_TRECHO_RODOVIA.KM               IS 'Marco quilometrico';
COMMENT ON COLUMN T_MTV_TRECHO_RODOVIA.ALTURA_VEGETACAO IS 'Altura atual da vegetacao em cm';
COMMENT ON COLUMN T_MTV_TRECHO_RODOVIA.DIAS_SEM_ROCADA  IS 'Dias desde a ultima intervencao';
COMMENT ON COLUMN T_MTV_TRECHO_RODOVIA.TIPO_TERRENO     IS 'Subclasse: SECO (TrechoSeco) ou UMIDO (TrechoUmido)';
COMMENT ON COLUMN T_MTV_TRECHO_RODOVIA.ID_SENSOR        IS 'Sensor IoT instalado (apenas trechos umidos)';

-- ---------------------------------------------------------------------
-- 3. EQUIPE DE MANUTENCAO  (classe EquipeManutencao, Sprint 1)
--    ID_TRECHO: trecho critico pelo qual a equipe e responsavel (opcional).
-- ---------------------------------------------------------------------
CREATE TABLE T_MTV_EQUIPE_MANUTENCAO (
    ID_EQUIPE   NUMBER(10)    DEFAULT SQ_MTV_EQUIPE.NEXTVAL NOT NULL,
    NOME        VARCHAR2(60)  NOT NULL,
    ID_TRECHO   NUMBER(10),
    CONSTRAINT PK_MTV_EQUIPE        PRIMARY KEY (ID_EQUIPE),
    CONSTRAINT UK_MTV_EQUIPE_NOME   UNIQUE (NOME),
    CONSTRAINT FK_MTV_EQUIPE_TRECHO FOREIGN KEY (ID_TRECHO)
        REFERENCES T_MTV_TRECHO_RODOVIA (ID_TRECHO)
);

COMMENT ON TABLE  T_MTV_EQUIPE_MANUTENCAO           IS 'Equipes de manutencao de rocada';
COMMENT ON COLUMN T_MTV_EQUIPE_MANUTENCAO.ID_EQUIPE IS 'PK gerada pela sequence SQ_MTV_EQUIPE';
COMMENT ON COLUMN T_MTV_EQUIPE_MANUTENCAO.ID_TRECHO IS 'Trecho critico associado a equipe (pode ser nulo)';

-- ---------------------------------------------------------------------
-- 4. INTERVENCAO OPERACIONAL  (registro de um servico executado)
--    TIPO guarda a subclasse de IntervencaoOperacional que foi executada:
--    MECANIZADA / MANUAL / PULVERIZACAO.
-- ---------------------------------------------------------------------
CREATE TABLE T_MTV_INTERVENCAO (
    ID_INTERVENCAO  NUMBER(10)     DEFAULT SQ_MTV_INTERVENCAO.NEXTVAL NOT NULL,
    TIPO            VARCHAR2(15)   NOT NULL,
    ID_TRECHO       NUMBER(10)     NOT NULL,
    ID_EQUIPE       NUMBER(10),
    DATA_EXECUCAO   TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL,
    ALTURA_ANTES    NUMBER(6,2)    NOT NULL,
    ALTURA_DEPOIS   NUMBER(6,2)    NOT NULL,
    DESCRICAO       VARCHAR2(200),
    CONSTRAINT PK_MTV_INTERVENCAO     PRIMARY KEY (ID_INTERVENCAO),
    CONSTRAINT FK_MTV_INTERV_TRECHO   FOREIGN KEY (ID_TRECHO)
        REFERENCES T_MTV_TRECHO_RODOVIA (ID_TRECHO),
    CONSTRAINT FK_MTV_INTERV_EQUIPE   FOREIGN KEY (ID_EQUIPE)
        REFERENCES T_MTV_EQUIPE_MANUTENCAO (ID_EQUIPE),
    CONSTRAINT CK_MTV_INTERV_TIPO     CHECK (TIPO IN ('MECANIZADA', 'MANUAL', 'PULVERIZACAO')),
    CONSTRAINT CK_MTV_INTERV_ALTURAS  CHECK (ALTURA_ANTES >= 0 AND ALTURA_DEPOIS >= 0)
);

CREATE INDEX IX_MTV_INTERV_TRECHO ON T_MTV_INTERVENCAO (ID_TRECHO);
CREATE INDEX IX_MTV_INTERV_EQUIPE ON T_MTV_INTERVENCAO (ID_EQUIPE);

COMMENT ON TABLE  T_MTV_INTERVENCAO                IS 'Historico de intervencoes operacionais executadas nos trechos';
COMMENT ON COLUMN T_MTV_INTERVENCAO.ID_INTERVENCAO IS 'PK gerada pela sequence SQ_MTV_INTERVENCAO';
COMMENT ON COLUMN T_MTV_INTERVENCAO.TIPO           IS 'Subclasse: MECANIZADA, MANUAL ou PULVERIZACAO';
COMMENT ON COLUMN T_MTV_INTERVENCAO.ALTURA_ANTES   IS 'Altura da vegetacao (cm) antes do servico';
COMMENT ON COLUMN T_MTV_INTERVENCAO.ALTURA_DEPOIS  IS 'Altura residual (cm) apos o servico';
COMMENT ON COLUMN T_MTV_INTERVENCAO.DESCRICAO      IS 'Texto devolvido por executarServico()';

-- ---------------------------------------------------------------------
-- 5. RELATORIO DE PRIORIDADE  (historico gerado pelo GeradorRelatorio)
--    TOTAL_TRECHOS e uma coluna VIRTUAL (calculada pelo proprio Oracle):
--    a entidade JPA so le esse valor, nunca grava.
-- ---------------------------------------------------------------------
CREATE TABLE T_MTV_RELATORIO_PRIORIDADE (
    ID_RELATORIO   NUMBER(10)      DEFAULT SQ_MTV_RELATORIO.NEXTVAL NOT NULL,
    DATA_GERACAO   TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
    QT_CRITICA     NUMBER(5)       DEFAULT 0 NOT NULL,
    QT_ALTA        NUMBER(5)       DEFAULT 0 NOT NULL,
    QT_MEDIA       NUMBER(5)       DEFAULT 0 NOT NULL,
    QT_BAIXA       NUMBER(5)       DEFAULT 0 NOT NULL,
    TOTAL_TRECHOS  NUMBER(6)       GENERATED ALWAYS AS (QT_CRITICA + QT_ALTA + QT_MEDIA + QT_BAIXA) VIRTUAL,
    RESUMO         VARCHAR2(2000),
    CONSTRAINT PK_MTV_RELATORIO     PRIMARY KEY (ID_RELATORIO),
    CONSTRAINT CK_MTV_RELATORIO_QT  CHECK (QT_CRITICA >= 0 AND QT_ALTA >= 0 AND QT_MEDIA >= 0 AND QT_BAIXA >= 0)
);

COMMENT ON TABLE  T_MTV_RELATORIO_PRIORIDADE               IS 'Historico dos relatorios de prioridade gerados pelo sistema';
COMMENT ON COLUMN T_MTV_RELATORIO_PRIORIDADE.ID_RELATORIO  IS 'PK gerada pela sequence SQ_MTV_RELATORIO';
COMMENT ON COLUMN T_MTV_RELATORIO_PRIORIDADE.QT_CRITICA    IS 'Trechos com prioridade CRITICA (>= 40 cm)';
COMMENT ON COLUMN T_MTV_RELATORIO_PRIORIDADE.QT_ALTA       IS 'Trechos com prioridade ALTA (25 a 40 cm)';
COMMENT ON COLUMN T_MTV_RELATORIO_PRIORIDADE.QT_MEDIA      IS 'Trechos com prioridade MEDIA (15 a 25 cm)';
COMMENT ON COLUMN T_MTV_RELATORIO_PRIORIDADE.QT_BAIXA      IS 'Trechos com prioridade BAIXA (< 15 cm)';
COMMENT ON COLUMN T_MTV_RELATORIO_PRIORIDADE.TOTAL_TRECHOS IS 'Coluna virtual: soma das contagens';
COMMENT ON COLUMN T_MTV_RELATORIO_PRIORIDADE.RESUMO        IS 'Resumo textual dos itens do relatorio';

-- ---------------------------------------------------------------------
-- 6. Conferencia
-- ---------------------------------------------------------------------
SELECT table_name
  FROM user_tables
 WHERE table_name LIKE 'T_MTV_%'
 ORDER BY table_name;

SELECT sequence_name, last_number, increment_by
  FROM user_sequences
 WHERE sequence_name LIKE 'SQ_MTV_%'
 ORDER BY sequence_name;
