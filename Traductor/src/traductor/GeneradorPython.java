package traductor;

import java.util.*;

/**
 * Recorre el AST (Nodo "programa") y emite código Python equivalente.
 *
 * Cobertura actual (mismas construcciones que ya reconoce la gramática):
 *   declaracionVariable, asignacion, condicion (if/else), cicloMientras,
 *   cicloPara, cicloDoWhile, cicloForeach, estructuraSwitch, escritura,
 *   break/continue/return, bloque, expresiones aritméticas y lógicas.
 *
 * Es intencionalmente simple (concatenación de strings con indentación
 * manual) para que sea fácil de leer, depurar y corregir/ampliar.
 */
public class GeneradorPython {

    private static final String INDENT = "    ";
    // Permite distinguir si un break pertenece a un ciclo o a un switch.
    private final Deque<String> contextosRuptura = new ArrayDeque<>();
    // Guarda la actualización correspondiente al ciclo donde aparece continue.
    private final Deque<String> actualizacionesContinue = new ArrayDeque<>();

    public String generar(Nodo raiz) {
        contextosRuptura.clear();
        actualizacionesContinue.clear();

        StringBuilder sb = new StringBuilder();
        for (Nodo hijo : raiz.hijos) {
            sb.append(generarInstruccion(hijo, 0));
        }
        return sb.toString();
    }
    
    private String tab(int nivel) {
        return INDENT.repeat(Math.max(0, nivel));
    }

    private String generarInstruccion(Nodo n, int nivel) {
        switch (n.tipo) {
            case "comentario": return generarComentario(n, nivel);
            case "declaracionVariable": return generarDeclaracion(n, nivel);
            case "declaracionesMultiples": StringBuilder declaraciones = new StringBuilder();
                for (Nodo variable : n.hijos) {
                    declaraciones.append(
                        generarDeclaracion(variable, nivel)
                    );
                }
                return declaraciones.toString();
            case "operacionArreglo":    return generarOperacionArreglo(n, nivel);
            case "copiarArreglo":       return generarCopiarArreglo(n, nivel);
            case "asignacion":          return generarAsignacion(n, nivel);
            case "asignacionSimple":    return generarAsignacion(n, nivel);
            case "incrementoDecremento": return generarIncrementoDecremento(n, nivel);
            case "condicion":           return generarCondicion(n, nivel);
            case "cicloPara":           return generarPara(n, nivel);
            case "cicloDoWhile":        return generarDoWhile(n, nivel);
            case "estructuraSwitch":    return generarSwitch(n, nivel);
            case "escritura":           return tab(nivel) + "print(" + generarExpr(n.hijo(0)) + ")\n";
            case "cicloMientras":       return generarMientras(n, nivel);
            case "cicloForeach":        return generarForeach(n, nivel);
            case "breakInstruccion":    return generarBreak(nivel);
            case "continueInstruccion": return generarContinue(nivel);
            case "returnInstruccion":   return tab(nivel) + "return" + (n.hijos.isEmpty() ? "" : " " + generarExpr(n.hijo(0))) + "\n";
            case "bloque":              return generarBloque(n, nivel);
            default:
                return tab(nivel) + "# TODO: nodo no soportado aun -> " + n.tipo + "\n";
        }
    }
    
    private String generarComentario(Nodo n, int nivel) {
        String comentario = n.valor;

        if (comentario.startsWith("//")) {
            return tab(nivel)
                    + "#"
                    + comentario.substring(2)
                    + "\n";
        }

        if (comentario.startsWith("/*")
                && comentario.endsWith("*/")) {

            String contenido = comentario.substring(
                    2, comentario.length() - 2
            );

            String[] lineas = contenido.split("\\R", -1);
            StringBuilder resultado = new StringBuilder();

            for (String linea : lineas) {
                resultado.append(tab(nivel))
                        .append("#")
                        .append(linea.isEmpty() ? "" : " " + linea.stripLeading())
                        .append("\n");
            }

            return resultado.toString();
        }

        return tab(nivel) + "# " + comentario + "\n";
    }

    private String generarDeclaracion(Nodo n, int nivel) {
        String nombre = n.valor;
        String tipoDato = n.atributoPorDefecto("tipoDato", "");

        if (!n.hijos.isEmpty()) {
            return tab(nivel) + nombre + " = "
                    + generarExpr(n.hijo(0)) + "\n";
        }

        String valorDefault = switch (tipoDato) {
            case "int", "long", "short", "byte" -> "0";
            case "double", "float" -> "0.0";
            case "bool", "boolean" -> "False";
            case "string" -> "\"\"";
            case "char" -> "''";
            default -> tipoDato.endsWith("[]") ? "[]" : "None";
        };

        return tab(nivel) + nombre + " = " + valorDefault + "\n";
    }
    
    private String generarAsignacion(Nodo n, int nivel) {
        String operador = n.atributoPorDefecto(
                "operadorAsignacion", "=");

        String destino;
        Nodo expresion;

        if ("true".equals(n.atributo("asignacionArreglo"))) {
            destino = n.valor + "[" + generarExpr(n.hijo(0)) + "]";
            expresion = n.hijo(1);
        } else {
            destino = n.valor;
            expresion = n.hijo(0);
        }

        return tab(nivel) + destino + " " + operador + " "
                + generarExpr(expresion) + "\n";
    }

    private String generarContinue(int nivel) {
        StringBuilder sb = new StringBuilder();

        if (!actualizacionesContinue.isEmpty()) {
            String actualizacion = actualizacionesContinue.peek();

            if (!actualizacion.isEmpty()) {
                sb.append(tab(nivel))
                  .append(actualizacion)
                  .append("\n");
            }
        }

        sb.append(tab(nivel)).append("continue\n");

        return sb.toString();
    }
    
    private String generarIncrementoDecremento(Nodo n, int nivel) {
        String operador = n.atributoPorDefecto("operador", "");

        if ("++".equals(operador)) {
            return tab(nivel) + n.valor + " += 1\n";
        }

        if ("--".equals(operador)) {
            return tab(nivel) + n.valor + " -= 1\n";
        }

        return "";
    }
    
    private String generarMientras(Nodo n, int nivel) {
        actualizacionesContinue.push("");

        try {
            return tab(nivel)
                    + "while " + generarExpr(n.hijo(0)) + ":\n"
                    + generarBloqueConContexto(
                        n.hijo(1), nivel + 1, "ciclo"
                    );
        } finally {
            actualizacionesContinue.pop();
        }
    }

    private String generarForeach(Nodo n, int nivel) {
        actualizacionesContinue.push("");

        try {
            return tab(nivel)
                    + "for " + n.valor + " in "
                    + n.atributo("coleccion") + ":\n"
                    + generarBloqueConContexto(
                        n.hijo(0), nivel + 1, "ciclo"
                    );
        } finally {
            actualizacionesContinue.pop();
        }
    }

    private String generarBreak(int nivel) {
        if (!contextosRuptura.isEmpty()) {
            String contexto = contextosRuptura.peek();

            if ("switch".equals(contexto)) {
                // En C#, este break termina el switch.
                // El if/elif generado en Python termina por sí mismo.
                return "";
            }

            if ("ciclo".equals(contexto)) {
                return tab(nivel) + "break\n";
            }
        }

        // Evita generar un break inválido en Python.
        return tab(nivel)
                + "# break sin ciclo ni switch contenedor\n";
    }

    private String generarBloqueConContexto(
            Nodo bloque, int nivel, String contexto) {

        contextosRuptura.push(contexto);

        try {
            return generarBloque(bloque, nivel);
        } finally {
            contextosRuptura.pop();
        }
    }
    
    private String generarCondicion(Nodo n, int nivel) {
        StringBuilder sb = new StringBuilder();
        sb.append(tab(nivel)).append("if ").append(generarExpr(n.hijo(0))).append(":\n");
        sb.append(generarBloque(n.hijo(1), nivel + 1));
        if ("true".equals(n.atributo("tieneElse"))) {
            sb.append(tab(nivel)).append("else:\n");
            sb.append(generarBloque(n.hijo(2), nivel + 1));
        }
        return sb.toString();
    }
    
    private String generarPara(Nodo n, int nivel) {
        Nodo init = n.hijo(0);
        Nodo cond = n.hijo(1);
        Nodo upd = n.hijo(2);
        Nodo cuerpo = n.hijo(3);

        StringBuilder sb = new StringBuilder();

        // Inicialización: puede estar vacía.
        if (init != null && !"sinInicializacion".equals(init.tipo)) {
            sb.append(generarInstruccion(init, nivel));
        }

        // Preparar la actualización del ciclo.
        String actualizacion = obtenerActualizacionPara(upd);

        // Condición del ciclo.
        sb.append(tab(nivel))
          .append("while ")
          .append(generarExpr(cond))
          .append(":\n");

        // El continue de este ciclo debe ejecutar su actualización.
        actualizacionesContinue.push(actualizacion);

        try {
            sb.append(
                generarBloqueConContexto(cuerpo, nivel + 1, "ciclo")
            );
        } finally {
            actualizacionesContinue.pop();
        }

        // Actualización normal, al terminar el cuerpo.
        sb.append(tab(nivel + 1))
          .append(actualizacion)
          .append("\n");

        return sb.toString();
    }

    private String obtenerActualizacionPara(Nodo n) {
        if (n == null) {
            return "";
        }

        String operador = n.atributoPorDefecto(
            "operadorAsignacion", "="
        );

        if ("++".equals(operador)) {
            return n.valor + " += 1";
        }

        if ("--".equals(operador)) {
            return n.valor + " -= 1";
        }

        Nodo expresion = n.hijo(0);

        return n.valor + " " + operador + " " + generarExpr(expresion);
    }
    
    private String generarDoWhile(Nodo n, int nivel) {
        Nodo cuerpo = n.hijo(0);
        Nodo cond = n.hijo(1);

        StringBuilder sb = new StringBuilder();

        sb.append(tab(nivel)).append("while True:\n");

        actualizacionesContinue.push("");

        try {
            sb.append(
                generarBloqueConContexto(cuerpo, nivel + 1, "ciclo")
            );
        } finally {
            actualizacionesContinue.pop();
        }

        sb.append(tab(nivel + 1))
          .append("if not (")
          .append(generarExpr(cond))
          .append("):\n");

        sb.append(tab(nivel + 2)).append("break\n");

        return sb.toString();
    }
    
    private String generarSwitch(Nodo n, int nivel) {
        StringBuilder sb = new StringBuilder();
        String expr = generarExpr(n.hijo(0));
        boolean primero = true;

        int i = 1;

        while (i < n.hijos.size()) {
            Nodo caso = n.hijo(i);

            if (caso.tipo.equals("casoSwitch")) {
                List<Nodo> etiquetas = new ArrayList<>();
                etiquetas.add(caso);

                // Agrupar etiquetas consecutivas sin instrucciones.
                while (caso.hijos.size() == 1
                        && i + 1 < n.hijos.size()
                        && n.hijo(i + 1).tipo.equals("casoSwitch")) {

                    i++;
                    caso = n.hijo(i);
                    etiquetas.add(caso);
                }

                StringBuilder condicion = new StringBuilder();

                for (int j = 0; j < etiquetas.size(); j++) {
                    if (j > 0) {
                        condicion.append(" or ");
                    }

                    condicion.append(expr)
                             .append(" == ")
                             .append(generarExpr(etiquetas.get(j).hijo(0)));
                }

                String palabra = primero ? "if" : "elif";

                sb.append(tab(nivel))
                  .append(palabra)
                  .append(" ")
                  .append(condicion)
                  .append(":\n");

                sb.append(generarCuerpoCaso(caso, 1, nivel + 1));

                primero = false;
                i++;

            } else if (caso.tipo.equals("casoDefault")) {
                sb.append(tab(nivel)).append("else:\n");
                sb.append(generarCuerpoCaso(caso, 0, nivel + 1));
                i++;

            } else {
                i++;
            }
        }

        return sb.toString();
    }

    private String generarCuerpoCaso(
            Nodo caso, int desdeIndice, int nivel) {

        StringBuilder sb = new StringBuilder();

        contextosRuptura.push("switch");

        try {
            for (int j = desdeIndice; j < caso.hijos.size(); j++) {
                sb.append(generarInstruccion(caso.hijo(j), nivel));
            }
        } finally {
            contextosRuptura.pop();
        }

        if (sb.length() == 0) {
            sb.append(tab(nivel)).append("pass\n");
        }

        return sb.toString();
    }

    private String generarBloque(Nodo bloque, int nivel) {
        if (bloque == null || bloque.hijos.isEmpty()) {
            return tab(nivel) + "pass\n";
        }
        
        StringBuilder sb = new StringBuilder();
        
        for (Nodo inst : bloque.hijos) {
            sb.append(generarInstruccion(inst, nivel));
        }
        
        return sb.toString();
    }

    /** Genera expresiones simples y expresiones anidadas. */
    private String generarExpr(Nodo n) {

        if (n == null) {
            return "";
        }
        
        // Lectura de datos desde la consola
        if (n.tipo.equals("lecturaConsola")) {
            return "input()";
        }
        
        // Llamadas a funciones
        if (n.tipo.equals("llamadaFuncion")) {
            StringBuilder llamada = new StringBuilder();
            llamada.append(n.valor).append("(");

            for (int i = 0; i < n.hijos.size(); i++) {
                if (i > 0) {
                    llamada.append(", ");
                }

                llamada.append(generarExpr(n.hijo(i)));
            }

            llamada.append(")");
            return llamada.toString();
        }
        
        // Array.IndexOf(arreglo, elemento)
        if ("busquedaIndiceArreglo".equals(n.tipo)) {
            String arreglo = n.valor;
            String elemento = generarExpr(n.hijo(0));

            return arreglo + ".index(" + elemento + ")"
                    + " if " + elemento + " in " + arreglo
                    + " else -1";
        }

        // Arreglo literal: {10, 20, 30} -> [10, 20, 30]
        if ("arreglo".equals(n.tipo)) {
            StringBuilder sb = new StringBuilder("[");

            for (int i = 0; i < n.hijos.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(generarExpr(n.hijo(i)));
            }

            sb.append("]");
            return sb.toString();
        }

        // Acceso por índice: numeros[0]
        if ("accesoArreglo".equals(n.tipo)) {
            return n.valor + "[" + generarExpr(n.hijo(0)) + "]";
        }

        // Propiedad Length: numeros.Length -> len(numeros)
        if ("propiedadArreglo".equals(n.tipo)) {
            return "len(" + n.valor + ")";
        }

        // Valor simple
        if (n.tipo.equals("valor")) {
            return generarValor(n);
        }

        // Expresión agrupada con paréntesis
        if (n.tipo.equals("agrupacion")) {
            return "(" + generarExpr(n.hijo(0)) + ")";
        }

        String op = n.atributo("operador");

        // Negación lógica
        if ("!".equals(op)) {
            return "not " + generarExpr(n.hijo(0));
        }

        // Negación aritmética
        if ("unario-".equals(op)) {
            return "-" + generarExpr(n.hijo(0));
        }

        // Expresión con dos operandos
        if (n.hijos.size() == 2 && op != null) {
            String izquierda = generarExpr(n.hijo(0));
            String derecha = generarExpr(n.hijo(1));

            String operador = op;

            // Operadores lógicos de C# a Python
            if ("&&".equals(operador)) {
                operador = "and";
            } else if ("||".equals(operador)) {
                operador = "or";
            }

            return izquierda + " " + operador + " " + derecha;
        }

        // Si el nodo solo contiene una expresión, la seguimos recorriendo
        if (n.hijos.size() == 1) {
            return generarExpr(n.hijo(0));
        }

        return "";
    }

    private String generarValor(Nodo v) {
        String tipo = v.atributo("tipoValor");
        return switch (tipo == null ? "" : tipo) {
            case "true" -> "True";
            case "false" -> "False";
            case "null" -> "None";
            case "cadena" -> normalizarCadena(v.valor);
            default -> v.valor; // identificador, numero, decimal
        };
    }

    private String normalizarCadena(String lexema) {
        // Convierte comillas simples de C# a comillas dobles estilo Python
        if (lexema.startsWith("'") && lexema.endsWith("'")) {
            return "\"" + lexema.substring(1, lexema.length() - 1) + "\"";
        }
        return lexema;
    }
    
    private String generarOperacionArreglo(Nodo n, int nivel) {
        String nombreArreglo = n.valor;
        String operacion = n.atributo("operacion");

        if ("Sort".equals(operacion)) {
            return tab(nivel) + nombreArreglo + ".sort()\n";
        }

        if ("Reverse".equals(operacion)) {
            return tab(nivel) + nombreArreglo + ".reverse()\n";
        }

        return tab(nivel) + "# Operacion de arreglo no soportada\n";
    }
    
    private String generarCopiarArreglo(Nodo n, int nivel) {
        String origen = n.atributo("origen");
        String destino = n.atributo("destino");
        String cantidad = generarExpr(n.hijo(0));

        return tab(nivel) + destino + "[:"
                + cantidad + "] = "
                + origen + "[:" + cantidad + "]\n";
    }
}
