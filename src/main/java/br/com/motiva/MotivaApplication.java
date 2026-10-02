package br.com.motiva;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Ponto de entrada da Sprint 4. Substitui o antigo main.Main da Sprint 3:
// em vez de um roteiro no console, sobe um servidor HTTP (Tomcat embutido)
// com a API REST em http://localhost:8080/api/...
//
// @SpringBootApplication liga a auto-configuracao e o component scan a partir
// deste pacote (br.com.motiva): controllers, services e repositories sao
// encontrados e injetados automaticamente.
@SpringBootApplication
public class MotivaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MotivaApplication.class, args);
    }
}
