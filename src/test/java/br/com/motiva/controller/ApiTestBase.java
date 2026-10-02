package br.com.motiva.controller;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Base dos testes de API (Aula 15): sobe a aplicacao INTEIRA (@SpringBootTest)
// e dispara requisicoes HTTP simuladas com MockMvc, passando por Controller ->
// Service -> Repository -> banco H2 em memoria (perfil "test").
//
// @Transactional: cada teste roda numa transacao desfeita no final (rollback),
// entao um teste nunca enxerga os dados de outro.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
abstract class ApiTestBase {

    @Autowired
    protected MockMvc mockMvc;

    protected long criarTrechoSeco(String codigo, double altura) throws Exception {
        return criar("/api/trechos", """
                {"codigo": "%s", "km": 10.0, "alturaVegetacao": %s, "tipoTerreno": "SECO"}
                """.formatted(codigo, altura));
    }

    protected long criarTrechoUmido(String codigo, double altura, String sensor) throws Exception {
        return criar("/api/trechos", """
                {"codigo": "%s", "km": 20.0, "alturaVegetacao": %s, "tipoTerreno": "UMIDO", "idSensor": "%s"}
                """.formatted(codigo, altura, sensor));
    }

    protected long criarEquipe(String nome, Long trechoId) throws Exception {
        return criar("/api/equipes", """
                {"nome": "%s", "trechoId": %s}
                """.formatted(nome, trechoId));
    }

    // POST que precisa dar 201; devolve o id do recurso criado.
    protected long criar(String url, String json) throws Exception {
        MvcResult resultado = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return idDe(resultado);
    }

    protected static long idDe(MvcResult resultado) throws Exception {
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
