package br.com.motiva.model;

// Tipos de terreno de um trecho. O nome de cada constante e exatamente o
// valor gravado na coluna TIPO_TERRENO, que no JPA e o DISCRIMINADOR da
// heranca: SECO -> TrechoSeco, UMIDO -> TrechoUmido.
public enum TipoTerreno {
    SECO,
    UMIDO
}
