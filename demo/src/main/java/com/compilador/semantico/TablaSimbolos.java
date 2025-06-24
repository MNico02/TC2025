package com.compilador.semantico;

import java.util.*;

/**
 * Tabla de Símbolos que gestiona ámbitos (scopes) y permite
 * inserción, búsqueda y reporte de identificadores a lo largo del programa.
 *
 * Opción 1 aplicada: **no** eliminamos el mapa de símbolos al salir del ámbito,
 * de modo que las variables y parámetros locales queden disponibles para el
 * reporte final de la tabla, pero la semántica del compilador sigue intacta.
 */
public class TablaSimbolos {

    // ────────────────────────────────────────────────────────────────────────────
    //  Atributos
    // ────────────────────────────────────────────────────────────────────────────
    /** Mapa (ámbito → mapa ordenado de nombre → símbolo) */
    private final Map<String, LinkedHashMap<String, Simbolo>> tablaPorAmbito = new HashMap<>();

    /** Pila de ámbitos activos. El tope indica el ámbito actual */
    private final Deque<String> pilaAmbitos = new ArrayDeque<>();

    /** Contador global para asignar el campo `orden` incrementalmente */
    private int contadorOrden = 0;

    // ────────────────────────────────────────────────────────────────────────────
    //  Constructor
    // ────────────────────────────────────────────────────────────────────────────
    public TablaSimbolos() {
        // Ámbito raíz
        pilaAmbitos.push("global");
        tablaPorAmbito.put("global", new LinkedHashMap<>());
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Gestión de ámbitos
    // ────────────────────────────────────────────────────────────────────────────
    public String getAmbitoActual() {
        return pilaAmbitos.peek();
    }

    public void entrarAmbito(String ambito) {
        pilaAmbitos.push(ambito);
        tablaPorAmbito.putIfAbsent(ambito, new LinkedHashMap<>());
    }

    /**
     * Opción 1 → no borramos el mapa del ámbito al salir, sólo desapilamos.
     */
    public void salirAmbito() {
        pilaAmbitos.pop();
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Inserción y búsqueda de símbolos
    // ────────────────────────────────────────────────────────────────────────────
    public boolean agregar(Simbolo s) {
        String ambito = getAmbitoActual();
        LinkedHashMap<String, Simbolo> mapaActual = tablaPorAmbito.get(ambito);

        if (mapaActual.containsKey(s.getNombre())) {
            return false; // redeclaración
        }

        // Clonamos el símbolo asignando el orden
        Simbolo sConOrden = new Simbolo(
                s.getNombre(),
                s.getTipo(),
                s.getCategoria(),
                s.getLinea(),
                s.getColumna(),
                s.getAmbito(),
                s.esConstante(),
                contadorOrden++
        );

        mapaActual.put(sConOrden.getNombre(), sConOrden);
        return true;
    }

    /** Búsqueda jerárquica desde el ámbito actual hasta global */
    public Simbolo buscar(String nombre) {
        for (String ambito : pilaAmbitos) {
            LinkedHashMap<String, Simbolo> mapa = tablaPorAmbito.get(ambito);
            if (mapa != null && mapa.containsKey(nombre)) {
                return mapa.get(nombre);
            }
        }
        return null;
    }

    /** Búsqueda estricta en un ámbito dado */
    public Simbolo buscarEnAmbitoExacto(String ambito, String nombre) {
        LinkedHashMap<String, Simbolo> mapa = tablaPorAmbito.get(ambito);
        return (mapa != null) ? mapa.get(nombre) : null;
    }

    /** Búsqueda en un ámbito específico y luego en sus ancestros */
    public Simbolo buscarEnAmbitoActualYPadre(String nombre, String ambitoEspecifico) {
        Deque<String> copia = new ArrayDeque<>(pilaAmbitos);
        while (!copia.isEmpty() && !copia.peek().equals(ambitoEspecifico)) {
            copia.pop();
        }
        for (String amb : copia) {
            LinkedHashMap<String, Simbolo> mapa = tablaPorAmbito.get(amb);
            if (mapa != null && mapa.containsKey(nombre)) {
                return mapa.get(nombre);
            }
        }
        // Finalmente global
        return tablaPorAmbito.get("global").get(nombre);
    }

    // ────────────────────────────────────────────────────────────────────────────
    //  Reporte
    // ────────────────────────────────────────────────────────────────────────────
    public List<Simbolo> getTodosLosSimbolos() {
        List<Simbolo> lista = new ArrayList<>();
        for (LinkedHashMap<String, Simbolo> mapa : tablaPorAmbito.values()) {
            lista.addAll(mapa.values());
        }
        lista.sort(Comparator.comparingInt(Simbolo::getOrden));
        return lista;
    }

    public void imprimir() {
        System.out.println("\n=== TABLA DE SÍMBOLOS ===");
        System.out.printf("%-12s %-10s %-9s %-5s %-5s %-11s %-15s %-10s%n",
                "NOMBRE", "TIPO", "CATEGORIA", "LIN", "COL", "AMBITO", "PARAMETROS", "FLAGS");
        System.out.println("------------------------------------------------------------------------------");
        for (Simbolo s : getTodosLosSimbolos()) {
            String params = s.getCategoria() == Simbolo.Categoria.FUNCION
                    ? s.getParametros().toString()
                    : "-";
            String flags = (s.esConstante() ? "const " : "")
                    + (s.esInicializada() ? "" : "no-inicializada ")
                    + (s.esUsada() ? "" : "no-usada");
            System.out.printf("%-12s %-10s %-9s %-5d %-5d %-11s %-15s %-10s%n",
                    s.getNombre(),
                    s.getTipo(),
                    s.getCategoria().name().toLowerCase(),
                    s.getLinea(),
                    s.getColumna(),
                    s.getAmbito(),
                    params,
                    flags.trim());
        }
    }
}
