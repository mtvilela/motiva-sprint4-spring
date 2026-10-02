#!/usr/bin/env bash
# =====================================================================
#  MOTIVA - Roteiro de demonstracao da API REST (Sprint 4)
#
#  E o antigo main.Main da Sprint 3, agora feito com requisicoes HTTP:
#  CRUD de trecho, equipe e intervencao + relatorio de prioridade.
#  Cada passo (comando curl, status HTTP e JSON de resposta) e registrado
#  em docs/evidencias-requisicoes.md.
#
#  Uso, com a API rodando (./mvnw spring-boot:run):
#      ./docs/demo-api.sh                 # http://localhost:8080
#      ./docs/demo-api.sh http://host:porta
#
#  Os registros temporarios criados aqui sao removidos no final (como o
#  Main da Sprint 3 fazia). O relatorio gerado fica no historico: esse e
#  o comportamento esperado do POST /api/relatorios.
#  Requer: curl. Opcional: python3 (so para identar o JSON).
# =====================================================================
set -uo pipefail
cd "$(dirname "$0")/.."

BASE="${1:-http://localhost:8080}"
SAIDA="docs/evidencias-requisicoes.md"
SUFIXO="$(date +%H%M%S)"
HOJE="$(date +%Y-%m-%d)"
AGORA="$(date +%Y-%m-%dT%H:%M:%S)"
PASSO=0
SECOES="$(mktemp)"
TABELA="$(mktemp)"
trap 'rm -f "$SECOES" "$TABELA"' EXIT

if ! curl -s -o /dev/null "$BASE/api/trechos"; then
    echo "API fora do ar em $BASE. Suba com ./mvnw spring-boot:run e tente de novo." >&2
    exit 1
fi

formatar_json() {
    if command -v python3 >/dev/null 2>&1; then
        python3 -c 'import json, sys
dados = sys.stdin.read()
try:
    print(json.dumps(json.loads(dados), indent=2, ensure_ascii=False))
except ValueError:
    print(dados)'
    else
        cat
    fi
}

# requisicao "Titulo" METODO /caminho [corpo-json]
# Deixa o corpo da resposta em $RESPOSTA e o status em $STATUS.
requisicao() {
    local titulo="$1" metodo="$2" caminho="$3" corpo="${4:-}"
    local cabecalhos corpo_resposta location
    PASSO=$((PASSO + 1))
    cabecalhos="$(mktemp)"
    corpo_resposta="$(mktemp)"

    if [ -n "$corpo" ]; then
        STATUS=$(curl -s -o "$corpo_resposta" -D "$cabecalhos" -w '%{http_code}' -X "$metodo" "$BASE$caminho" \
            -H 'Content-Type: application/json' -d "$corpo")
    else
        STATUS=$(curl -s -o "$corpo_resposta" -D "$cabecalhos" -w '%{http_code}' -X "$metodo" "$BASE$caminho")
    fi
    RESPOSTA="$(cat "$corpo_resposta")"
    location="$(grep -i '^Location:' "$cabecalhos" | tr -d '\r' | cut -d' ' -f2-)"

    {
        echo "### $PASSO. $titulo"
        echo
        echo '```bash'
        if [ -n "$corpo" ]; then
            echo "curl -X $metodo \"$BASE$caminho\" -H \"Content-Type: application/json\" -d '$corpo'"
        else
            echo "curl -X $metodo \"$BASE$caminho\""
        fi
        echo '```'
        echo
        if [ -n "$location" ]; then
            echo "Resposta: **HTTP $STATUS** · \`Location: $location\`"
        else
            echo "Resposta: **HTTP $STATUS**"
        fi
        echo
        if [ -n "$RESPOSTA" ]; then
            echo '```json'
            echo "$RESPOSTA" | formatar_json
            echo '```'
        else
            echo '_(sem corpo)_'
        fi
        echo
    } >> "$SECOES"
    echo "| $PASSO | \`$metodo\` | \`$caminho\` | $titulo | **$STATUS** |" >> "$TABELA"
    printf '%2d. %-6s %-52s -> HTTP %s\n' "$PASSO" "$metodo" "$caminho" "$STATUS"
    rm -f "$cabecalhos" "$corpo_resposta"
}

# Os DTOs de resposta comecam pelo id: {"id":12,...}
id_da_resposta() {
    echo "$RESPOSTA" | sed -n 's/^{"id":\([0-9][0-9]*\).*/\1/p'
}

secao() {
    printf '\n## %s\n\n' "$1" >> "$SECOES"
    echo "== $1"
}

CODIGO="DEMO-$SUFIXO"
EQUIPE="Equipe Demo $SUFIXO"

# ---------------------------------------------------------------------
secao "Trechos (CRUD + derived queries)"
requisicao "Listar trechos (polimorfismo: SECO x UMIDO)" GET "/api/trechos"
requisicao "Criar trecho UMIDO" POST "/api/trechos" \
    "{\"codigo\":\"$CODIGO\",\"km\":120.5,\"alturaVegetacao\":20.0,\"diasSemRocada\":5,\"tipoTerreno\":\"UMIDO\",\"idSensor\":\"IOT-$SUFIXO\"}"
TRECHO_ID="$(id_da_resposta)"
requisicao "Buscar trecho por ID" GET "/api/trechos/$TRECHO_ID"
requisicao "Atualizar trecho (PUT)" PUT "/api/trechos/$TRECHO_ID" \
    "{\"codigo\":\"$CODIGO\",\"km\":120.5,\"alturaVegetacao\":22.0,\"diasSemRocada\":8,\"tipoTerreno\":\"UMIDO\",\"idSensor\":\"IOT-$SUFIXO-B\"}"
requisicao "Simular 10 dias de crescimento (umido: 2.5 cm/dia)" POST "/api/trechos/$TRECHO_ID/simular-crescimento?dias=10"
requisicao "Derived query findByAlturaVegetacaoGreaterThanEqual" GET "/api/trechos?alturaMinima=40"
requisicao "Buscar trecho inexistente" GET "/api/trechos/999999"
requisicao "Criar trecho invalido (Bean Validation)" POST "/api/trechos" \
    "{\"codigo\":\"\",\"km\":-1,\"alturaVegetacao\":-5,\"tipoTerreno\":\"UMIDO\"}"
requisicao "Criar trecho UMIDO sem sensor (regra no Service)" POST "/api/trechos" \
    "{\"codigo\":\"SEM-SENSOR\",\"km\":1,\"alturaVegetacao\":5,\"tipoTerreno\":\"UMIDO\"}"

# ---------------------------------------------------------------------
secao "Equipes (CRUD + regra da Sprint 1)"
requisicao "Criar equipe associada ao trecho critico" POST "/api/equipes" \
    "{\"nome\":\"$EQUIPE\",\"trechoId\":$TRECHO_ID}"
EQUIPE_ID="$(id_da_resposta)"
requisicao "Associar equipe a trecho NAO critico (BR101-D)" POST "/api/equipes" \
    "{\"nome\":\"Equipe Invalida $SUFIXO\",\"trechoId\":4}"
requisicao "Listar equipes" GET "/api/equipes"
requisicao "Atualizar equipe (renomear e desassociar)" PUT "/api/equipes/$EQUIPE_ID" \
    "{\"nome\":\"$EQUIPE - Plantao\",\"trechoId\":null}"

# ---------------------------------------------------------------------
secao "Intervencoes (CRUD + motor de regras)"
requisicao "Registrar intervencao sem tipo (o MotorDeRegras escolhe)" POST "/api/intervencoes" \
    "{\"trechoId\":$TRECHO_ID,\"equipeId\":$EQUIPE_ID}"
INTERVENCAO_ID="$(id_da_resposta)"
requisicao "Trecho depois da intervencao (dirty checking gravou a nova altura)" GET "/api/trechos/$TRECHO_ID"
requisicao "Derived query findByTipo" GET "/api/intervencoes?tipo=MECANIZADA"
requisicao "Buscar intervencao por ID" GET "/api/intervencoes/$INTERVENCAO_ID"
requisicao "Corrigir o registro (PUT)" PUT "/api/intervencoes/$INTERVENCAO_ID" \
    "{\"tipo\":\"MECANIZADA\",\"trechoId\":$TRECHO_ID,\"equipeId\":$EQUIPE_ID,\"dataExecucao\":\"$AGORA\",\"alturaAntes\":47.0,\"alturaDepois\":5.0,\"descricao\":\"Rocada mecanizada conferida em campo pela equipe.\"}"
requisicao "Apagar trecho que tem intervencao (FK)" DELETE "/api/trechos/$TRECHO_ID"

# ---------------------------------------------------------------------
secao "Limpeza (DELETE)"
requisicao "Remover intervencao" DELETE "/api/intervencoes/$INTERVENCAO_ID"
requisicao "Remover equipe" DELETE "/api/equipes/$EQUIPE_ID"
requisicao "Remover trecho" DELETE "/api/trechos/$TRECHO_ID"
requisicao "Buscar o trecho removido" GET "/api/trechos/$TRECHO_ID"

# ---------------------------------------------------------------------
secao "Relatorio de prioridade (motor da Sprint 2 como servico REST)"
requisicao "Gerar relatorio a partir dos trechos atuais e salvar no historico" POST "/api/relatorios"
requisicao "Listar historico" GET "/api/relatorios"
requisicao "Consultar historico por periodo" GET "/api/relatorios/periodo?inicio=2026-08-01&fim=$HOJE"
requisicao "Periodo invalido (inicio depois do fim)" GET "/api/relatorios/periodo?inicio=$HOJE&fim=2026-08-01"

# ---------------------------------------------------------------------
{
    echo "# Evidencias: requisicoes contra o Oracle da FIAP"
    echo
    echo "Registro gerado por \`docs/demo-api.sh\` em $(date '+%d/%m/%Y %H:%M:%S'),"
    echo "com a API rodando em \`$BASE\` e conectada ao Oracle \`oracle.fiap.com.br:1521:ORCL\`"
    echo "(tabelas \`T_MTV_*\` da Sprint 3). Cada passo mostra o comando cURL, o status HTTP e o JSON devolvido."
    echo
    echo "| # | Metodo | Rota | Passo | Status |"
    echo "|---|---|---|---|---|"
    cat "$TABELA"
    cat "$SECOES"
} > "$SAIDA"

echo
echo "Registro salvo em $SAIDA ($PASSO requisicoes)."
