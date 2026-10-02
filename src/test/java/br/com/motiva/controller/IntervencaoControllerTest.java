package br.com.motiva.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IntervencaoControllerTest extends ApiTestBase {

    @Test
    void semTipoOMotorDeRegrasEscolheEOTrechoEAtualizado() throws Exception {
        long trecho = criarTrechoSeco("I-CRITICO", 48.0);
        long equipe = criarEquipe("Equipe I", null);

        // CRITICA -> Rocada Mecanizada (regra 2 da Sprint 2)
        mockMvc.perform(post("/api/intervencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trechoId\": %d, \"equipeId\": %d}".formatted(trecho, equipe)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("MECANIZADA"))
                .andExpect(jsonPath("$.servico").value("Rocada Mecanizada"))
                .andExpect(jsonPath("$.alturaAntes").value(48.0))
                .andExpect(jsonPath("$.alturaDepois").value(5.0))
                .andExpect(jsonPath("$.equipe.nome").value("Equipe I"))
                .andExpect(jsonPath("$.descricao").value(startsWith("[Rocada Mecanizada] KM 10.0")));

        // dirty checking: a nova altura do trecho foi gravada sem save() explicito
        mockMvc.perform(get("/api/trechos/{id}", trecho))
                .andExpect(jsonPath("$.alturaVegetacao").value(5.0))
                .andExpect(jsonPath("$.diasSemRocada").value(0))
                .andExpect(jsonPath("$.prioridade").value("BAIXA"));
    }

    @Test
    void trechoUmidoAltoRecebePulverizacao() throws Exception {
        long trecho = criarTrechoUmido("I-UMIDO", 30.0, "IOT-I");

        mockMvc.perform(post("/api/intervencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trechoId\": %d}".formatted(trecho)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("PULVERIZACAO"))
                .andExpect(jsonPath("$.alturaDepois").value(10.0))
                .andExpect(jsonPath("$.equipe").doesNotExist());
    }

    @Test
    void trechoBaixoSemTipoDevolve400MasComTipoInformadoRegistra() throws Exception {
        long trecho = criarTrechoSeco("I-BAIXO", 9.0);
        String semTipo = "{\"trechoId\": %d}".formatted(trecho);

        mockMvc.perform(post("/api/intervencoes").contentType(MediaType.APPLICATION_JSON).content(semTipo))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/intervencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trechoId\": %d, \"tipo\": \"MANUAL\"}".formatted(trecho)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alturaDepois").value(8.0));
    }

    @Test
    void trechoOuEquipeInexistenteDevolve404ESemTrecho400() throws Exception {
        mockMvc.perform(post("/api/intervencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trechoId\": 999999, \"tipo\": \"MANUAL\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/intervencoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\": \"MANUAL\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.trechoId").value("e obrigatorio"));
    }

    @Test
    void putCorrigeORegistroE400SeAlturaDepoisMaior() throws Exception {
        long trecho = criarTrechoSeco("I-PUT", 30.0);
        long intervencao = criar("/api/intervencoes", "{\"trechoId\": %d}".formatted(trecho));

        mockMvc.perform(put("/api/intervencoes/{id}", intervencao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "MECANIZADA", "trechoId": %d, "dataExecucao": "2026-09-30T08:00:00",
                                 "alturaAntes": 30.0, "alturaDepois": 5.0, "descricao": "Conferido em campo."}
                                """.formatted(trecho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataExecucao").value("2026-09-30T08:00:00"))
                .andExpect(jsonPath("$.descricao").value("Conferido em campo."));

        mockMvc.perform(put("/api/intervencoes/{id}", intervencao)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "MANUAL", "trechoId": %d, "dataExecucao": "2026-09-30T08:00:00",
                                 "alturaAntes": 10.0, "alturaDepois": 20.0}
                                """.formatted(trecho)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("A altura depois da intervencao nao pode ser maior que a altura antes."));

        mockMvc.perform(put("/api/intervencoes/{id}", 999_999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo": "MANUAL", "trechoId": %d, "dataExecucao": "2026-09-30T08:00:00",
                                 "alturaAntes": 10.0, "alturaDepois": 5.0}
                                """.formatted(trecho)))
                .andExpect(status().isNotFound());
    }

    @Test
    void filtroPorTipoUsaFindByTipo() throws Exception {
        long critico = criarTrechoSeco("I-F1", 45.0);
        long medio = criarTrechoSeco("I-F2", 18.0);
        criar("/api/intervencoes", "{\"trechoId\": %d}".formatted(critico)); // MECANIZADA
        criar("/api/intervencoes", "{\"trechoId\": %d}".formatted(medio));   // MANUAL

        mockMvc.perform(get("/api/intervencoes").param("tipo", "MANUAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trecho.codigo").value("I-F2"));

        mockMvc.perform(get("/api/trechos/{id}/intervencoes", critico))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].tipo").value("MECANIZADA"));
    }

    @Test
    void deleteDevolve204EDepois404() throws Exception {
        long trecho = criarTrechoSeco("I-DEL", 20.0);
        long intervencao = criar("/api/intervencoes", "{\"trechoId\": %d}".formatted(trecho));

        // enquanto existir a intervencao, o trecho nao pode ser apagado (FK)
        mockMvc.perform(delete("/api/trechos/{id}", trecho)).andExpect(status().isConflict());

        mockMvc.perform(delete("/api/intervencoes/{id}", intervencao)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/intervencoes/{id}", intervencao)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/trechos/{id}", trecho)).andExpect(status().isNoContent());
    }
}
