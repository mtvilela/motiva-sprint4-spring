-- =====================================================================
--  MOTIVA - Sistema de Priorizacao de Rocada | Sprint 4 (POO / FIAP)
--  Script de DADOS DE TESTE (os mesmos da Sprint 3)
--
--  Os IDs vem do DEFAULT <sequence>.NEXTVAL de cada tabela, por isso as
--  chaves estrangeiras sao resolvidas por subconsulta (CODIGO / NOME).
--  Pre-requisito: 01-criacao.sql executado.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Trechos. Alturas escolhidas para cobrir todas as prioridades:
--      >= 40 CRITICA | 25-40 ALTA | 15-25 MEDIA | < 15 BAIXA
-- ---------------------------------------------------------------------
INSERT INTO T_MTV_TRECHO_RODOVIA (CODIGO, KM, ALTURA_VEGETACAO, DIAS_SEM_ROCADA, TIPO_TERRENO, ID_SENSOR)
VALUES ('BR101-A', 12.0, 18.0, 10, 'UMIDO', 'IOT-001');

INSERT INTO T_MTV_TRECHO_RODOVIA (CODIGO, KM, ALTURA_VEGETACAO, DIAS_SEM_ROCADA, TIPO_TERRENO, ID_SENSOR)
VALUES ('BR101-B', 27.5, 22.0, 36, 'SECO', NULL);

INSERT INTO T_MTV_TRECHO_RODOVIA (CODIGO, KM, ALTURA_VEGETACAO, DIAS_SEM_ROCADA, TIPO_TERRENO, ID_SENSOR)
VALUES ('BR101-C', 40.0, 45.0, 60, 'UMIDO', 'IOT-002');

INSERT INTO T_MTV_TRECHO_RODOVIA (CODIGO, KM, ALTURA_VEGETACAO, DIAS_SEM_ROCADA, TIPO_TERRENO, ID_SENSOR)
VALUES ('BR101-D', 58.0, 9.0, 19, 'SECO', NULL);

INSERT INTO T_MTV_TRECHO_RODOVIA (CODIGO, KM, ALTURA_VEGETACAO, DIAS_SEM_ROCADA, TIPO_TERRENO, ID_SENSOR)
VALUES ('BR101-E', 73.0, 30.0, 45, 'UMIDO', 'IOT-003');

INSERT INTO T_MTV_TRECHO_RODOVIA (CODIGO, KM, ALTURA_VEGETACAO, DIAS_SEM_ROCADA, TIPO_TERRENO, ID_SENSOR)
VALUES ('BR101-F', 85.0, 27.0, 33, 'SECO', NULL);

INSERT INTO T_MTV_TRECHO_RODOVIA (CODIGO, KM, ALTURA_VEGETACAO, DIAS_SEM_ROCADA, TIPO_TERRENO, ID_SENSOR)
VALUES ('BR101-G', 96.5, 52.0, 75, 'UMIDO', 'IOT-004');

-- ---------------------------------------------------------------------
-- 2. Equipes (Sprint 1: equipe associada a um trecho critico)
-- ---------------------------------------------------------------------
INSERT INTO T_MTV_EQUIPE_MANUTENCAO (NOME, ID_TRECHO)
VALUES ('Equipe Norte', (SELECT ID_TRECHO FROM T_MTV_TRECHO_RODOVIA WHERE CODIGO = 'BR101-C'));

INSERT INTO T_MTV_EQUIPE_MANUTENCAO (NOME, ID_TRECHO)
VALUES ('Equipe Sul', (SELECT ID_TRECHO FROM T_MTV_TRECHO_RODOVIA WHERE CODIGO = 'BR101-G'));

INSERT INTO T_MTV_EQUIPE_MANUTENCAO (NOME, ID_TRECHO)
VALUES ('Equipe Leste', NULL);

-- ---------------------------------------------------------------------
-- 3. Intervencoes ja executadas (historico)
-- ---------------------------------------------------------------------
INSERT INTO T_MTV_INTERVENCAO (TIPO, ID_TRECHO, ID_EQUIPE, DATA_EXECUCAO, ALTURA_ANTES, ALTURA_DEPOIS, DESCRICAO)
VALUES ('MECANIZADA',
        (SELECT ID_TRECHO FROM T_MTV_TRECHO_RODOVIA WHERE CODIGO = 'BR101-B'),
        (SELECT ID_EQUIPE FROM T_MTV_EQUIPE_MANUTENCAO WHERE NOME = 'Equipe Norte'),
        TIMESTAMP '2026-08-15 08:30:00', 48.0, 5.0,
        '[Rocada Mecanizada] KM 27.5: trator cortou de 48.0 cm para 5.0 cm.');

INSERT INTO T_MTV_INTERVENCAO (TIPO, ID_TRECHO, ID_EQUIPE, DATA_EXECUCAO, ALTURA_ANTES, ALTURA_DEPOIS, DESCRICAO)
VALUES ('MANUAL',
        (SELECT ID_TRECHO FROM T_MTV_TRECHO_RODOVIA WHERE CODIGO = 'BR101-D'),
        (SELECT ID_EQUIPE FROM T_MTV_EQUIPE_MANUTENCAO WHERE NOME = 'Equipe Leste'),
        TIMESTAMP '2026-09-01 09:15:00', 19.0, 8.0,
        '[Rocada Manual] KM 58.0: equipe a pe cortou de 19.0 cm para 8.0 cm.');

INSERT INTO T_MTV_INTERVENCAO (TIPO, ID_TRECHO, ID_EQUIPE, DATA_EXECUCAO, ALTURA_ANTES, ALTURA_DEPOIS, DESCRICAO)
VALUES ('PULVERIZACAO',
        (SELECT ID_TRECHO FROM T_MTV_TRECHO_RODOVIA WHERE CODIGO = 'BR101-A'),
        (SELECT ID_EQUIPE FROM T_MTV_EQUIPE_MANUTENCAO WHERE NOME = 'Equipe Sul'),
        TIMESTAMP '2026-09-10 14:00:00', 28.0, 10.0,
        '[Pulverizacao] KM 12.0: herbicida aplicado, vegetacao de 28.0 cm sera dessecada (rebrote retardado).');

-- ---------------------------------------------------------------------
-- 4. Relatorio historico (gerado antes das intervencoes acima)
-- ---------------------------------------------------------------------
INSERT INTO T_MTV_RELATORIO_PRIORIDADE (DATA_GERACAO, QT_CRITICA, QT_ALTA, QT_MEDIA, QT_BAIXA, RESUMO)
VALUES (TIMESTAMP '2026-08-14 07:00:00', 1, 2, 2, 2,
        'CRITICA BR101-B KM 27.5 48.0cm -> Rocada Mecanizada | ALTA BR101-A KM 12.0 28.0cm -> Pulverizacao | ALTA BR101-E KM 73.0 26.0cm -> Pulverizacao | MEDIA BR101-C KM 40.0 20.0cm -> Rocada Manual | MEDIA BR101-D KM 58.0 19.0cm -> Rocada Manual | BAIXA BR101-F KM 85.0 8.0cm -> Nenhuma (monitorar) | BAIXA BR101-G KM 96.5 6.0cm -> Nenhuma (monitorar)');

COMMIT;

-- ---------------------------------------------------------------------
-- 5. Conferencia
-- ---------------------------------------------------------------------
SELECT 'T_MTV_TRECHO_RODOVIA' AS TABELA, COUNT(*) AS QTD FROM T_MTV_TRECHO_RODOVIA
UNION ALL
SELECT 'T_MTV_EQUIPE_MANUTENCAO', COUNT(*) FROM T_MTV_EQUIPE_MANUTENCAO
UNION ALL
SELECT 'T_MTV_INTERVENCAO', COUNT(*) FROM T_MTV_INTERVENCAO
UNION ALL
SELECT 'T_MTV_RELATORIO_PRIORIDADE', COUNT(*) FROM T_MTV_RELATORIO_PRIORIDADE;
