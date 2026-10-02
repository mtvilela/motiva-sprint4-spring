package br.com.motiva.controller;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RelatorioControllerTest extends ApiTestBase {

    // Alturas longe dos limiares: a leitura IoT (+-1 cm) nao muda a prioridade.
    private void cenarioComTodasAsPrioridades() throws Exception {
        criarTrechoSeco("R-CRITICO", 50.0);
        criarTrechoUmido("R-ALTO", 30.0, "IOT-R1");
        criarTrechoSeco("R-MEDIO", 20.0);
        criarTrechoUmido("R-BAIXO", 5.0, "IOT-R2");
    }

    @Test
    void postGeraOrdenaEPersisteOHistorico() throws Exception {
        cenarioComTodasAsPrioridades();

        mockMvc.perform(post("/api/relatorios"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/api/relatorios/\\d+$")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.qtCritica").value(1))
                .andExpect(jsonPath("$.qtAlta").value(1))
                .andExpect(jsonPath("$.qtMedia").value(1))
                .andExpect(jsonPath("$.qtBaixa").value(1))
                .andExpect(jsonPath("$.totalTrechos").value(4))      // coluna virtual lida via @Generated
                .andExpect(jsonPath("$.qtComIntervencao").value(3))
                .andExpect(jsonPath("$.itens", hasSize(4)))
                .andExpect(jsonPath("$.itens[0].prioridade").value("CRITICA"))
                .andExpect(jsonPath("$.itens[0].intervencaoRecomendada").value("Rocada Mecanizada"))
                .andExpect(jsonPath("$.itens[1].prioridade").value("ALTA"))
                .andExpect(jsonPath("$.itens[1].fonte").value("IoT"))
                .andExpect(jsonPath("$.itens[1].tipoIntervencao").value("PULVERIZACAO"))
                .andExpect(jsonPath("$.itens[3].prioridade").value("BAIXA"))
                .andExpect(jsonPath("$.itens[3].intervencaoRecomendada").value("Nenhuma (monitorar)"));

        // historico: GET lista (sem os itens, so cabecalho + resumo)
        mockMvc.perform(get("/api/relatorios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].totalTrechos").value(4))
                .andExpect(jsonPath("$[0].itens").doesNotExist())
                .andExpect(jsonPath("$[0].resumo").value(matchesPattern("^CRITICA R-CRITICO KM 10\\.0 50\\.0cm.*")));
    }

    @Test
    void consultaPorPeriodo() throws Exception {
        cenarioComTodasAsPrioridades();
        long id = criar("/api/relatorios", "");
        String hoje = LocalDate.now().toString();

        mockMvc.perform(get("/api/relatorios/periodo").param("inicio", hoje).param("fim", hoje))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));

        mockMvc.perform(get("/api/relatorios/periodo").param("inicio", "2000-01-01").param("fim", "2000-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/relatorios/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/relatorios/{id}", 999_999)).andExpect(status().isNotFound());
    }

    @Test
    void periodoInvalidoDevolve400() throws Exception {
        // inicio depois do fim
        mockMvc.perform(get("/api/relatorios/periodo").param("inicio", "2026-10-01").param("fim", "2026-09-01"))
                .andExpect(status().isBadRequest());
        // parametro obrigatorio ausente
        mockMvc.perform(get("/api/relatorios/periodo").param("inicio", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O parametro 'fim' e obrigatorio."));
        // data fora do formato ISO
        mockMvc.perform(get("/api/relatorios/periodo").param("inicio", "01/10/2026").param("fim", "2026-10-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void relatorioSemTrechosTemTotalZero() throws Exception {
        mockMvc.perform(post("/api/relatorios"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalTrechos").value(0))
                .andExpect(jsonPath("$.itens", hasSize(0)));
    }
}
