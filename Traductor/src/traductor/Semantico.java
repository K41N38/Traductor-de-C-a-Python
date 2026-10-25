package traductor;
import java.util.*;

/**
 * RECONSTRUIDA a partir del uso que hace Analizador.jj:
 *   Semantico.limpiar(), Semantico.declararVariable(nombre, tipo),
 *   Semantico.validarOperacionAritmetica(tipo1, tipo2, operador),
 *   Semantico.hayErrores(), Semantico.imprimirErrores(), Semantico.error(msg)
 * Si ya tienes tu propia versión original, reemplaza este archivo por el tuyo
 * (revisa que los nombres de método coincidan con lo que llama tu .jj real).
 */
public class Semantico {

    private static List<String> errores = new ArrayList<>();
    private static Map<String, String> tablaVariables = new HashMap<>();

    public static void limpiar() {
        errores.clear();
        tablaVariables.clear();
    }

    public static void declararVariable(String nombre, String tipo) {
        if (tablaVariables.containsKey(nombre)) {
            errores.add("Variable '" + nombre + "' ya fue declarada.");
        } else {
            tablaVariables.put(nombre, tipo);
        }
    }
    
    public static String obtenerTipoVariable(String nombre) {
        return tablaVariables.get(nombre);
    }
    
    /**
     * Validación mínima: no permite mezclar cadenas con operadores aritméticos
     * distintos de '+' (concatenación). Ajusta las reglas según lo que
     * necesites para tu proyecto.
     */
    public static void validarOperacionAritmetica(String tipo1, String tipo2, String operador) {
        boolean hayCadena = "cadena".equals(tipo1) || "cadena".equals(tipo2);
        if (hayCadena && !"+".equals(operador)) {
            errores.add("Operación '" + operador + "' inválida entre tipos '" + tipo1 + "' y '" + tipo2 + "'.");
        }
    }

    public static void error(String mensaje) {
        errores.add(mensaje);
    }

    public static boolean hayErrores() {
        return !errores.isEmpty();
    }

    public static void imprimirErrores() {
        for (String e : errores) {
            System.out.println("  ✗ " + e);
        }
    }
    
    /**
     * Valida el uso de return dentro del árbol sintáctico.
     * Por ahora, el traductor trabaja con fragmentos de código
     * y no se ha confirmado soporte para funciones o métodos.
     */
    public static void validarPrograma(Nodo raiz) {
        validarRetornos(raiz, false);
    }

    /**
     * Recorre recursivamente el AST para detectar return fuera
     * de una función o método.
     */
    private static void validarRetornos(Nodo nodo, boolean dentroFuncion) {
        if (nodo == null) {
            return;
        }

        if ("returnInstruccion".equals(nodo.tipo) && !dentroFuncion) {
            error(
                "La instrucción 'return' aparece fuera de una función "
                + "o método. No se puede traducir como código independiente."
            );
        }

        for (Nodo hijo : nodo.hijos) {
            validarRetornos(hijo, dentroFuncion);
        }
    }
}
