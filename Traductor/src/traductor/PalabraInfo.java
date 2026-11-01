package traductor;

public class PalabraInfo {
    public String lexema;
    public String token;
    public String expresion;
    public String lenguaje;
    public String clasificacion;
    public String funcion;

    public PalabraInfo(String lexema, String token, String expresion,
                        String lenguaje, String clasificacion, String funcion) {
        this.lexema = lexema;
        this.token = token;
        this.expresion = expresion;
        this.lenguaje = lenguaje;
        this.clasificacion = clasificacion;
        this.funcion = funcion;
    }
}