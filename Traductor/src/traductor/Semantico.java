package traductor;
import java.util.*;

public class Semantico {

    private static List<String> errores = new ArrayList<>();

    private static Map<String, String> tablaVariables = new LinkedHashMap<>();

    // Información de las funciones declaradas en el programa.
    private static Map<String, FuncionInfo> tablaFunciones = new LinkedHashMap<>();

    // Ámbitos independientes para las variables de cada función.
    private static Map<String, AmbitoFuncion> tablaAmbitosFunciones =
            new LinkedHashMap<>();

    // Representa las variables y parámetros de una función.
    private static class AmbitoFuncion {
        String nombreFuncion;
        Map<String, String> variables = new LinkedHashMap<>();

        AmbitoFuncion(String nombreFuncion) {
            this.nombreFuncion = nombreFuncion;
        }
    }

    // Información de una función.
    private static class FuncionInfo {
        String nombre;
        String tipoRetorno;
        List<String> nombresParametros = new ArrayList<>();
        List<String> tiposParametros = new ArrayList<>();

        FuncionInfo(String nombre, String tipoRetorno) {
            this.nombre = nombre;
            this.tipoRetorno = normalizarTipo(tipoRetorno);
        }
    }

    public static void limpiar() {
        errores.clear();
        tablaVariables.clear();
        tablaFunciones.clear();
        tablaAmbitosFunciones.clear();
    }
    
    private static void registrarFunciones(Nodo nodo) {
        if (nodo == null) {
            return;
        }

        if ("funcion".equals(nodo.tipo)) {
            String nombre = nodo.valor;
            String tipoRetorno = nodo.atributoPorDefecto("tipoRetorno", "void");

            if (nombre == null || nombre.trim().isEmpty()) {
                errores.add("Se encontró una función sin nombre.");
                return;
            }

            if (tablaFunciones.containsKey(nombre)) {
                errores.add("La función '" + nombre + "' ya fue declarada.");
                return;
            }

            FuncionInfo info = new FuncionInfo(nombre, tipoRetorno);

            // Todos los hijos, excepto el último, son parámetros.
            int cantidadParametros = Math.max(0, nodo.hijos.size() - 1);

            for (int i = 0; i < cantidadParametros; i++) {
                Nodo parametro = nodo.hijos.get(i);

                if (!"parametro".equals(parametro.tipo)) {
                    continue;
                }

                info.nombresParametros.add(parametro.valor);
                info.tiposParametros.add(
                    normalizarTipo(parametro.atributoPorDefecto("tipoDato", "desconocido"))
                );
            }

            tablaFunciones.put(nombre, info);
        }

        for (Nodo hijo : nodo.hijos) {
            registrarFunciones(hijo);
        }
    }

    /**
     * Construye las tablas de variables globales y locales
     * recorriendo el árbol de sintaxis abstracta.
     */
    private static void registrarAmbitos(Nodo raiz) {
        tablaVariables.clear();
        tablaAmbitosFunciones.clear();

        if (raiz == null) {
            return;
        }

        registrarVariablesGlobales(raiz);
        registrarVariablesFunciones(raiz);
    }

    /**
     * Registra las variables que se encuentran fuera de funciones.
     */
    private static void registrarVariablesGlobales(Nodo nodo) {
        if (nodo == null) {
            return;
        }

        // No se deben registrar aquí las variables de una función.
        if ("funcion".equals(nodo.tipo)) {
            return;
        }

        registrarDeclaracionEnAmbito(nodo, tablaVariables, "global");

        for (Nodo hijo : nodo.hijos) {
            registrarVariablesGlobales(hijo);
        }
    }

    /**
     * Registra los parámetros y las variables locales de cada función.
     */
    private static void registrarVariablesFunciones(Nodo nodo) {
        if (nodo == null) {
            return;
        }

        if ("funcion".equals(nodo.tipo)) {
            String nombre = nodo.valor;

            if (nombre == null || nombre.trim().isEmpty()) {
                return;
            }

            AmbitoFuncion ambito = new AmbitoFuncion(nombre);

            // Los hijos, excepto el último, son los parámetros.
            int cantidadParametros = Math.max(0, nodo.hijos.size() - 1);

            for (int i = 0; i < cantidadParametros; i++) {
                Nodo parametro = nodo.hijo(i);

                if (!"parametro".equals(parametro.tipo)) {
                    continue;
                }

                String nombreParametro = parametro.valor;
                String tipoParametro = normalizarTipo(
                    parametro.atributoPorDefecto("tipoDato", "desconocido")
                );

                if (ambito.variables.containsKey(nombreParametro)) {
                    error(
                        "El parámetro '" + nombreParametro
                        + "' está declarado más de una vez en la función '"
                        + nombre + "'."
                    );
                } else {
                    ambito.variables.put(nombreParametro, tipoParametro);
                }
            }

            // El último hijo es el cuerpo de la función.
            if (!nodo.hijos.isEmpty()) {
                Nodo cuerpo = nodo.hijo(nodo.hijos.size() - 1);
                registrarVariablesLocales(cuerpo, ambito);
            }

            tablaAmbitosFunciones.put(nombre, ambito);

            // No se recorren nuevamente los hijos de esta función.
            return;
        }

        for (Nodo hijo : nodo.hijos) {
            registrarVariablesFunciones(hijo);
        }
    }

    /**
     * Recorre el cuerpo de una función y registra sus variables locales.
     */
    private static void registrarVariablesLocales(
            Nodo nodo, AmbitoFuncion ambito) {

        if (nodo == null) {
            return;
        }

        // No se mezclan ámbitos de funciones diferentes.
        if ("funcion".equals(nodo.tipo)) {
            return;
        }

        registrarDeclaracionEnAmbito(
            nodo,
            ambito.variables,
            "la función '" + ambito.nombreFuncion + "'"
        );

        for (Nodo hijo : nodo.hijos) {
            registrarVariablesLocales(hijo, ambito);
        }
    }

    /**
     * Registra una declaración individual o una declaración múltiple.
     */
    private static void registrarDeclaracionEnAmbito(
            Nodo nodo,
            Map<String, String> variables,
            String descripcionAmbito) {

        if (nodo == null) {
            return;
        }

        if ("declaracionVariable".equals(nodo.tipo)) {
            String nombre = nodo.valor;
            String tipo = normalizarTipo(
                nodo.atributoPorDefecto("tipoDato", "desconocido")
            );

            if (nombre == null || nombre.trim().isEmpty()) {
                return;
            }

            if (variables.containsKey(nombre)) {
                error(
                    "La variable '" + nombre
                    + "' ya fue declarada en " + descripcionAmbito + "."
                );
            } else {
                variables.put(nombre, tipo);
            }
        }
    }
    
    /**
     * Valida las llamadas a funciones considerando el ámbito
     * desde el que se proporcionan los argumentos.
     */
    private static void validarLlamadasFunciones(
            Nodo nodo, String nombreFuncion) {

        if (nodo == null) {
            return;
        }

        // Actualiza el contexto cuando se entra a una función.
        if ("funcion".equals(nodo.tipo)) {
            nombreFuncion = nodo.valor;

            // Se recorren los hijos con el ámbito de esta función.
            for (Nodo hijo : nodo.hijos) {
                validarLlamadasFunciones(hijo, nombreFuncion);
            }

            return;
        }

        if ("llamadaFuncion".equals(nodo.tipo)) {
            String nombre = nodo.valor;
            FuncionInfo funcion = tablaFunciones.get(nombre);

            if (funcion == null) {
                error(
                    "La función '" + nombre
                    + "' se utiliza, pero no ha sido declarada."
                );

            } else {
                int esperados = funcion.tiposParametros.size();
                int recibidos = nodo.hijos.size();

                if (esperados != recibidos) {
                    error(
                        "La función '" + nombre + "' requiere "
                        + esperados + " parámetro(s), pero recibió "
                        + recibidos + "."
                    );
                }

                int limite = Math.min(esperados, recibidos);

                for (int i = 0; i < limite; i++) {

                    String tipoEsperado = normalizarTipo(
                        funcion.tiposParametros.get(i)
                    );

                    String tipoRecibido = normalizarTipo(
                        inferirTipo(nodo.hijo(i), nombreFuncion)
                    );

                    if ("desconocido".equals(tipoRecibido)) {
                        continue;
                    }

                    if (!tiposCompatibles(
                            tipoEsperado, tipoRecibido)) {

                        error(
                            "El parámetro " + (i + 1)
                            + " de la función '" + nombre
                            + "' debe ser de tipo '" + tipoEsperado
                            + "', pero se recibió '" + tipoRecibido + "'."
                        );
                    }
                }
            }
        }

        for (Nodo hijo : nodo.hijos) {
            validarLlamadasFunciones(hijo, nombreFuncion);
        }
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
     * Obtiene el tipo de una variable considerando el ámbito de la función
     * desde la que se está consultando.
     *
     * Primero busca parámetros y variables locales. Si no los encuentra,
     * consulta las variables globales.
     */
    private static String obtenerTipoVariable(
            String nombre, String nombreFuncion) {

        if (nombre == null) {
            return null;
        }

        if (nombreFuncion != null) {
            AmbitoFuncion ambito
                    = tablaAmbitosFunciones.get(nombreFuncion);

            if (ambito != null
                    && ambito.variables.containsKey(nombre)) {

                return ambito.variables.get(nombre);
            }
        }

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
        // Primero se registran todas las funciones.
        registrarFunciones(raiz);

        // Después se construyen los ámbitos de variables.
        registrarAmbitos(raiz);

        // Después se realizan las validaciones del programa.
        validarLlamadasFunciones(raiz, null);
        validarRetornos(raiz, false);
        validarTiposRetorno(raiz);
        validarVariablesNoDeclaradas(
            raiz,
            new HashSet<String>(),
            new HashSet<String>()
        );
        validarTipos(raiz);
        validarInicializacion(raiz, new HashSet<String>());
    }

    private static void validarRetornos(Nodo nodo, boolean dentroFuncion) {
        if (nodo == null) {
            return;
        }

        // Al encontrar una función, sus instrucciones pertenecen
        // al contexto de esa función.
        boolean contextoFuncion = dentroFuncion || "funcion".equals(nodo.tipo);

        // Un return solamente es válido dentro de una función.
        if ("returnInstruccion".equals(nodo.tipo) && !contextoFuncion) {
            errores.add(
                "La instrucción 'return' aparece fuera de una función o método. "
                + "No se puede traducir como código independiente."
            );
        }

        // Recorrer los hijos conservando el contexto de la función.
        for (Nodo hijo : nodo.hijos) {
            validarRetornos(hijo, contextoFuncion);
        }
    }

    private static void validarTiposRetorno(Nodo nodo) {
        if (nodo == null) {
            return;
        }

        if ("funcion".equals(nodo.tipo)) {
            String nombre = nodo.valor;
            String tipoRetorno = normalizarTipo(
                    nodo.atributoPorDefecto("tipoRetorno", "void")
            );

            // El último hijo de la función es su cuerpo.
            if (!nodo.hijos.isEmpty()) {
                Nodo cuerpo = nodo.hijos.get(nodo.hijos.size() - 1);

                validarRetornosDeFuncion(
                        cuerpo,
                        nombre,
                        tipoRetorno
                );
            }

            // No recorrer nuevamente el cuerpo desde el recorrido general.
            return;
        }

        for (Nodo hijo : nodo.hijos) {
            validarTiposRetorno(hijo);
        }
    }
    
    private static void validarRetornosDeFuncion(
            Nodo nodo,
            String nombreFuncion,
            String tipoEsperado) {

        if (nodo == null) {
            return;
        }

        if ("returnInstruccion".equals(nodo.tipo)) {

            boolean tieneExpresion = !nodo.hijos.isEmpty();

            // Las funciones void no deben devolver valores.
            if ("void".equals(tipoEsperado)) {
                if (tieneExpresion) {
                    errores.add(
                            "La función '" + nombreFuncion
                            + "' es de tipo void y no puede devolver un valor."
                    );
                }

                return;
            }

            // Las funciones con retorno deben devolver una expresión.
            if (!tieneExpresion) {
                errores.add(
                        "La función '" + nombreFuncion
                        + "' debe devolver un valor de tipo '"
                        + tipoEsperado + "'."
                );

                return;
            }

            String tipoReal = normalizarTipo(
                inferirTipo(nodo.hijo(0), nombreFuncion)
            );

            // Solo se valida cuando fue posible determinar el tipo.
            if (tipoReal != null
                    && !"desconocido".equals(tipoReal)
                    && !tiposCompatibles(tipoEsperado, tipoReal)) {

                errores.add(
                        "La función '" + nombreFuncion
                        + "' debe devolver un valor de tipo '"
                        + tipoEsperado
                        + "', pero devuelve '" + tipoReal + "'."
                );
            }

            return;
        }

        for (Nodo hijo : nodo.hijos) {
            validarRetornosDeFuncion(
                    hijo,
                    nombreFuncion,
                    tipoEsperado
            );
        }
    }
    
    /**
     * Detecta identificadores que no aparecen en la tabla de variables globales
     * ni en el ámbito de la función correspondiente.
     */
    private static void validarVariablesNoDeclaradas(
            Nodo nodo,
            Set<String> variablesLocales,
            Set<String> erroresReportados) {

        if (nodo == null) {
            return;
        }

        // Cada función tiene sus propios parámetros y variables locales.
        if ("funcion".equals(nodo.tipo)) {

            Set<String> variablesFuncion
                    = new HashSet<String>(variablesLocales);

            // Registrar los parámetros.
            int cantidadParametros
                    = Math.max(0, nodo.hijos.size() - 1);

            for (int i = 0; i < cantidadParametros; i++) {

                Nodo parametro = nodo.hijo(i);

                if ("parametro".equals(parametro.tipo)
                        && parametro.valor != null) {

                    variablesFuncion.add(parametro.valor);
                }
            }

            // Incorporar las variables locales registradas para esta función.
            AmbitoFuncion ambito
                    = tablaAmbitosFunciones.get(nodo.valor);

            if (ambito != null) {
                variablesFuncion.addAll(
                        ambito.variables.keySet()
                );
            }

            // Validar exclusivamente el cuerpo de la función.
            if (!nodo.hijos.isEmpty()) {

                Nodo cuerpo = nodo.hijo(
                        nodo.hijos.size() - 1
                );

                validarVariablesNoDeclaradas(
                        cuerpo,
                        variablesFuncion,
                        erroresReportados
                );
            }

            return;
        }

        // Comprobar los identificadores utilizados en el nodo.
        comprobarVariable(
                nodo,
                variablesLocales,
                erroresReportados
        );

        // La variable de foreach está disponible en su cuerpo.
        if ("cicloForeach".equals(nodo.tipo)) {

            comprobarNombre(
                    nodo.atributo("coleccion"),
                    variablesLocales,
                    erroresReportados
            );

            Set<String> localesCiclo
                    = new HashSet<String>(variablesLocales);

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

        // La variable de catch está disponible dentro de su bloque.
        if ("clausulaCatch".equals(nodo.tipo)) {

            Set<String> localesCatch
                    = new HashSet<String>(variablesLocales);

            String variableExcepcion
                    = nodo.atributo("variableExcepcion");

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

        // Continuar recorriendo el árbol.
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
    
    private static void validarElementosArreglo(
            String tipoEsperado,
            Nodo arreglo,
            String nombreVariable,
            String nombreFuncion) {

        if (tipoEsperado == null || arreglo == null
                || !"arreglo".equals(arreglo.tipo)
                || !tipoEsperado.endsWith("[]")) {
            return;
        }

        String tipoElemento = tipoEsperado.substring(
                0, tipoEsperado.length() - 2
        );

        for (int i = 0; i < arreglo.hijos.size(); i++) {

            Nodo elemento = arreglo.hijo(i);

            String tipoRecibido = inferirTipo(
                    elemento, nombreFuncion
            );

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
     * Inicia la validación de tipos desde el ámbito global.
     */
    private static void validarTipos(Nodo nodo) {
        validarTipos(nodo, null);
    }

    /**
     * Valida declaraciones y asignaciones considerando el ámbito
     * correspondiente.
     */
    private static void validarTipos(
            Nodo nodo, String nombreFuncion) {

        if (nodo == null) {
            return;
        }

        // Al entrar a una función, se utiliza su propio ámbito.
        if ("funcion".equals(nodo.tipo)) {

            String funcionActual = nodo.valor;

            // Se valida el cuerpo, que es el último hijo.
            if (!nodo.hijos.isEmpty()) {

                Nodo cuerpo = nodo.hijo(
                        nodo.hijos.size() - 1
                );

                validarTipos(cuerpo, funcionActual);
            }

            return;
        }

        // Validación de declaraciones.
        if ("declaracionVariable".equals(nodo.tipo)) {

            String tipoEsperado = nodo.atributo("tipoDato");
            Nodo inicializador = nodo.hijo(0);

            if (tipoEsperado != null && inicializador != null) {

                String tipoRecibido = inferirTipo(
                        inicializador, nombreFuncion
                );

                if (!tiposCompatibles(
                        tipoEsperado, tipoRecibido)) {

                    error(
                            "La variable '" + nodo.valor
                            + "' es de tipo '" + tipoEsperado
                            + "', pero recibe un valor de tipo '"
                            + tipoRecibido + "'."
                    );

                } else if (tipoEsperado.endsWith("[]")
                        && "arreglo".equals(tipoRecibido)) {

                    validarElementosArreglo(
                            tipoEsperado,
                            inicializador,
                            nodo.valor,
                            nombreFuncion
                    );
                }
            }
        }

        // Validación de asignaciones normales y compuestas.
        if ("asignacion".equals(nodo.tipo)
                || "asignacionSimple".equals(nodo.tipo)) {

            String operador = nodo.atributoPorDefecto(
                    "operadorAsignacion", "="
            );

            String tipoEsperado = obtenerTipoVariable(
                    nodo.valor, nombreFuncion
            );

            if (tipoEsperado != null
                    && !nodo.hijos.isEmpty()) {

                Nodo valorAsignado = nodo.hijo(
                        nodo.hijos.size() - 1
                );

                String tipoRecibido = inferirTipo(
                        valorAsignado, nombreFuncion
                );

                if ("=".equals(operador)) {

                    if (tipoEsperado.endsWith("[]")
                            && "arreglo".equals(tipoRecibido)) {

                        validarElementosArreglo(
                                tipoEsperado,
                                valorAsignado,
                                nodo.valor,
                                nombreFuncion
                        );

                    } else if (!tipoEsperado.endsWith("[]")
                            && !tiposCompatibles(
                                    tipoEsperado, tipoRecibido)) {

                        error(
                                "No se puede asignar un valor de tipo '"
                                + tipoRecibido + "' a la variable '"
                                + nodo.valor + "' de tipo '"
                                + tipoEsperado + "'."
                        );
                    }

                } else if ("+=".equals(operador)
                        || "-=".equals(operador)
                        || "*=".equals(operador)
                        || "/=".equals(operador)
                        || "%=".equals(operador)) {

                    String tipoDestino
                            = normalizarTipo(tipoEsperado);

                    String tipoOrigen
                            = normalizarTipo(tipoRecibido);

                    if (tipoEsperado.endsWith("[]")) {

                        error(
                                "El operador '" + operador
                                + "' no es válido para la variable '"
                                + nodo.valor + "' de tipo '"
                                + tipoEsperado + "'."
                        );

                    } else if (!"desconocido".equals(tipoOrigen)) {

                        boolean compatible = false;

                        if ("+=".equals(operador)
                                && "string".equals(tipoDestino)) {

                            compatible = true;

                        } else if (esNumerico(tipoDestino)
                                && esNumerico(tipoOrigen)) {

                            compatible = true;
                        }

                        if (!compatible) {

                            error(
                                    "No se puede aplicar el operador '"
                                    + operador + "' a la variable '"
                                    + nodo.valor + "' de tipo '"
                                    + tipoEsperado
                                    + "' con un valor de tipo '"
                                    + tipoRecibido + "'."
                            );
                        }
                    }
                }
            }
        }

        // Continuar recorriendo el AST con el mismo ámbito.
        for (Nodo hijo : nodo.hijos) {
            validarTipos(hijo, nombreFuncion);
        }
    }

    /**
     * Mantiene la inferencia de tipos para consultas globales.
     */
    private static String inferirTipo(Nodo nodo) {
        return inferirTipo(nodo, null);
    }

    /**
     * Determina el tipo de una expresión considerando las variables locales de
     * la función correspondiente.
     */
    private static String inferirTipo(
            Nodo nodo, String nombreFuncion) {

        if (nodo == null) {
            return "desconocido";
        }

        if ("valor".equals(nodo.tipo)) {
            String tipoValor = nodo.atributo("tipoValor");

            if ("numero".equals(tipoValor)) {
                return "int";
            }
            if ("decimal".equals(tipoValor)) {
                return "double";
            }
            if ("cadena".equals(tipoValor)) {
                return "string";
            }

            if ("true".equals(tipoValor)
                    || "false".equals(tipoValor)) {
                return "boolean";
            }

            if ("null".equals(tipoValor)) {
                return "null";
            }

            if ("identificador".equals(tipoValor)) {
                String tipo = obtenerTipoVariable(
                        nodo.valor, nombreFuncion
                );

                return tipo == null ? "desconocido" : tipo;
            }
        }

        if ("lecturaConsola".equals(nodo.tipo)) {
            return "string";
        }

        // Reconoce las conversiones de tipos de C#.
        if ("conversionTipo".equals(nodo.tipo)) {
            String tipo = nodo.atributo("tipo");

            if ("int".equals(tipo)) {
                return "int";
            }
            if ("double".equals(tipo)) {
                return "double";
            }
        }

        if ("propiedadArreglo".equals(nodo.tipo)) {
            return "int";
        }

        if ("accesoArreglo".equals(nodo.tipo)) {
            String tipo = obtenerTipoVariable(
                    nodo.valor, nombreFuncion
            );

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
            return inferirTipo(nodo.hijo(0), nombreFuncion);
        }

        // Las llamadas a funciones utilizan su tipo de retorno.
        if ("llamadaFuncion".equals(nodo.tipo)) {
            FuncionInfo funcion = tablaFunciones.get(nodo.valor);

            if (funcion != null) {
                return funcion.tipoRetorno;
            }

            return "desconocido";
        }

        // Inferencia de operaciones aritméticas.
        if (nodo.atributo("operador") != null
                && nodo.hijos.size() >= 2) {

            String operador = nodo.atributo("operador");

            String tipo1 = inferirTipo(
                    nodo.hijo(0), nombreFuncion
            );

            String tipo2 = inferirTipo(
                    nodo.hijo(1), nombreFuncion
            );

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
            return inferirTipo(nodo.hijo(0), nombreFuncion);
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

                // Cada función tiene su propio estado de inicialización.
        if ("funcion".equals(nodo.tipo)) {

            Set<String> estadoFuncion =
                new HashSet<String>(inicializadas);

            // Se eliminan del estado heredado las variables locales
            // de esta función, para evitar compartir estados por nombre.
            AmbitoFuncion ambito =
                tablaAmbitosFunciones.get(nodo.valor);

            if (ambito != null) {
                for (String nombreVariable : ambito.variables.keySet()) {
                    estadoFuncion.remove(nombreVariable);
                }
            }

            // Los parámetros se consideran inicializados al entrar
            // en la función, porque reciben sus valores al invocarla.
            FuncionInfo funcion = tablaFunciones.get(nodo.valor);

            if (funcion != null) {
                for (String nombreParametro : funcion.nombresParametros) {
                    estadoFuncion.add(nombreParametro);
                }
            }

            // El último hijo de la función contiene su cuerpo.
            if (!nodo.hijos.isEmpty()) {
                validarInicializacion(
                    nodo.hijo(nodo.hijos.size() - 1),
                    estadoFuncion
                );
            }

            // No se propagan las variables locales al programa
            // ni a las demás funciones.
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

        // SWITCH: analiza cada caso por separado y combina
        // las variables inicializadas en todas las ramas.
        if ("estructuraSwitch".equals(nodo.tipo)) {

            // El primer hijo contiene la expresión del switch.
            validarUsosExpresion(
                nodo.hijo(0), inicializadas
            );

            Set<String> estadoComun = null;
            boolean tieneDefault = false;

            // Los casos comienzan en el segundo hijo.
            for (int i = 1; i < nodo.hijos.size(); i++) {

                Nodo rama = nodo.hijo(i);

                Set<String> estadoRama =
                    new HashSet<String>(inicializadas);

                int inicioInstrucciones = 0;

                if ("casoSwitch".equals(rama.tipo)) {

                    // El primer hijo del caso es su etiqueta.
                    if (!rama.hijos.isEmpty()) {
                        validarUsosExpresion(
                            rama.hijo(0), inicializadas
                        );
                    }

                    inicioInstrucciones = 1;

                } else if ("casoDefault".equals(rama.tipo)) {

                    tieneDefault = true;

                } else {
                    continue;
                }

                // Procesa las instrucciones de esta rama
                // en orden y conserva sus asignaciones.
                for (int j = inicioInstrucciones;
                        j < rama.hijos.size(); j++) {

                    validarInicializacion(
                        rama.hijo(j), estadoRama
                    );
                }

                // Conserva únicamente las variables inicializadas
                // en todas las ramas analizadas.
                if (estadoComun == null) {
                    estadoComun =
                        new HashSet<String>(estadoRama);
                } else {
                    estadoComun.retainAll(estadoRama);
                }
            }

            // Sin default, el switch podría no ejecutar ningún caso.
            // Por eso solo propagamos el resultado si existe default.
            if (tieneDefault && estadoComun != null) {
                inicializadas.clear();
                inicializadas.addAll(estadoComun);
            }

            return;
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

        // Solo se comprueban variables que estén registradas
        // en el ámbito global o en alguno de los ámbitos de función.
        if (!variableRegistrada(nombre)) {
            return;
        }

        if (!inicializadas.contains(nombre)) {
            error(
                    "La variable '" + nombre
                    + "' se utiliza antes de recibir un valor."
            );
        }
    }
    
    /**
     * Comprueba si una variable está registrada en algún ámbito.
     */
    private static boolean variableRegistrada(String nombre) {

        if (nombre == null || nombre.isEmpty()) {
            return false;
        }

        // Primero se revisan las variables globales.
        if (tablaVariables.containsKey(nombre)) {
            return true;
        }

        // Después se revisan las variables de las funciones.
        for (AmbitoFuncion ambito : tablaAmbitosFunciones.values()) {
            if (ambito.variables.containsKey(nombre)) {
                return true;
            }
        }

        return false;
    }
}