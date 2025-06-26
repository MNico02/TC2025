package com.compilador;

import java.util.*;

/**
 * Optimizador de código de tres direcciones
 */
public class Optimizador {

    private List<String> codigo;

    public Optimizador(List<String> codigo) {
        // Hacemos copia defensiva
        this.codigo = new ArrayList<>(codigo);
    }

    /** Ejecuta todas las pasadas de optimización */
    public List<String> optimizar() {
        eliminarCodigoMuerto();
        propagarConstantes();
        simplificarExpresiones();
        eliminarSentenciasRedundantes();
        return codigo;
    }

    /** 1) Eliminación de código muerto */
    private void eliminarCodigoMuerto() {
        Set<Integer> alcanzables = new HashSet<>();
        Map<String,Integer> etiquetas = new HashMap<>();
        // Registrar etiquetas
        for(int i=0; i<codigo.size(); i++){
            String ln = codigo.get(i);
            if(ln.endsWith(":")){
                etiquetas.put(ln.substring(0, ln.length()-1), i);
            }
        }
        // Marcar desde inicio
        marcar(0, alcanzables, etiquetas);
        // Si existe func_main, también partir de ahí
        if(etiquetas.containsKey("func_main")){
            marcar(etiquetas.get("func_main"), alcanzables, etiquetas);
        }
        // Filtrar
        List<String> nuevo = new ArrayList<>();
        for(int i=0; i<codigo.size(); i++){
            if(alcanzables.contains(i)) nuevo.add(codigo.get(i));
        }
        codigo = nuevo;
    }
    private void marcar(int i, Set<Integer> vis, Map<String,Integer> et) {
        if(i<0||i>=codigo.size()||vis.contains(i)) return;
        vis.add(i);
        String instr = codigo.get(i);
        if(instr.startsWith("goto ")){
            String lbl = instr.substring(5);
            if(et.containsKey(lbl)) marcar(et.get(lbl), vis, et);
            return;
        }
        if(instr.startsWith("if ")){
            String[] p = instr.split(" goto ");
            if(p.length==2 && et.containsKey(p[1])) marcar(et.get(p[1]), vis, et);
        }
        if(instr.equals("return")||instr.startsWith("return ")) return;
        marcar(i+1, vis, et);
    }

    /** 2) Propagación de constantes */
    private void propagarConstantes() {
        Map<String,String> consts = new HashMap<>();
        List<String> out = new ArrayList<>();
        for(String ln : codigo){
            // asignación simple a literal?
            if(ln.matches("\\s*\\w+\\s*=\\s*[-]?\\d+(\\.\\d+)?\\s*")){
                String[] ps = ln.split("=");
                String dest = ps[0].trim(), val = ps[1].trim();
                consts.put(dest, val);
                out.add(ln);
                continue;
            }
            // reemplazar operandos
            String mod = ln;
            for(Map.Entry<String,String> e: consts.entrySet()){
                mod = mod.replaceAll("\\b"+e.getKey()+"\\b", e.getValue());
            }
            out.add(mod);
        }
        codigo = out;
    }

    /** 3) Simplificación de expresiones constantes  (e.g. 2+3->5) */
    private void simplificarExpresiones() {
        List<String> out = new ArrayList<>();
        for(String ln: codigo){
            if(ln.contains("=") && ln.matches(".*=\\s*[-]?\\d+\\s*[+\\-*/%]\\s*[-]?\\d+.*")){
                String[] ps = ln.split("=");
                String dest = ps[0].trim(), expr = ps[1].trim();
                String[] tk = expr.split("\\s+");
                try {
                    int a = Integer.parseInt(tk[0]), b = Integer.parseInt(tk[2]);
                    int r=0;
                    switch(tk[1]){
                        case "+": r=a+b; break;
                        case "-": r=a-b; break;
                        case "*": r=a*b; break;
                        case "/": r=b!=0? a/b : null; break;
                        case "%": r=b!=0? a%b : null; break;
                    }
                    if(tk[1].equals("/")||tk[1].equals("%")){
                        if(b==0){ out.add(ln); continue; }
                    }
                    out.add(dest + " = " + r);
                    continue;
                } catch(Exception ex){}
            }
            out.add(ln);
        }
        codigo = out;
    }

    /** 4) Eliminación de asignaciones redundantes (a = a) */
    private void eliminarSentenciasRedundantes() {
        List<String> out = new ArrayList<>();
        for(String ln: codigo){
            if(ln.matches("\\s*(\\w+)\\s*=\\s*\\1\\s*;?")) {
                continue;
            }
            out.add(ln);
        }
        codigo = out;
    }
}
