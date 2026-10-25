package traductor;
import java.util.*;

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
     * Ejecuta las validaciones semánticas del AST.
     */
    public static void validarPrograma(Nodo raiz) {
        validarRetornos(raiz, false);

        validarVariablesNoDeclaradas(
            raiz,
            new HashSet<String>(),
            new HashSet<String>()
        );

        validarTipos(raiz);

        validarInicializacion(
            raiz,
            new HashSet<String>()
        );
    }

    /**
     * Detecta return fuera de una función o método.
     */
    private static void validarRetornos(
            Nodo nodo,
            boolean dentroFuncion) {

        if (nodo == null) {
            return;
        }

        if ("returnInstruccion".equals(nodo.tipo)
                && !dentroFuncion) {

            error(
                "La instrucción 'return' aparece fuera de una función "
                + "o método. No se puede traducir como código independiente."
            );
        }

        for (Nodo hijo : nodo.hijos) {
            validarRetornos(hijo, dentroFuncion);
        }
    }

    /**
     * Detecta identificadores que no aparecen en la tabla de variables
     * y que tampoco son variables locales de foreach o catch.
     */
    private static void validarVariablesNoDeclaradas(
            Nodo nodo,
            Set<String> variablesLocales,
            Set<String> erroresReportados) {

        if (nodo == null) {
            return;
        }

        // Las variables declaradas durante el análisis sintáctico
        // ya están registradas en tablaVariables.
        comprobarVariable(
            nodo,
            variablesLocales,
            erroresReportados
        );

        // Una variable de foreach existe dentro de su cuerpo.
        if ("cicloForeach".equals(nodo.tipo)) {

            comprobarNombre(
                nodo.atributo("coleccion"),
                variablesLocales,
                erroresReportados
            );

            Set<String> localesCiclo =
                new HashSet<String>(variablesLocales);

            if (nodo.valor != null && !nodo.valor.isEmpty()) {
                localesCiclo.add(nodo.valor);
            }

            for (Nodo hijo : nodo.hijos) {
                validarVariablesNoDeclaradas(
                    hijo,
                    localesCiclo,
                    erroresReportados
                );
            }

            return;
        }

        // La variable de catch solo está disponible dentro de su bloque.
        if ("clausulaCatch".equals(nodo.tipo)) {

            Set<String> localesCatch =
                new HashSet<String>(variablesLocales);

            String variableExcepcion =
                nodo.atributo("variableExcepcion");

            if (variableExcepcion != null
                    && !variableExcepcion.isEmpty()) {
                localesCatch.add(variableExcepcion);
            }

            for (Nodo hijo : nodo.hijos) {
                validarVariablesNoDeclaradas(
                    hijo,
                    localesCatch,
                    erroresReportados
                );
            }

            return;
        }

        for (Nodo hijo : nodo.hijos) {
            validarVariablesNoDeclaradas(
                hijo,
                variablesLocales,
                erroresReportados
            );
        }
    }

    /**
     * Comprueba los identificadores según el tipo de nodo del AST.
     */
    private static void comprobarVariable(
            Nodo nodo,
            Set<String> variablesLocales,
            Set<String> erroresReportados) {

        if ("valor".equals(nodo.tipo)
                && "identificador".equals(
                    nodo.atributo("tipoValor"))) {

            comprobarNombre(
                nodo.valor,
                variablesLocales,
                erroresReportados
            );
        }

        if ("asignacion".equals(nodo.tipo)
                || "asignacionSimple".equals(nodo.tipo)
                || "incrementoDecremento".equals(nodo.tipo)) {

            comprobarNombre(
                nodo.valor,
                variablesLocales,
                erroresReportados
            );
        }

        if ("propiedadArreglo".equals(nodo.tipo)
                || "propiedadExcepcion".equals(nodo.tipo)
                || "operacionArreglo".equals(nodo.tipo)
                || "busquedaIndiceArreglo".equals(nodo.tipo)) {

            comprobarNombre(
                nodo.valor,
                variablesLocales,
                erroresReportados
            );
        }

        if ("copiarArreglo".equals(nodo.tipo)) {

            comprobarNombre(
                nodo.atributo("origen"),
                variablesLocales,
                erroresReportados
            );

            comprobarNombre(
                nodo.atributo("destino"),
                variablesLocales,
                erroresReportados
            );
        }
    }

    /**
     * Informa una sola vez por cada nombre de variable inexistente.
     */
    private static void comprobarNombre(
            String nombre,
            Set<String> variablesLocales,
            Set<String> erroresReportados) {

        if (nombre == null || nombre.isEmpty()) {
            return;
        }

        if (variablesLocales.contains(nombre)) {
            return;
        }

        if (obtenerTipoVariable(nombre) == null
                && erroresReportados.add(nombre)) {

            error(
                "La variable '" + nombre
                + "' se utiliza, pero no ha sido declarada."
            );
        }
    }
    
    /**
    * Valida que los elementos de un arreglo sean compatibles
    * con el tipo de dato declarado.
    */
   private static void validarElementosArreglo(
           String tipoEsperado, Nodo arreglo, String nombreVariable) {

       if (tipoEsperado == null || arreglo == null
               || !"arreglo".equals(arreglo.tipo)
               || !tipoEsperado.endsWith("[]")) {
           return;
       }

       // Obtiene el tipo de los elementos, por ejemplo:
       // int[] -> int; string[] -> string.
       String tipoElemento = tipoEsperado.substring(
           0, tipoEsperado.length() - 2
       );

       for (int i = 0; i < arreglo.hijos.size(); i++) {
           Nodo elemento = arreglo.hijo(i);
           String tipoRecibido = inferirTipo(elemento);

           if (!tiposCompatibles(tipoElemento, tipoRecibido)) {
               error(
                   "El elemento " + (i + 1) + " del arreglo '"
                   + nombreVariable + "' es de tipo '"
                   + tipoRecibido + "', pero se esperaba '"
                   + tipoElemento + "'."
               );
           }
       }
   }
    
    /**
     * Valida los tipos de las declaraciones y asignaciones.
     */
    private static void validarTipos(Nodo nodo) {
        if (nodo == null) {
            return;
        }

        // Declaración de una variable.
        if ("declaracionVariable".equals(nodo.tipo)) {
            String tipoEsperado = nodo.atributo("tipoDato");
            Nodo inicializador = nodo.hijo(0);

            if (tipoEsperado != null && inicializador != null) {
                String tipoRecibido = inferirTipo(inicializador);

                if (!tiposCompatibles(tipoEsperado, tipoRecibido)) {
                    error("La variable '" + nodo.valor
                        + "' es de tipo '" + tipoEsperado
                        + "', pero recibe un valor de tipo '"
                        + tipoRecibido + "'.");
                } else if (tipoEsperado.endsWith("[]")
                        && "arreglo".equals(tipoRecibido)) {

                    validarElementosArreglo(
                        tipoEsperado, inicializador, nodo.valor
                    );
                }
            }
        }

        // Asignación normal: variable = expresión.
        if ("asignacion".equals(nodo.tipo)
                || "asignacionSimple".equals(nodo.tipo)) {

            String operador = nodo.atributo("operadorAsignacion");

            if ("=".equals(operador)) {
                String tipoEsperado = obtenerTipoVariable(nodo.valor);

                // En asignaciones a arreglos, el primer hijo
                // es el índice y el último es el valor asignado.
                if (nodo.hijos != null && !nodo.hijos.isEmpty()) {
                    Nodo valorAsignado =
                        nodo.hijo(nodo.hijos.size() - 1);

                    String tipoRecibido = inferirTipo(valorAsignado);

                    if (tipoEsperado != null) {
                        if (tipoEsperado.endsWith("[]")
                                && "arreglo".equals(tipoRecibido)) {

                            validarElementosArreglo(
                                tipoEsperado, valorAsignado, nodo.valor
                            );

                        } else if (!tipoEsperado.endsWith("[]")
                                && !tiposCompatibles(
                                    tipoEsperado, tipoRecibido)) {

                            error("No se puede asignar un valor de tipo '"
                                + tipoRecibido + "' a la variable '"
                                + nodo.valor + "' de tipo '"
                                + tipoEsperado + "'.");
                        }
                    }
                }
            }
        }

        for (Nodo hijo : nodo.hijos) {
            validarTipos(hijo);
        }
    }

    /**
     * Intenta determinar el tipo de una expresión del AST.
     */
    private static String inferirTipo(Nodo nodo) {
        if (nodo == null) {
            return "desconocido";
        }

        if ("valor".equals(nodo.tipo)) {
            String tipoValor = nodo.atributo("tipoValor");

            if ("numero".equals(tipoValor)) return "int";
            if ("decimal".equals(tipoValor)) return "double";
            if ("cadena".equals(tipoValor)) return "string";
            if ("true".equals(tipoValor)
                    || "false".equals(tipoValor)) return "boolean";
            if ("null".equals(tipoValor)) return "null";

            if ("identificador".equals(tipoValor)) {
                String tipo = obtenerTipoVariable(nodo.valor);
                return tipo == null ? "desconocido" : tipo;
            }
        }

        if ("lecturaConsola".equals(nodo.tipo)) {
            return "string";
        }

        if ("propiedadArreglo".equals(nodo.tipo)) {
            return "int";
        }

        if ("accesoArreglo".equals(nodo.tipo)) {
            String tipo = obtenerTipoVariable(nodo.valor);

            if (tipo != null && tipo.endsWith("[]")) {
                return tipo.substring(0, tipo.length() - 2);
            }
        }

        if ("arreglo".equals(nodo.tipo)) {
            return "arreglo";
        }

        if ("expresionLogica".equals(nodo.tipo)) {
            return "boolean";
        }

        if ("agrupacion".equals(nodo.tipo)) {
            return inferirTipo(nodo.hijo(0));
        }

        // Para operaciones aritméticas, intenta deducir el tipo
        // a partir de los operandos.
        if (nodo.atributo("operador") != null
                && nodo.hijos.size() >= 2) {

            String operador = nodo.atributo("operador");
            String tipo1 = inferirTipo(nodo.hijo(0));
            String tipo2 = inferirTipo(nodo.hijo(1));

            if ("+".equals(operador)
                    && ("string".equals(tipo1)
                        || "string".equals(tipo2))) {
                return "string";
            }

            if (esNumerico(tipo1) && esNumerico(tipo2)) {
                if ("double".equals(tipo1)
                        || "double".equals(tipo2)
                        || "float".equals(tipo1)
                        || "float".equals(tipo2)) {
                    return "double";
                }

                if ("long".equals(tipo1)
                        || "long".equals(tipo2)) {
                    return "long";
                }

                return "int";
            }
        }

        // Para nodos de expresión con un solo hijo.
        if (nodo.hijos.size() == 1) {
            return inferirTipo(nodo.hijo(0));
        }

        return "desconocido";
    }

    /**
     * Determina si un tipo es numérico.
     */
    private static boolean esNumerico(String tipo) {
        return "byte".equals(tipo)
            || "short".equals(tipo)
            || "int".equals(tipo)
            || "long".equals(tipo)
            || "float".equals(tipo)
            || "double".equals(tipo)
            || "decimal".equals(tipo);
    }

    /**
     * Comprueba si el tipo recibido puede asignarse al esperado.
     */
    private static boolean tiposCompatibles(
            String esperado, String recibido) {

        esperado = normalizarTipo(esperado);
        recibido = normalizarTipo(recibido);

        if ("desconocido".equals(recibido)) {
            return true;
        }

        if (esperado.equals(recibido)) {
            return true;
        }

        // Permitir arreglos literales en variables de tipo arreglo.
        if (esperado.endsWith("[]") && "arreglo".equals(recibido)) {
            return true;
        }

        if ("null".equals(recibido)) {
            return esperado.endsWith("[]")
                || "string".equals(esperado);
        }

        if (esNumerico(esperado) && esNumerico(recibido)) {
            // Se permiten conversiones numéricas habituales
            // de tipos pequeños a tipos de mayor capacidad.
            if ("double".equals(esperado)
                    || "float".equals(esperado)) {
                return true;
            }

            if ("long".equals(esperado)) {
                return !"double".equals(recibido)
                    && !"float".equals(recibido)
                    && !"decimal".equals(recibido);
            }

            if ("int".equals(esperado)) {
                return "byte".equals(recibido)
                    || "short".equals(recibido);
            }
        }

        return false;
    }

    /**
     * Normaliza algunos nombres de tipos.
     */
    private static String normalizarTipo(String tipo) {
        if (tipo == null) {
            return "desconocido";
        }

        switch (tipo.toLowerCase(Locale.ROOT)) {
            case "bool":
                return "boolean";
            case "cadena":
                return "string";
            default:
                return tipo.toLowerCase(Locale.ROOT);
        }
    }
    
    /**
     * Detecta variables utilizadas antes de recibir una asignación.
     */
    private static void validarInicializacion(
            Nodo nodo, Set<String> inicializadas) {

        if (nodo == null) {
            return;
        }

        // El programa y los bloques ejecutan sus instrucciones
        // en orden, por lo que se conserva el estado entre ellas.
        if ("programa".equals(nodo.tipo)
                || "bloque".equals(nodo.tipo)
                || "declaracionesMultiples".equals(nodo.tipo)) {

            for (Nodo hijo : nodo.hijos) {
                validarInicializacion(hijo, inicializadas);
            }
            return;
        }

        // Una declaración con inicializador queda inicializada.
        if ("declaracionVariable".equals(nodo.tipo)) {
            if (!nodo.hijos.isEmpty()) {
                validarUsosExpresion(
                    nodo.hijo(0), inicializadas
                );

                if (nodo.valor != null) {
                    inicializadas.add(nodo.valor);
                }
            } else if (nodo.valor != null) {
                inicializadas.remove(nodo.valor);
            }
            return;
        }

        // Una asignación simple inicializa la variable.
        if ("asignacion".equals(nodo.tipo)
                || "asignacionSimple".equals(nodo.tipo)) {

            String operador = nodo.atributoPorDefecto(
                "operadorAsignacion", "="
            );

            boolean esArreglo =
                "true".equals(nodo.atributo("asignacionArreglo"));

            // El índice y el arreglo se utilizan antes de asignar.
            if (esArreglo) {
                comprobarUsoInicializado(
                    nodo.valor, inicializadas
                );

                validarUsosExpresion(
                    nodo.hijo(0), inicializadas
                );

                validarUsosExpresion(
                    nodo.hijo(1), inicializadas
                );
                return;
            }

            // Los operadores compuestos leen el valor anterior.
            if (!"=".equals(operador)) {
                comprobarUsoInicializado(
                    nodo.valor, inicializadas
                );
            }

            // Primero se comprueba el lado derecho.
            if (!nodo.hijos.isEmpty()) {
                validarUsosExpresion(
                    nodo.hijo(0), inicializadas
                );
            }

            // Después de una asignación válida, ya tiene valor.
            if (nodo.valor != null) {
                inicializadas.add(nodo.valor);
            }
            return;
        }

        // Incrementar o decrementar requiere un valor previo.
        if ("incrementoDecremento".equals(nodo.tipo)) {
            comprobarUsoInicializado(
                nodo.valor, inicializadas
            );
            return;
        }

        // Las condiciones se evalúan y sus ramas se analizan
        // por separado para combinar correctamente sus resultados.
        if ("condicion".equals(nodo.tipo)) {
            validarUsosExpresion(
                nodo.hijo(0), inicializadas
            );

            Set<String> estadoIf =
                new HashSet<String>(inicializadas);

            validarInicializacion(
                nodo.hijo(1), estadoIf
            );

            if ("true".equals(nodo.atributo("tieneElse"))) {
                Set<String> estadoElse =
                    new HashSet<String>(inicializadas);

                validarInicializacion(
                    nodo.hijo(2), estadoElse
                );

                // Una variable queda inicializada después del if/else
                // solamente si lo está en ambas ramas.
                estadoIf.retainAll(estadoElse);

                inicializadas.clear();
                inicializadas.addAll(estadoIf);
            }

            // Sin else, se conserva el estado anterior:
            // la rama if podría no ejecutarse.
            return;
        }

        // En los ciclos, el cuerpo puede no ejecutarse.
        if ("cicloMientras".equals(nodo.tipo)
                || "cicloPara".equals(nodo.tipo)
                || "cicloDoWhile".equals(nodo.tipo)
                || "cicloForeach".equals(nodo.tipo)) {

            if ("cicloPara".equals(nodo.tipo)) {
                // La inicialización del for sí se ejecuta primero.
                validarInicializacion(
                    nodo.hijo(0), inicializadas
                );

                validarUsosExpresion(
                    nodo.hijo(1), inicializadas
                );

                Set<String> estadoCiclo =
                    new HashSet<String>(inicializadas);

                validarInicializacion(
                    nodo.hijo(3), estadoCiclo
                );

                validarUsosExpresion(
                    nodo.hijo(2), estadoCiclo
                );
                return;
            }

            if ("cicloMientras".equals(nodo.tipo)) {
                validarUsosExpresion(
                    nodo.hijo(0), inicializadas
                );
                validarInicializacion(
                    nodo.hijo(1),
                    new HashSet<String>(inicializadas)
                );
                return;
            }

            if ("cicloDoWhile".equals(nodo.tipo)) {
                Set<String> estadoCiclo =
                    new HashSet<String>(inicializadas);

                // El cuerpo se ejecuta al menos una vez.
                validarInicializacion(
                    nodo.hijo(0), estadoCiclo
                );

                // La condición se evalúa después del cuerpo.
                validarUsosExpresion(
                    nodo.hijo(1), estadoCiclo
                );

                // Conserva las variables inicializadas por el cuerpo.
                inicializadas.clear();
                inicializadas.addAll(estadoCiclo);

                return;
            }

            if ("cicloForeach".equals(nodo.tipo)) {
                validarUsosExpresion(
                    new Nodo("valor", nodo.atributo("coleccion"))
                        .atributo("tipoValor", "identificador"),
                    inicializadas
                );

                Set<String> estadoCiclo =
                    new HashSet<String>(inicializadas);

                if (nodo.valor != null) {
                    estadoCiclo.add(nodo.valor);
                }

                validarInicializacion(
                    nodo.hijo(0), estadoCiclo
                );
                return;
            }
        }

        // Para otras estructuras, se revisan sus expresiones
        // y sus bloques sin propagar asignaciones inciertas.
        for (Nodo hijo : nodo.hijos) {
            if ("bloque".equals(hijo.tipo)
                    || "clausulaCatch".equals(hijo.tipo)
                    || "casoSwitch".equals(hijo.tipo)
                    || "casoDefault".equals(hijo.tipo)) {

                validarInicializacion(
                    hijo,
                    new HashSet<String>(inicializadas)
                );
            } else {
                validarUsosExpresion(hijo, inicializadas);
            }
        }
    }

    /**
     * Revisa los identificadores que se leen en una expresión.
     */
    private static void validarUsosExpresion(
            Nodo nodo, Set<String> inicializadas) {

        if (nodo == null) {
            return;
        }

        if ("valor".equals(nodo.tipo)
                && "identificador".equals(
                    nodo.atributo("tipoValor"))) {

            comprobarUsoInicializado(
                nodo.valor, inicializadas
            );
            return;
        }

        if ("accesoArreglo".equals(nodo.tipo)
                || "propiedadArreglo".equals(nodo.tipo)
                || "propiedadExcepcion".equals(nodo.tipo)
                || "operacionArreglo".equals(nodo.tipo)
                || "busquedaIndiceArreglo".equals(nodo.tipo)) {

            comprobarUsoInicializado(
                nodo.valor, inicializadas
            );
        }

        if ("copiarArreglo".equals(nodo.tipo)) {
            comprobarUsoInicializado(
                nodo.atributo("origen"), inicializadas
            );
            comprobarUsoInicializado(
                nodo.atributo("destino"), inicializadas
            );
        }

        for (Nodo hijo : nodo.hijos) {
            validarUsosExpresion(hijo, inicializadas);
        }
    }

    /**
     * Informa si una variable se lee antes de ser inicializada.
     */
    private static void comprobarUsoInicializado(
            String nombre, Set<String> inicializadas) {

        if (nombre == null || nombre.isEmpty()) {
            return;
        }

        if (obtenerTipoVariable(nombre) == null) {
            return;
        }

        if (!inicializadas.contains(nombre)) {
            error(
                "La variable '" + nombre
                + "' se utiliza antes de recibir un valor."
            );
        }
    }
}