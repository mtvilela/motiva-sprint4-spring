package br.com.motiva.model;

// Niveis de prioridade de rocada, do mais urgente para o menos urgente
// (os limiares da nossa Sprint 2). A ordem do enum (ordinal) e usada para
// ordenar o relatorio.
//
// O enum so GUARDA os limiares (altura minima em cm que dispara cada nivel).
// Quem APLICA a classificacao e o MotorDeRegras (@Service): nenhum
// controller conhece esses numeros.
public enum Prioridade {
    CRITICA(40.0),
    ALTA(25.0),
    MEDIA(15.0),
    BAIXA(0.0);

    private final double alturaMinimaCm;

    Prioridade(double alturaMinimaCm) {
        this.alturaMinimaCm = alturaMinimaCm;
    }

    public double getAlturaMinimaCm() {
        return alturaMinimaCm;
    }
}
