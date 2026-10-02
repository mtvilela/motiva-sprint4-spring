package br.com.motiva.model;

// INTERFACE: contrato de comportamento desacoplado da hierarquia de trechos.
// Quem possui tecnologia instalada consegue transmitir dados de sensor
// sem precisar de inspecao visual em campo.
//
// Mantida ENXUTA (Interface Segregation Principle): so expoe o que diz
// respeito a telemetria IoT, nada de regras de rocada ou crescimento.
public interface MonitoravelViaIoT {

    // Le o sensor instalado e devolve a altura da vegetacao medida (cm).
    double transmitirDadosSensor();

    // Identificador do sensor que enviou os dados.
    String getIdSensor();
}
