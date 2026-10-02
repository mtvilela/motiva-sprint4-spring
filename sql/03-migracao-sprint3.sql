-- =====================================================================
--  MOTIVA - Sistema de Priorizacao de Rocada | Sprint 4 (POO / FIAP)
--  MIGRACAO das tabelas da Sprint 3: IDENTITY -> SEQUENCE nomeada
--
--  Use este script se o seu schema JA TEM as tabelas T_MTV_* criadas pela
--  Sprint 3. Ele NAO apaga nem altera nenhum dado: so troca a forma como a
--  chave primaria e gerada.
--
--  Por que? Na Sprint 3 as PKs eram GENERATED ALWAYS AS IDENTITY. Por baixo
--  dos panos o Oracle cria uma sequence OCULTA com nome gerado pelo banco
--  (ex.: ISEQ$$_4205327), que nao da para referenciar de forma estavel no
--  @SequenceGenerator do JPA - e o "ALWAYS" proibe o Hibernate de gravar o
--  ID que ele mesmo buscou na sequence. A Sprint 4 pede sequences nomeadas,
--  entao, para cada tabela, este script:
--    1) remove o IDENTITY (a sequence oculta ISEQ$$_ some junto);
--    2) cria a sequence nomeada comecando no MAIOR ID existente + 1;
--    3) define DEFAULT <sequence>.NEXTVAL na coluna, para que INSERTs sem
--       ID (SQL Developer, scripts e o proprio JDBC da Sprint 3) continuem
--       funcionando.
--
--  Idempotente: pode ser executado de novo sem problema (a sequence e
--  recriada a partir do maior ID atual).
--  Execucao: SQL Developer -> abrir o arquivo -> Run Script (F5).
-- =====================================================================

DECLARE
    PROCEDURE migrar(p_tabela IN VARCHAR2, p_coluna IN VARCHAR2, p_sequence IN VARCHAR2) IS
        v_qtd    NUMBER;
        v_inicio NUMBER;
    BEGIN
        SELECT COUNT(*) INTO v_qtd
          FROM user_tab_identity_cols
         WHERE table_name = p_tabela
           AND column_name = p_coluna;
        IF v_qtd > 0 THEN
            EXECUTE IMMEDIATE 'ALTER TABLE ' || p_tabela || ' MODIFY (' || p_coluna || ' DROP IDENTITY)';
        END IF;

        SELECT COUNT(*) INTO v_qtd
          FROM user_sequences
         WHERE sequence_name = p_sequence;
        IF v_qtd > 0 THEN
            EXECUTE IMMEDIATE 'DROP SEQUENCE ' || p_sequence;
        END IF;

        EXECUTE IMMEDIATE 'SELECT NVL(MAX(' || p_coluna || '), 0) + 1 FROM ' || p_tabela INTO v_inicio;
        EXECUTE IMMEDIATE 'CREATE SEQUENCE ' || p_sequence
                       || ' START WITH ' || v_inicio || ' INCREMENT BY 1 NOCACHE';

        EXECUTE IMMEDIATE 'ALTER TABLE ' || p_tabela
                       || ' MODIFY (' || p_coluna || ' DEFAULT ' || p_sequence || '.NEXTVAL)';
    END migrar;
BEGIN
    migrar('T_MTV_TRECHO_RODOVIA',       'ID_TRECHO',      'SQ_MTV_TRECHO');
    migrar('T_MTV_EQUIPE_MANUTENCAO',    'ID_EQUIPE',      'SQ_MTV_EQUIPE');
    migrar('T_MTV_INTERVENCAO',          'ID_INTERVENCAO', 'SQ_MTV_INTERVENCAO');
    migrar('T_MTV_RELATORIO_PRIORIDADE', 'ID_RELATORIO',   'SQ_MTV_RELATORIO');
END;
/

-- ---------------------------------------------------------------------
-- Conferencia
-- ---------------------------------------------------------------------
-- 1) Nenhuma coluna IDENTITY deve sobrar nas tabelas do projeto (0 linhas)
SELECT table_name, column_name
  FROM user_tab_identity_cols
 WHERE table_name LIKE 'T_MTV_%';

-- 2) As 4 sequences nomeadas, cada uma no proximo ID livre
SELECT sequence_name, last_number, increment_by
  FROM user_sequences
 WHERE sequence_name LIKE 'SQ_MTV_%'
 ORDER BY sequence_name;

-- 3) Cada PK agora tem DEFAULT <sequence>.NEXTVAL
SELECT table_name, column_name, data_default
  FROM user_tab_columns
 WHERE (table_name, column_name) IN (('T_MTV_TRECHO_RODOVIA',       'ID_TRECHO'),
                                     ('T_MTV_EQUIPE_MANUTENCAO',    'ID_EQUIPE'),
                                     ('T_MTV_INTERVENCAO',          'ID_INTERVENCAO'),
                                     ('T_MTV_RELATORIO_PRIORIDADE', 'ID_RELATORIO'))
 ORDER BY table_name;
