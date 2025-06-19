package com.compilador;

import java.util.ArrayList;
import java.util.List;

/**
 * Constructor de código intermedio de 3 direcciones:
 * • Crea temporales (t0, t1, …)
 * • Crea etiquetas (L0, L1, …)
 * • Almacena y luego imprime las instrucciones
 */
public class GeneradorCodigo {
    private final List<String> codigo = new ArrayList<>();
    private int tempCounter  = 0;
    private int labelCounter = 0;

    public GeneradorCodigo() {
        System.out.println("🔧 GENERADOR: Iniciado");
    }

    /** Nuevo temporal tN */
    public String newTemp() {
        String t = "t" + tempCounter++;
        return t;
    }

    /** Nueva etiqueta LN */
    public String newLabel() {
        String L = "L" + labelCounter++;
        return L;
    }

    /**  t = left op right */
    public String genOperacionBinaria(String op, String left, String right) {
        String t = newTemp();
        String inst = t + " = " + left + " " + op + " " + right;
        codigo.add(inst);
        return t;
    }

    /**  x = valor */
    public void genAsignacion(String x, String valor) {
        codigo.add(x + " = " + valor);
    }

    /**  label: */
    public void genLabel(String label) {
        codigo.add(label + ":");
    }

    /**  if !cond goto label */
    public void genIfFalse(String cond, String label) {
        codigo.add("if !" + cond + " goto " + label);
    }

    /**  goto label */
    public void genGoto(String label) {
        codigo.add("goto " + label);
    }

    /** Imprime el código generado */
    public void imprimirCodigo() {
        System.out.println("\n📝 === CÓDIGO DE TRES DIRECCIONES ===");
        for (int i = 0; i < codigo.size(); i++) {
            System.out.printf("%3d: %s%n", i, codigo.get(i));
        }
    }

    public List<String> getCodigo() {
        return codigo;
    }
}