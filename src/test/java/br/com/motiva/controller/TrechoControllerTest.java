package br.com.motiva.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrechoControllerTest extends ApiTestBase {

    @Test
    void postCriaTrechoUmidoE201ComLocation() throws Exception {
        mockMvc.perform(post("/api/trechos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "T-UMIDO", "km": 12.5, "alturaVegetacao": 42.0,
                                 "diasSemRocada": 30, "tipoTerreno": "UMIDO", "idSensor": "IOT-T1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/trechos/\\d+$")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.tipoTerreno").value("UMIDO"))
                .andExpect(jsonPath("$.idSensor").value("IOT-T1"))
                .andExpect(jsonPath("$.monitoradoViaIoT").value(true))
                .andExpect(jsonPath("$.taxaCrescimentoDiaria").value(2.5))
                .andExpect(jsonPath("$.prioridade").value("CRITICA"));
    }

    @Test
    void getPorIdDevolve200EPorIdInexistente404() throws Exception {
        long id = criarTrechoSeco("T-GET", 18.0);

        mockMvc.perform(get("/api/trechos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("T-GET"))
                .andExpect(jsonPath("$.tipoTerreno").value("SECO"))
                .andExpect(jsonPath("$.idSensor").doesNotExist())
                .andExpect(jsonPath("$.prioridade").value("MEDIA"));

        mockMvc.perform(get("/api/trechos/{id}", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value("Trecho 999999 nao encontrado."));
    }

    @Test
    void postComDadosInvalidosDevolve400ComOsCampos() throws Exception {
        mockMvc.perform(post("/api/trechos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "", "km": -1, "alturaVegetacao": -5}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.codigo").exists())
                .andExpect(jsonPath("$.campos.km").value("nao pode ser negativo"))
                .andExpect(jsonPath("$.campos.alturaVegetacao").value("nao pode ser negativa"))
                .andExpect(jsonPath("$.campos.tipoTerreno").exists());
    }

    @Test
    void regrasDaSprint1FicamNoServiceEDevolvem400() throws Exception {
        // UMIDO sem sensor
        mockMvc.perform(post("/api/trechos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "T-SEM-SENSOR", "km": 1, "alturaVegetacao": 5, "tipoTerreno": "UMIDO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Um trecho UMIDO precisa de um sensor IoT instalado (idSensor)."));

        // SECO com sensor
        mockMvc.perform(post("/api/trechos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "T-SECO-SENSOR", "km": 1, "alturaVegetacao": 5,
                                 "tipoTerreno": "SECO", "idSensor": "IOT-X"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void codigoDuplicadoDevolve409() throws Exception {
        criarTrechoSeco("T-DUP", 10.0);

        mockMvc.perform(post("/api/trechos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "T-DUP", "km": 2, "alturaVegetacao": 3, "tipoTerreno": "SECO"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Ja existe um trecho com o codigo T-DUP."));
    }

    @Test
    void putAtualiza200E404SeNaoExistir() throws Exception {
        long id = criarTrechoUmido("T-PUT", 20.0, "IOT-OLD");
        String corpo = """
                {"codigo": "T-PUT", "km": 33.3, "alturaVegetacao": 26.0, "diasSemRocada": 7,
                 "tipoTerreno": "UMIDO", "idSensor": "IOT-NEW"}
                """;

        mockMvc.perform(put("/api/trechos/{id}", id).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.km").value(33.3))
                .andExpect(jsonPath("$.alturaVegetacao").value(26.0))
                .andExpect(jsonPath("$.idSensor").value("IOT-NEW"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"));

        mockMvc.perform(put("/api/trechos/{id}", 999_999).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isNotFound());
    }

    @Test
    void putNaoDeixaTrocarOTipoDeTerreno() throws Exception {
        long id = criarTrechoSeco("T-TIPO", 10.0);

        mockMvc.perform(put("/api/trechos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo": "T-TIPO", "km": 10, "alturaVegetacao": 10,
                                 "tipoTerreno": "UMIDO", "idSensor": "IOT-1"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteDevolve204EDepois404() throws Exception {
        long id = criarTrechoSeco("T-DEL", 10.0);

        mockMvc.perform(delete("/api/trechos/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/trechos/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/trechos/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteDeTrechoComEquipeDevolve409() throws Exception {
        long trecho = criarTrechoSeco("T-COM-EQUIPE", 45.0);
        criarEquipe("Equipe do Trecho", trecho);

        mockMvc.perform(delete("/api/trechos/{id}", trecho)).andExpect(status().isConflict());
    }

    @Test
    void filtrosUsamAsDerivedQueries() throws Exception {
        criarTrechoSeco("F-SECO-ALTO", 45.0);
        criarTrechoSeco("F-SECO-BAIXO", 5.0);
        criarTrechoUmido("F-UMIDO-ALTO", 41.0, "IOT-F1");
        criarTrechoUmido("F-UMIDO-MEDIO", 20.0, "IOT-F2");

        // findByAlturaVegetacaoGreaterThanEqual -> mais urgente primeiro
        mockMvc.perform(get("/api/trechos").param("alturaMinima", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].codigo").value("F-SECO-ALTO"))
                .andExpect(jsonPath("$[1].codigo").value("F-UMIDO-ALTO"));

        // findByTipoTerrenoOrderByKmAsc
        mockMvc.perform(get("/api/trechos").param("tipoTerreno", "UMIDO"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].tipoTerreno").value("UMIDO"));

        // findByTipoTerrenoAndAlturaVegetacaoGreaterThanEqual
        mockMvc.perform(get("/api/trechos").param("tipoTerreno", "SECO").param("alturaMinima", "40"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo").value("F-SECO-ALTO"));

        // limiar CRITICA vem do dominio (Prioridade), nao do controller
        mockMvc.perform(get("/api/trechos/criticos"))
                .andExpect(jsonPath("$", hasSize(2)));

        // findByCodigo
        mockMvc.perform(get("/api/trechos/codigo/{codigo}", "F-SECO-BAIXO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioridade").value("BAIXA"));
    }

    @Test
    void filtroComValoresInvalidosDevolve400() throws Exception {
        mockMvc.perform(get("/api/trechos").param("alturaMinima", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/trechos").param("tipoTerreno", "BARRO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("Valor 'BARRO' invalido para o parametro 'tipoTerreno' (valores aceitos: [SECO, UMIDO])."));
    }

    @Test
    void simularCrescimentoEPolimorfico() throws Exception {
        long umido = criarTrechoUmido("S-UMIDO", 10.0, "IOT-S");
        long seco = criarTrechoSeco("S-SECO", 10.0);

        mockMvc.perform(post("/api/trechos/{id}/simular-crescimento", umido).param("dias", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alturaVegetacao").value(35.0))   // 10 + 2.5 x 10
                .andExpect(jsonPath("$.diasSemRocada").value(10))
                .andExpect(jsonPath("$.prioridade").value("ALTA"));

        mockMvc.perform(post("/api/trechos/{id}/simular-crescimento", seco).param("dias", "10"))
                .andExpect(jsonPath("$.alturaVegetacao").value(18.0));  // 10 + 0.8 x 10

        mockMvc.perform(post("/api/trechos/{id}/simular-crescimento", seco).param("dias", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void idComTipoErradoDevolve400() throws Exception {
        mockMvc.perform(get("/api/trechos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Valor 'abc' invalido para o parametro 'id'."));
    }
}
