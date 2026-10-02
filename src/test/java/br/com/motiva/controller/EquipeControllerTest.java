package br.com.motiva.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EquipeControllerTest extends ApiTestBase {

    @Test
    void equipeAssociadaATrechoCriticoE201() throws Exception {
        long trecho = criarTrechoSeco("E-CRITICO", 45.0);

        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Equipe Norte", "trechoId": %d}
                                """.formatted(trecho)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Equipe Norte"))
                .andExpect(jsonPath("$.trechoResponsavel.id").value(trecho))
                .andExpect(jsonPath("$.trechoResponsavel.codigo").value("E-CRITICO"));
    }

    @Test
    void regraDaSprint1SoTrechoCriticoDevolve400() throws Exception {
        long trecho = criarTrechoSeco("E-NAO-CRITICO", 30.0);

        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Equipe Sul", "trechoId": %d}
                                """.formatted(trecho)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(
                        "A equipe so pode ser associada a um trecho CRITICO (>= 40 cm). "
                                + "O trecho E-NAO-CRITICO esta com 30.0 cm (ALTA)."));
    }

    @Test
    void trechoInexistenteDevolve404() throws Exception {
        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Equipe Fantasma", "trechoId": 999999}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void nomeObrigatorioENomeDuplicado() throws Exception {
        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.nome").value("e obrigatorio"));

        criarEquipe("Equipe Leste", null);
        mockMvc.perform(post("/api/equipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"EQUIPE LESTE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void putRenomeiaEDesassociaOTrecho() throws Exception {
        long trecho = criarTrechoSeco("E-PUT", 50.0);
        long equipe = criarEquipe("Equipe Oeste", trecho);

        mockMvc.perform(put("/api/equipes/{id}", equipe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Equipe Oeste - Plantao\", \"trechoId\": null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Equipe Oeste - Plantao"))
                .andExpect(jsonPath("$.trechoResponsavel").doesNotExist());

        mockMvc.perform(get("/api/equipes/disponiveis"))
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(equipe), hasSize(1)));
    }

    @Test
    void putMantendoOTrechoAtualNaoReaplicaARegraDoCritico() throws Exception {
        long trecho = criarTrechoSeco("E-ROCADO", 45.0);
        long equipe = criarEquipe("Equipe Centro", trecho);
        // o trecho e rocado depois da associacao e deixa de ser critico
        criar("/api/intervencoes", "{\"trechoId\": %d, \"tipo\": \"MECANIZADA\"}".formatted(trecho));

        mockMvc.perform(put("/api/equipes/{id}", equipe)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\": \"Equipe Centro\", \"trechoId\": %d}".formatted(trecho)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trechoResponsavel.alturaVegetacao").value(5.0));
    }

    @Test
    void filtroPorNomeEDelete() throws Exception {
        criarEquipe("Equipe Alfa", null);
        long beta = criarEquipe("Equipe Beta", null);

        mockMvc.perform(get("/api/equipes").param("nome", "bet"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nome").value("Equipe Beta"));

        mockMvc.perform(delete("/api/equipes/{id}", beta)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/equipes/{id}", beta)).andExpect(status().isNotFound());
    }

    @Test
    void naoApagaEquipeComIntervencoes() throws Exception {
        long trecho = criarTrechoSeco("E-HIST", 30.0);
        long equipe = criarEquipe("Equipe Historica", null);
        criar("/api/intervencoes", "{\"trechoId\": %d, \"equipeId\": %d}".formatted(trecho, equipe));

        mockMvc.perform(delete("/api/equipes/{id}", equipe)).andExpect(status().isConflict());
    }
}
