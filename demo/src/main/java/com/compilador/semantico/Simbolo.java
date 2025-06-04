package com.compilador.semantico;

import java.util.ArrayList;
import java.util.List;

public class Simbolo {
    public enum Categoria { VARIABLE, FUNCION, PARAMETRO, CONSTANTE }

    private final String nombre;
    private final String tipo;
    private final Categoria categoria;
    private final int linea;
    private final int columna;
    private final String ambito;
    private final List<String> parametros; // sólo para funciones

    // Campos nuevos:
    private boolean inicializada = false;
    private boolean usada = false;
    private final boolean esConstante;   // si fue declarada como constante
    private final int orden;             // índice para saber en qué orden se declaró

    public Simbolo(String nombre,
                   String tipo,
                   Categoria categoria,
                   int linea,
                   int columna,
                   String ambito,
                   boolean esConstante,
                   int orden) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.linea = linea;
        this.columna = columna;
        this.ambito = ambito;
        this.parametros = new ArrayList<>();
        this.esConstante = esConstante;
        this.orden = orden;
    }

    // Constructor original (esConstante=false, orden=0 inicialmente)
    public Simbolo(String nombre,
                   String tipo,
                   Categoria categoria,
                   int linea,
                   int columna,
                   String ambito) {
        this(nombre, tipo, categoria, linea, columna, ambito, false, -1);
    }

    // Getters y setters:

    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public Categoria getCategoria() { return categoria; }
    public int getLinea() { return linea; }
    public int getColumna() { return columna; }
    public String getAmbito() { return ambito; }
    public List<String> getParametros() { return parametros; }
    public boolean esConstante() { return esConstante; }
    public int getOrden() { return orden; }

    public boolean esInicializada() { return inicializada; }
    public void setInicializada(boolean v) { this.inicializada = v; }

    public boolean esUsada() { return usada; }
    public void setUsada(boolean v) { this.usada = v; }

    public void agregarParametro(String tipoParam) {
        this.parametros.add(tipoParam);
    }

    @Override
    public String toString() {
        String params = categoria == Categoria.FUNCION
                ? parametros.toString()
                : "-";
        return String.format("%-10s %-10s %-10s %-5d %-7d %-10s %s %-8s",
                nombre,
                tipo,
                categoria.name().toLowerCase(),
                linea,
                columna,
                ambito,
                params,
                (esConstante ? "const" : ""));
    }
}
