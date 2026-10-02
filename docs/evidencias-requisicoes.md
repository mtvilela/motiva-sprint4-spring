# Evidencias: requisicoes contra o Oracle da FIAP

Registro gerado por `docs/demo-api.sh` em 02/10/2026 11:44:37,
com a API rodando em `http://localhost:8080` e conectada ao Oracle `oracle.fiap.com.br:1521:ORCL`
(tabelas `T_MTV_*` da Sprint 3). Cada passo mostra o comando cURL, o status HTTP e o JSON devolvido.

| # | Metodo | Rota | Passo | Status |
|---|---|---|---|---|
| 1 | `GET` | `/api/trechos` | Listar trechos (polimorfismo: SECO x UMIDO) | **200** |
| 2 | `POST` | `/api/trechos` | Criar trecho UMIDO | **201** |
| 3 | `GET` | `/api/trechos/9` | Buscar trecho por ID | **200** |
| 4 | `PUT` | `/api/trechos/9` | Atualizar trecho (PUT) | **200** |
| 5 | `POST` | `/api/trechos/9/simular-crescimento?dias=10` | Simular 10 dias de crescimento (umido: 2.5 cm/dia) | **200** |
| 6 | `GET` | `/api/trechos?alturaMinima=40` | Derived query findByAlturaVegetacaoGreaterThanEqual | **200** |
| 7 | `GET` | `/api/trechos/999999` | Buscar trecho inexistente | **404** |
| 8 | `POST` | `/api/trechos` | Criar trecho invalido (Bean Validation) | **400** |
| 9 | `POST` | `/api/trechos` | Criar trecho UMIDO sem sensor (regra no Service) | **400** |
| 10 | `POST` | `/api/equipes` | Criar equipe associada ao trecho critico | **201** |
| 11 | `POST` | `/api/equipes` | Associar equipe a trecho NAO critico (BR101-D) | **400** |
| 12 | `GET` | `/api/equipes` | Listar equipes | **200** |
| 13 | `PUT` | `/api/equipes/4` | Atualizar equipe (renomear e desassociar) | **200** |
| 14 | `POST` | `/api/intervencoes` | Registrar intervencao sem tipo (o MotorDeRegras escolhe) | **201** |
| 15 | `GET` | `/api/trechos/9` | Trecho depois da intervencao (dirty checking gravou a nova altura) | **200** |
| 16 | `GET` | `/api/intervencoes?tipo=MECANIZADA` | Derived query findByTipo | **200** |
| 17 | `GET` | `/api/intervencoes/5` | Buscar intervencao por ID | **200** |
| 18 | `PUT` | `/api/intervencoes/5` | Corrigir o registro (PUT) | **200** |
| 19 | `DELETE` | `/api/trechos/9` | Apagar trecho que tem intervencao (FK) | **409** |
| 20 | `DELETE` | `/api/intervencoes/5` | Remover intervencao | **204** |
| 21 | `DELETE` | `/api/equipes/4` | Remover equipe | **204** |
| 22 | `DELETE` | `/api/trechos/9` | Remover trecho | **204** |
| 23 | `GET` | `/api/trechos/9` | Buscar o trecho removido | **404** |
| 24 | `POST` | `/api/relatorios` | Gerar relatorio a partir dos trechos atuais e salvar no historico | **201** |
| 25 | `GET` | `/api/relatorios` | Listar historico | **200** |
| 26 | `GET` | `/api/relatorios/periodo?inicio=2026-08-01&fim=2026-10-02` | Consultar historico por periodo | **200** |
| 27 | `GET` | `/api/relatorios/periodo?inicio=2026-10-02&fim=2026-08-01` | Periodo invalido (inicio depois do fim) | **400** |

## Trechos (CRUD + derived queries)

### 1. Listar trechos (polimorfismo: SECO x UMIDO)

```bash
curl -X GET "http://localhost:8080/api/trechos"
```

Resposta: **HTTP 200**

```json
[
  {
    "id": 1,
    "codigo": "BR101-A",
    "km": 12.0,
    "alturaVegetacao": 17.69,
    "diasSemRocada": 10,
    "tipoTerreno": "UMIDO",
    "idSensor": "IOT-001",
    "monitoradoViaIoT": true,
    "taxaCrescimentoDiaria": 2.5,
    "prioridade": "MEDIA"
  },
  {
    "id": 2,
    "codigo": "BR101-B",
    "km": 27.5,
    "alturaVegetacao": 22.0,
    "diasSemRocada": 36,
    "tipoTerreno": "SECO",
    "idSensor": null,
    "monitoradoViaIoT": false,
    "taxaCrescimentoDiaria": 0.8,
    "prioridade": "MEDIA"
  },
  {
    "id": 3,
    "codigo": "BR101-C",
    "km": 40.0,
    "alturaVegetacao": 44.77,
    "diasSemRocada": 60,
    "tipoTerreno": "UMIDO",
    "idSensor": "IOT-002",
    "monitoradoViaIoT": true,
    "taxaCrescimentoDiaria": 2.5,
    "prioridade": "CRITICA"
  },
  {
    "id": 4,
    "codigo": "BR101-D",
    "km": 58.0,
    "alturaVegetacao": 9.0,
    "diasSemRocada": 19,
    "tipoTerreno": "SECO",
    "idSensor": null,
    "monitoradoViaIoT": false,
    "taxaCrescimentoDiaria": 0.8,
    "prioridade": "BAIXA"
  },
  {
    "id": 5,
    "codigo": "BR101-E",
    "km": 73.0,
    "alturaVegetacao": 30.64,
    "diasSemRocada": 45,
    "tipoTerreno": "UMIDO",
    "idSensor": "IOT-003",
    "monitoradoViaIoT": true,
    "taxaCrescimentoDiaria": 2.5,
    "prioridade": "ALTA"
  },
  {
    "id": 6,
    "codigo": "BR101-F",
    "km": 85.0,
    "alturaVegetacao": 27.0,
    "diasSemRocada": 33,
    "tipoTerreno": "SECO",
    "idSensor": null,
    "monitoradoViaIoT": false,
    "taxaCrescimentoDiaria": 0.8,
    "prioridade": "ALTA"
  },
  {
    "id": 7,
    "codigo": "BR101-G",
    "km": 96.5,
    "alturaVegetacao": 52.18,
    "diasSemRocada": 75,
    "tipoTerreno": "UMIDO",
    "idSensor": "IOT-004",
    "monitoradoViaIoT": true,
    "taxaCrescimentoDiaria": 2.5,
    "prioridade": "CRITICA"
  }
]
```

### 2. Criar trecho UMIDO

```bash
curl -X POST "http://localhost:8080/api/trechos" -H "Content-Type: application/json" -d '{"codigo":"DEMO-114435","km":120.5,"alturaVegetacao":20.0,"diasSemRocada":5,"tipoTerreno":"UMIDO","idSensor":"IOT-114435"}'
```

Resposta: **HTTP 201** · `Location: http://localhost:8080/api/trechos/9`

```json
{
  "id": 9,
  "codigo": "DEMO-114435",
  "km": 120.5,
  "alturaVegetacao": 20.0,
  "diasSemRocada": 5,
  "tipoTerreno": "UMIDO",
  "idSensor": "IOT-114435",
  "monitoradoViaIoT": true,
  "taxaCrescimentoDiaria": 2.5,
  "prioridade": "MEDIA"
}
```

### 3. Buscar trecho por ID

```bash
curl -X GET "http://localhost:8080/api/trechos/9"
```

Resposta: **HTTP 200**

```json
{
  "id": 9,
  "codigo": "DEMO-114435",
  "km": 120.5,
  "alturaVegetacao": 20.0,
  "diasSemRocada": 5,
  "tipoTerreno": "UMIDO",
  "idSensor": "IOT-114435",
  "monitoradoViaIoT": true,
  "taxaCrescimentoDiaria": 2.5,
  "prioridade": "MEDIA"
}
```

### 4. Atualizar trecho (PUT)

```bash
curl -X PUT "http://localhost:8080/api/trechos/9" -H "Content-Type: application/json" -d '{"codigo":"DEMO-114435","km":120.5,"alturaVegetacao":22.0,"diasSemRocada":8,"tipoTerreno":"UMIDO","idSensor":"IOT-114435-B"}'
```

Resposta: **HTTP 200**

```json
{
  "id": 9,
  "codigo": "DEMO-114435",
  "km": 120.5,
  "alturaVegetacao": 22.0,
  "diasSemRocada": 8,
  "tipoTerreno": "UMIDO",
  "idSensor": "IOT-114435-B",
  "monitoradoViaIoT": true,
  "taxaCrescimentoDiaria": 2.5,
  "prioridade": "MEDIA"
}
```

### 5. Simular 10 dias de crescimento (umido: 2.5 cm/dia)

```bash
curl -X POST "http://localhost:8080/api/trechos/9/simular-crescimento?dias=10"
```

Resposta: **HTTP 200**

```json
{
  "id": 9,
  "codigo": "DEMO-114435",
  "km": 120.5,
  "alturaVegetacao": 47.0,
  "diasSemRocada": 18,
  "tipoTerreno": "UMIDO",
  "idSensor": "IOT-114435-B",
  "monitoradoViaIoT": true,
  "taxaCrescimentoDiaria": 2.5,
  "prioridade": "CRITICA"
}
```

### 6. Derived query findByAlturaVegetacaoGreaterThanEqual

```bash
curl -X GET "http://localhost:8080/api/trechos?alturaMinima=40"
```

Resposta: **HTTP 200**

```json
[
  {
    "id": 7,
    "codigo": "BR101-G",
    "km": 96.5,
    "alturaVegetacao": 52.18,
    "diasSemRocada": 75,
    "tipoTerreno": "UMIDO",
    "idSensor": "IOT-004",
    "monitoradoViaIoT": true,
    "taxaCrescimentoDiaria": 2.5,
    "prioridade": "CRITICA"
  },
  {
    "id": 9,
    "codigo": "DEMO-114435",
    "km": 120.5,
    "alturaVegetacao": 47.0,
    "diasSemRocada": 18,
    "tipoTerreno": "UMIDO",
    "idSensor": "IOT-114435-B",
    "monitoradoViaIoT": true,
    "taxaCrescimentoDiaria": 2.5,
    "prioridade": "CRITICA"
  },
  {
    "id": 3,
    "codigo": "BR101-C",
    "km": 40.0,
    "alturaVegetacao": 44.77,
    "diasSemRocada": 60,
    "tipoTerreno": "UMIDO",
    "idSensor": "IOT-002",
    "monitoradoViaIoT": true,
    "taxaCrescimentoDiaria": 2.5,
    "prioridade": "CRITICA"
  }
]
```

### 7. Buscar trecho inexistente

```bash
curl -X GET "http://localhost:8080/api/trechos/999999"
```

Resposta: **HTTP 404**

```json
{
  "timestamp": "2026-10-02T11:44:36",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Trecho 999999 nao encontrado.",
  "caminho": "/api/trechos/999999"
}
```

### 8. Criar trecho invalido (Bean Validation)

```bash
curl -X POST "http://localhost:8080/api/trechos" -H "Content-Type: application/json" -d '{"codigo":"","km":-1,"alturaVegetacao":-5,"tipoTerreno":"UMIDO"}'
```

Resposta: **HTTP 400**

```json
{
  "timestamp": "2026-10-02T11:44:36",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados invalidos: corrija os campos indicados.",
  "caminho": "/api/trechos",
  "campos": {
    "alturaVegetacao": "nao pode ser negativa",
    "km": "nao pode ser negativo",
    "codigo": "e obrigatorio"
  }
}
```

### 9. Criar trecho UMIDO sem sensor (regra no Service)

```bash
curl -X POST "http://localhost:8080/api/trechos" -H "Content-Type: application/json" -d '{"codigo":"SEM-SENSOR","km":1,"alturaVegetacao":5,"tipoTerreno":"UMIDO"}'
```

Resposta: **HTTP 400**

```json
{
  "timestamp": "2026-10-02T11:44:36",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Um trecho UMIDO precisa de um sensor IoT instalado (idSensor).",
  "caminho": "/api/trechos"
}
```


## Equipes (CRUD + regra da Sprint 1)

### 10. Criar equipe associada ao trecho critico

```bash
curl -X POST "http://localhost:8080/api/equipes" -H "Content-Type: application/json" -d '{"nome":"Equipe Demo 114435","trechoId":9}'
```

Resposta: **HTTP 201** · `Location: http://localhost:8080/api/equipes/4`

```json
{
  "id": 4,
  "nome": "Equipe Demo 114435",
  "trechoResponsavel": {
    "id": 9,
    "codigo": "DEMO-114435",
    "km": 120.5,
    "alturaVegetacao": 47.0
  }
}
```

### 11. Associar equipe a trecho NAO critico (BR101-D)

```bash
curl -X POST "http://localhost:8080/api/equipes" -H "Content-Type: application/json" -d '{"nome":"Equipe Invalida 114435","trechoId":4}'
```

Resposta: **HTTP 400**

```json
{
  "timestamp": "2026-10-02T11:44:36",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "A equipe so pode ser associada a um trecho CRITICO (>= 40 cm). O trecho BR101-D esta com 9.0 cm (BAIXA).",
  "caminho": "/api/equipes"
}
```

### 12. Listar equipes

```bash
curl -X GET "http://localhost:8080/api/equipes"
```

Resposta: **HTTP 200**

```json
[
  {
    "id": 4,
    "nome": "Equipe Demo 114435",
    "trechoResponsavel": {
      "id": 9,
      "codigo": "DEMO-114435",
      "km": 120.5,
      "alturaVegetacao": 47.0
    }
  },
  {
    "id": 3,
    "nome": "Equipe Leste",
    "trechoResponsavel": null
  },
  {
    "id": 1,
    "nome": "Equipe Norte",
    "trechoResponsavel": {
      "id": 3,
      "codigo": "BR101-C",
      "km": 40.0,
      "alturaVegetacao": 44.77
    }
  },
  {
    "id": 2,
    "nome": "Equipe Sul",
    "trechoResponsavel": {
      "id": 7,
      "codigo": "BR101-G",
      "km": 96.5,
      "alturaVegetacao": 52.18
    }
  }
]
```

### 13. Atualizar equipe (renomear e desassociar)

```bash
curl -X PUT "http://localhost:8080/api/equipes/4" -H "Content-Type: application/json" -d '{"nome":"Equipe Demo 114435 - Plantao","trechoId":null}'
```

Resposta: **HTTP 200**

```json
{
  "id": 4,
  "nome": "Equipe Demo 114435 - Plantao",
  "trechoResponsavel": null
}
```


## Intervencoes (CRUD + motor de regras)

### 14. Registrar intervencao sem tipo (o MotorDeRegras escolhe)

```bash
curl -X POST "http://localhost:8080/api/intervencoes" -H "Content-Type: application/json" -d '{"trechoId":9,"equipeId":4}'
```

Resposta: **HTTP 201** · `Location: http://localhost:8080/api/intervencoes/5`

```json
{
  "id": 5,
  "tipo": "MECANIZADA",
  "servico": "Rocada Mecanizada",
  "trecho": {
    "id": 9,
    "codigo": "DEMO-114435",
    "km": 120.5,
    "alturaVegetacao": 5.0
  },
  "equipe": {
    "id": 4,
    "nome": "Equipe Demo 114435 - Plantao"
  },
  "dataExecucao": "2026-10-02T11:44:36",
  "alturaAntes": 47.0,
  "alturaDepois": 5.0,
  "descricao": "[Rocada Mecanizada] KM 120.5: trator cortou de 47.0 cm para 5.0 cm."
}
```

### 15. Trecho depois da intervencao (dirty checking gravou a nova altura)

```bash
curl -X GET "http://localhost:8080/api/trechos/9"
```

Resposta: **HTTP 200**

```json
{
  "id": 9,
  "codigo": "DEMO-114435",
  "km": 120.5,
  "alturaVegetacao": 5.0,
  "diasSemRocada": 0,
  "tipoTerreno": "UMIDO",
  "idSensor": "IOT-114435-B",
  "monitoradoViaIoT": true,
  "taxaCrescimentoDiaria": 2.5,
  "prioridade": "BAIXA"
}
```

### 16. Derived query findByTipo

```bash
curl -X GET "http://localhost:8080/api/intervencoes?tipo=MECANIZADA"
```

Resposta: **HTTP 200**

```json
[
  {
    "id": 5,
    "tipo": "MECANIZADA",
    "servico": "Rocada Mecanizada",
    "trecho": {
      "id": 9,
      "codigo": "DEMO-114435",
      "km": 120.5,
      "alturaVegetacao": 5.0
    },
    "equipe": {
      "id": 4,
      "nome": "Equipe Demo 114435 - Plantao"
    },
    "dataExecucao": "2026-10-02T11:44:36",
    "alturaAntes": 47.0,
    "alturaDepois": 5.0,
    "descricao": "[Rocada Mecanizada] KM 120.5: trator cortou de 47.0 cm para 5.0 cm."
  },
  {
    "id": 1,
    "tipo": "MECANIZADA",
    "servico": "Rocada Mecanizada",
    "trecho": {
      "id": 2,
      "codigo": "BR101-B",
      "km": 27.5,
      "alturaVegetacao": 22.0
    },
    "equipe": {
      "id": 1,
      "nome": "Equipe Norte"
    },
    "dataExecucao": "2026-08-15T08:30:00",
    "alturaAntes": 48.0,
    "alturaDepois": 5.0,
    "descricao": "[Rocada Mecanizada] KM 27.5: trator cortou de 48.0 cm para 5.0 cm."
  }
]
```

### 17. Buscar intervencao por ID

```bash
curl -X GET "http://localhost:8080/api/intervencoes/5"
```

Resposta: **HTTP 200**

```json
{
  "id": 5,
  "tipo": "MECANIZADA",
  "servico": "Rocada Mecanizada",
  "trecho": {
    "id": 9,
    "codigo": "DEMO-114435",
    "km": 120.5,
    "alturaVegetacao": 5.0
  },
  "equipe": {
    "id": 4,
    "nome": "Equipe Demo 114435 - Plantao"
  },
  "dataExecucao": "2026-10-02T11:44:36",
  "alturaAntes": 47.0,
  "alturaDepois": 5.0,
  "descricao": "[Rocada Mecanizada] KM 120.5: trator cortou de 47.0 cm para 5.0 cm."
}
```

### 18. Corrigir o registro (PUT)

```bash
curl -X PUT "http://localhost:8080/api/intervencoes/5" -H "Content-Type: application/json" -d '{"tipo":"MECANIZADA","trechoId":9,"equipeId":4,"dataExecucao":"2026-10-02T11:44:35","alturaAntes":47.0,"alturaDepois":5.0,"descricao":"Rocada mecanizada conferida em campo pela equipe."}'
```

Resposta: **HTTP 200**

```json
{
  "id": 5,
  "tipo": "MECANIZADA",
  "servico": "Rocada Mecanizada",
  "trecho": {
    "id": 9,
    "codigo": "DEMO-114435",
    "km": 120.5,
    "alturaVegetacao": 5.0
  },
  "equipe": {
    "id": 4,
    "nome": "Equipe Demo 114435 - Plantao"
  },
  "dataExecucao": "2026-10-02T11:44:35",
  "alturaAntes": 47.0,
  "alturaDepois": 5.0,
  "descricao": "Rocada mecanizada conferida em campo pela equipe."
}
```

### 19. Apagar trecho que tem intervencao (FK)

```bash
curl -X DELETE "http://localhost:8080/api/trechos/9"
```

Resposta: **HTTP 409**

```json
{
  "timestamp": "2026-10-02T11:44:37",
  "status": 409,
  "erro": "Conflict",
  "mensagem": "O trecho DEMO-114435 possui 1 intervencao(oes) registrada(s) e nao pode ser removido.",
  "caminho": "/api/trechos/9"
}
```


## Limpeza (DELETE)

### 20. Remover intervencao

```bash
curl -X DELETE "http://localhost:8080/api/intervencoes/5"
```

Resposta: **HTTP 204**

_(sem corpo)_

### 21. Remover equipe

```bash
curl -X DELETE "http://localhost:8080/api/equipes/4"
```

Resposta: **HTTP 204**

_(sem corpo)_

### 22. Remover trecho

```bash
curl -X DELETE "http://localhost:8080/api/trechos/9"
```

Resposta: **HTTP 204**

_(sem corpo)_

### 23. Buscar o trecho removido

```bash
curl -X GET "http://localhost:8080/api/trechos/9"
```

Resposta: **HTTP 404**

```json
{
  "timestamp": "2026-10-02T11:44:37",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Trecho 9 nao encontrado.",
  "caminho": "/api/trechos/9"
}
```


## Relatorio de prioridade (motor da Sprint 2 como servico REST)

### 24. Gerar relatorio a partir dos trechos atuais e salvar no historico

```bash
curl -X POST "http://localhost:8080/api/relatorios"
```

Resposta: **HTTP 201** · `Location: http://localhost:8080/api/relatorios/3`

```json
{
  "id": 3,
  "dataGeracao": "2026-10-02T11:44:37",
  "qtCritica": 2,
  "qtAlta": 2,
  "qtMedia": 2,
  "qtBaixa": 1,
  "totalTrechos": 7,
  "qtComIntervencao": 6,
  "resumo": "CRITICA BR101-C KM 40.0 44.6cm -> Rocada Mecanizada | CRITICA BR101-G KM 96.5 52.6cm -> Rocada Mecanizada | ALTA BR101-E KM 73.0 30.5cm -> Pulverizacao | ALTA BR101-F KM 85.0 27.0cm -> Rocada Mecanizada | MEDIA BR101-A KM 12.0 16.8cm -> Rocada Manual | MEDIA BR101-B KM 27.5 22.0cm -> Rocada Manual | BAIXA BR101-D KM 58.0 9.0cm -> Nenhuma (monitorar)",
  "itens": [
    {
      "prioridade": "CRITICA",
      "trechoId": 3,
      "codigo": "BR101-C",
      "km": 40.0,
      "alturaVegetacao": 44.58,
      "tipoTerreno": "UMIDO",
      "fonte": "IoT",
      "tipoIntervencao": "MECANIZADA",
      "intervencaoRecomendada": "Rocada Mecanizada"
    },
    {
      "prioridade": "CRITICA",
      "trechoId": 7,
      "codigo": "BR101-G",
      "km": 96.5,
      "alturaVegetacao": 52.56,
      "tipoTerreno": "UMIDO",
      "fonte": "IoT",
      "tipoIntervencao": "MECANIZADA",
      "intervencaoRecomendada": "Rocada Mecanizada"
    },
    {
      "prioridade": "ALTA",
      "trechoId": 5,
      "codigo": "BR101-E",
      "km": 73.0,
      "alturaVegetacao": 30.5,
      "tipoTerreno": "UMIDO",
      "fonte": "IoT",
      "tipoIntervencao": "PULVERIZACAO",
      "intervencaoRecomendada": "Pulverizacao"
    },
    {
      "prioridade": "ALTA",
      "trechoId": 6,
      "codigo": "BR101-F",
      "km": 85.0,
      "alturaVegetacao": 27.0,
      "tipoTerreno": "SECO",
      "fonte": "Visual",
      "tipoIntervencao": "MECANIZADA",
      "intervencaoRecomendada": "Rocada Mecanizada"
    },
    {
      "prioridade": "MEDIA",
      "trechoId": 1,
      "codigo": "BR101-A",
      "km": 12.0,
      "alturaVegetacao": 16.78,
      "tipoTerreno": "UMIDO",
      "fonte": "IoT",
      "tipoIntervencao": "MANUAL",
      "intervencaoRecomendada": "Rocada Manual"
    },
    {
      "prioridade": "MEDIA",
      "trechoId": 2,
      "codigo": "BR101-B",
      "km": 27.5,
      "alturaVegetacao": 22.0,
      "tipoTerreno": "SECO",
      "fonte": "Visual",
      "tipoIntervencao": "MANUAL",
      "intervencaoRecomendada": "Rocada Manual"
    },
    {
      "prioridade": "BAIXA",
      "trechoId": 4,
      "codigo": "BR101-D",
      "km": 58.0,
      "alturaVegetacao": 9.0,
      "tipoTerreno": "SECO",
      "fonte": "Visual",
      "tipoIntervencao": null,
      "intervencaoRecomendada": "Nenhuma (monitorar)"
    }
  ]
}
```

### 25. Listar historico

```bash
curl -X GET "http://localhost:8080/api/relatorios"
```

Resposta: **HTTP 200**

```json
[
  {
    "id": 3,
    "dataGeracao": "2026-10-02T11:44:37",
    "qtCritica": 2,
    "qtAlta": 2,
    "qtMedia": 2,
    "qtBaixa": 1,
    "totalTrechos": 7,
    "qtComIntervencao": 6,
    "resumo": "CRITICA BR101-C KM 40.0 44.6cm -> Rocada Mecanizada | CRITICA BR101-G KM 96.5 52.6cm -> Rocada Mecanizada | ALTA BR101-E KM 73.0 30.5cm -> Pulverizacao | ALTA BR101-F KM 85.0 27.0cm -> Rocada Mecanizada | MEDIA BR101-A KM 12.0 16.8cm -> Rocada Manual | MEDIA BR101-B KM 27.5 22.0cm -> Rocada Manual | BAIXA BR101-D KM 58.0 9.0cm -> Nenhuma (monitorar)"
  },
  {
    "id": 2,
    "dataGeracao": "2026-09-20T18:55:07.503355",
    "qtCritica": 2,
    "qtAlta": 2,
    "qtMedia": 2,
    "qtBaixa": 1,
    "totalTrechos": 7,
    "qtComIntervencao": 6,
    "resumo": "CRITICA BR101-C KM 40,0 44,8cm -> Rocada Mecanizada | CRITICA BR101-G KM 96,5 52,2cm -> Rocada Mecanizada | ALTA BR101-E KM 73,0 30,6cm -> Pulverizacao | ALTA BR101-F KM 85,0 27,0cm -> Rocada Mecanizada | MEDIA BR101-A KM 12,0 17,7cm -> Rocada Manual | MEDIA BR101-B KM 27,5 22,0cm -> Rocada Manual | BAIXA BR101-D KM 58,0 9,0cm -> Nenhuma (monitorar)"
  },
  {
    "id": 1,
    "dataGeracao": "2026-08-14T07:00:00",
    "qtCritica": 1,
    "qtAlta": 2,
    "qtMedia": 2,
    "qtBaixa": 2,
    "totalTrechos": 7,
    "qtComIntervencao": 5,
    "resumo": "CRITICA BR101-B KM 27.5 48.0cm -> Rocada Mecanizada | ALTA BR101-A KM 12.0 28.0cm -> Pulverizacao | ALTA BR101-E KM 73.0 26.0cm -> Pulverizacao | MEDIA BR101-C KM 40.0 20.0cm -> Rocada Manual | MEDIA BR101-D KM 58.0 19.0cm -> Rocada Manual | BAIXA BR101-F KM 85.0 8.0cm -> Nenhuma (monitorar) | BAIXA BR101-G KM 96.5 6.0cm -> Nenhuma (monitorar)"
  }
]
```

### 26. Consultar historico por periodo

```bash
curl -X GET "http://localhost:8080/api/relatorios/periodo?inicio=2026-08-01&fim=2026-10-02"
```

Resposta: **HTTP 200**

```json
[
  {
    "id": 3,
    "dataGeracao": "2026-10-02T11:44:37",
    "qtCritica": 2,
    "qtAlta": 2,
    "qtMedia": 2,
    "qtBaixa": 1,
    "totalTrechos": 7,
    "qtComIntervencao": 6,
    "resumo": "CRITICA BR101-C KM 40.0 44.6cm -> Rocada Mecanizada | CRITICA BR101-G KM 96.5 52.6cm -> Rocada Mecanizada | ALTA BR101-E KM 73.0 30.5cm -> Pulverizacao | ALTA BR101-F KM 85.0 27.0cm -> Rocada Mecanizada | MEDIA BR101-A KM 12.0 16.8cm -> Rocada Manual | MEDIA BR101-B KM 27.5 22.0cm -> Rocada Manual | BAIXA BR101-D KM 58.0 9.0cm -> Nenhuma (monitorar)"
  },
  {
    "id": 2,
    "dataGeracao": "2026-09-20T18:55:07.503355",
    "qtCritica": 2,
    "qtAlta": 2,
    "qtMedia": 2,
    "qtBaixa": 1,
    "totalTrechos": 7,
    "qtComIntervencao": 6,
    "resumo": "CRITICA BR101-C KM 40,0 44,8cm -> Rocada Mecanizada | CRITICA BR101-G KM 96,5 52,2cm -> Rocada Mecanizada | ALTA BR101-E KM 73,0 30,6cm -> Pulverizacao | ALTA BR101-F KM 85,0 27,0cm -> Rocada Mecanizada | MEDIA BR101-A KM 12,0 17,7cm -> Rocada Manual | MEDIA BR101-B KM 27,5 22,0cm -> Rocada Manual | BAIXA BR101-D KM 58,0 9,0cm -> Nenhuma (monitorar)"
  },
  {
    "id": 1,
    "dataGeracao": "2026-08-14T07:00:00",
    "qtCritica": 1,
    "qtAlta": 2,
    "qtMedia": 2,
    "qtBaixa": 2,
    "totalTrechos": 7,
    "qtComIntervencao": 5,
    "resumo": "CRITICA BR101-B KM 27.5 48.0cm -> Rocada Mecanizada | ALTA BR101-A KM 12.0 28.0cm -> Pulverizacao | ALTA BR101-E KM 73.0 26.0cm -> Pulverizacao | MEDIA BR101-C KM 40.0 20.0cm -> Rocada Manual | MEDIA BR101-D KM 58.0 19.0cm -> Rocada Manual | BAIXA BR101-F KM 85.0 8.0cm -> Nenhuma (monitorar) | BAIXA BR101-G KM 96.5 6.0cm -> Nenhuma (monitorar)"
  }
]
```

### 27. Periodo invalido (inicio depois do fim)

```bash
curl -X GET "http://localhost:8080/api/relatorios/periodo?inicio=2026-10-02&fim=2026-08-01"
```

Resposta: **HTTP 400**

```json
{
  "timestamp": "2026-10-02T11:44:37",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "A data de inicio (2026-10-02) nao pode ser depois da data de fim (2026-08-01).",
  "caminho": "/api/relatorios/periodo"
}
```

