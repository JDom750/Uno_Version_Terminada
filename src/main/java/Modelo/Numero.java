package Modelo;

public    enum Numero{
    CERO,UNO,DOS,TRES,CUATRO,CINCO,SEIS,SIETE,OCHO,NUEVE,MASDOS,SALTARSE,CAMBIOSENTIDO,CAMBIOCOLOR,MASCUATRO;

    private static final Numero[] numeros = Numero.values();
    public static Numero getNumero(int i){
        return Numero.numeros[i];
    }
}
