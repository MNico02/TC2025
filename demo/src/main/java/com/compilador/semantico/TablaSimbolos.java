package com.compilador.semantico;

import java.util.*;

/**
 * Tabla de Símbolos que gestiona ámbitos, orden de declaración y permite
 * búsquedas locales y jerárquicas (local → global).
 */
public class TablaSimbolos {
    // Mapa de ámbito → (nombre símbolo → Simbolo)
    private final Map<String, LinkedHashMap<String, Simbolo>> tablaPorAmbito = new HashMap<>();

    // Pila de ámbitos activos (tope = ámbito actual)
    private final Deque<String> pilaAmbitos = new ArrayDeque<>();

    // Contador global para asignar orden incremental a cada símbolo
    private int contadorOrden = 0;

    public TablaSimbolos() {
        // Iniciar con ámbito "global"
        pilaAmbitos.push("global");
        tablaPorAmbito.put("global", new LinkedHashMap<>());
    }

    /**
     * Devuelve el ámbito actual (tope de la pila).
     */
    public String getAmbitoActual() {
        return pilaAmbitos.peek();
    }

    /**
     * Entra a un nuevo ámbito (por ejemplo, al entrar en el cuerpo de una función).
     * Crea un mapa vacío si aún no existe ese ámbito.
     */
    public void entrarAmbito(String ambito) {
        pilaAmbitos.push(ambito);
        tablaPorAmbito.putIfAbsent(ambito, new LinkedHashMap<>());
    }

    /**
     * Sale del ámbito actual, regresando al anterior.
     * Nota: eliminamos el mapa de símbolos de ese ámbito para no mezclarlo
     * con futuros ámbitos que puedan reusar el mismo nombre. Si se desea
     * conservar símbolos de funciones para imprimir luego, se podría omitir
     * la remoción aquí.
     */
    public void salirAmbito() {
        String ambito = pilaAmbitos.pop();
        tablaPorAmbito.remove(ambito);
    }

    /**
     * Agrega un símbolo en el ámbito actual.
     * Retorna true si se agregó exitosamente, o false si ya existía un símbolo
     * con el mismo nombre en ese mismo ámbito.
     *
     * Asigna automáticamente un valor de "orden" incremental al símbolo para
     * poder comparar órdenes de declaración.
     */
    public boolean agregar(Simbolo s) {
        String ambito = getAmbitoActual();
        LinkedHashMap<String, Simbolo> mapaActual = tablaPorAmbito.get(ambito);

        if (mapaActual.containsKey(s.getNombre())) {
            return false;
        }
        // Crear un nuevo Simbolo con orden asignado
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
        // Si es función, sus parámetros se agregarán en exitDeclaracionFuncion del listener
        mapaActual.put(sConOrden.getNombre(), sConOrden);
        return true;
    }

    /**
     * Busca un símbolo por nombre empezando en el ámbito actual y, de no hallarlo,
     * retrocede en la pila de ámbitos hasta "global". Retorna null si no se encuentra.
     */
    public Simbolo buscar(String nombre) {
        for (String ambito : pilaAmbitos) {
            LinkedHashMap<String, Simbolo> mapa = tablaPorAmbito.get(ambito);
            if (mapa != null && mapa.containsKey(nombre)) {
                return mapa.get(nombre);
            }
        }
        return null;
    }

    /**
     * Busca un símbolo EXACTAMENTE en el ámbito dado (sin buscar en padres).
     * Retorna null si no existe en ese ámbito.
     */
    public Simbolo buscarEnAmbitoExacto(String ambito, String nombre) {
        LinkedHashMap<String, Simbolo> mapa = tablaPorAmbito.get(ambito);
        if (mapa != null && mapa.containsKey(nombre)) {
            return mapa.get(nombre);
        }
        return null;
    }

    /**
     * Busca un símbolo en el ámbito especificado, y si no está allí, busca
     * en su ámbito padre (local→global). Retorna null si no existe.
     *
     * Útil cuando se quiere verificar parámetros dentro de una función sin
     * salir del ámbito de la misma.
     */
    public Simbolo buscarEnAmbitoActualYPadre(String nombre, String ambitoEspecifico) {
        // Guardar pila temporal
        Deque<String> pilaCopia = new ArrayDeque<>(pilaAmbitos);
        // Bajar hasta que el tope sea ambitoEspecifico
        while (!pilaCopia.isEmpty() && !pilaCopia.peek().equals(ambitoEspecifico)) {
            pilaCopia.pop();
        }
        // Buscar en ambitoEspecifico y luego en sus padres
        for (String ambito : pilaCopia) {
            LinkedHashMap<String, Simbolo> mapa = tablaPorAmbito.get(ambito);
            if (mapa != null && mapa.containsKey(nombre)) {
                return mapa.get(nombre);
            }
        }
        // Finalmente, buscar en global
        LinkedHashMap<String, Simbolo> globalMap = tablaPorAmbito.get("global");
        if (globalMap != null && globalMap.containsKey(nombre)) {
            return globalMap.get(nombre);
        }
        return null;
    }

    /**
     * Devuelve una lista de todos los símbolos en todos los ámbitos, ordenada
     * por el campo 'orden' que indica el momento de su declaración.
     */
    public List<Simbolo> getTodosLosSimbolos() {
        List<Simbolo> lista = new ArrayList<>();
        for (LinkedHashMap<String, Simbolo> mapa : tablaPorAmbito.values()) {
            lista.addAll(mapa.values());
        }
        lista.sort(Comparator.comparingInt(Simbolo::getOrden));
        return lista;
    }

    /**
     * Imprime en consola la tabla completa de símbolos, mostrando:
     * NOMBRE | TIPO | CATEGORÍA | LÍNEA | COLUMNA | ÁMBITO | PARÁMETROS | FLAGS
     */
    public void imprimir() {
        System.out.println("\n=== TABLA DE SÍMBOLOS ===");
        System.out.printf("%-10s %-10s %-10s %-5s %-7s %-10s %-15s %-10s%n",
                "NOMBRE", "TIPO", "CATEGORIA", "LIN", "COL", "AMBITO", "PARAMETROS", "FLAGS");
        System.out.println("------------------------------------------------------------------------------");
        for (Simbolo s : getTodosLosSimbolos()) {
            String params = s.getCategoria() == Simbolo.Categoria.FUNCION
                    ? s.getParametros().toString()
                    : "-";
            String flags = (s.esConstante() ? "const " : "")
                    + (s.esInicializada() ? "" : "no-inicializada ")
                    + (s.esUsada() ? "" : "no-usada");
            System.out.printf("%-10s %-10s %-10s %-5d %-7d %-10s %-15s %-10s%n",
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
