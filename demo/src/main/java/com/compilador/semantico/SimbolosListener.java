package com.compilador.semantico;

import com.compilador.MiniLenguajeParser;
import com.compilador.MiniLenguajeParserBaseListener;
import com.compilador.MiniLenguajeParser.*;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

/**
 * Listener semántico mejorado para:
 * - Detectar errores críticos con línea/columna y mensaje detallado.
 * - Detectar warnings no críticos (variables sin usar, sin inicializar, bucles infinitos…).
 */
public class SimbolosListener extends MiniLenguajeParserBaseListener {

    private final TablaSimbolos tabla       = new TablaSimbolos();
    private final List<String> errores      = new ArrayList<>();
    private final List<String> warnings     = new ArrayList<>();

    private final Deque<Void> pilaVacía = new ArrayDeque<>();
    // (la pila de parámetros ya no se usa aquí)
    private String tipoRetornoActual  = null;
    private String nombreFuncionActual = null;

    // ----------------------------------------
    // 1) Declaración de función
    // ----------------------------------------
    @Override
    public void enterDeclaracionFuncion(DeclaracionFuncionContext ctx) {
        String tipoRetorno = ctx.tipo().getText();
        String nombreFun   = ctx.ID().getText();
        Token idToken      = ctx.ID().getSymbol();
        int linea          = idToken.getLine();
        int col            = idToken.getCharPositionInLine();

        // Verificar redeclaración en global
        Simbolo existing = tabla.buscarEnAmbitoExacto("global", nombreFun);
        if (existing != null && existing.getCategoria() == Simbolo.Categoria.FUNCION) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. Función '%s' ya declarada en ámbito global.",
                    linea, col, nombreFun));
            // Aun así, crearemos el símbolo para continuar
        }

        // Agregar función (esConstante=false). orden asignado internamente.
        Simbolo sFun = new Simbolo(
                nombreFun,
                tipoRetorno,
                Simbolo.Categoria.FUNCION,
                linea,
                col,
                "global",
                false,
                -1
        );
        tabla.entrarAmbito("global");
        tabla.agregar(sFun);

        // Entrar a nuevo ámbito de función
        tabla.entrarAmbito(nombreFun);

        tipoRetornoActual   = tipoRetorno;
        nombreFuncionActual = nombreFun;
    }

    @Override
    public void exitDeclaracionFuncion(DeclaracionFuncionContext ctx) {
        // 1) Obtener el símbolo de la función en global
        Simbolo funcion = tabla.buscar(nombreFuncionActual);

        // 2) Registrar parámetros en el símbolo
        if (funcion != null && funcion.getCategoria() == Simbolo.Categoria.FUNCION) {
            if (ctx.parametros() != null) {
                for (ParametroContext pCtx : ctx.parametros().parametro()) {
                    String tipoParam = pCtx.tipo().getText();
                    funcion.agregarParametro(tipoParam);
                }
            }
        }

        // 3) Verificar parámetros no usados
        if (ctx.parametros() != null) {
            for (ParametroContext pCtx : ctx.parametros().parametro()) {
                String nombreParam = pCtx.ID().getText();
                // Reemplazamos buscarEnAmbitoJunto por buscar(...)
                Simbolo paramSymbol = tabla.buscar(nombreParam);
                if (paramSymbol != null && !paramSymbol.esUsada()) {
                    warnings.add(String.format(
                            "[Warning] Línea %d:%d. Parámetro '%s' en función '%s' nunca usado.",
                            paramSymbol.getLinea(),
                            paramSymbol.getColumna(),
                            nombreParam,
                            nombreFuncionActual));
                }
            }
        }

        // 4) Salir del ámbito de la función
        tabla.salirAmbito();
        // 5) Volver a global
        tabla.salirAmbito();

        tipoRetornoActual   = null;
        nombreFuncionActual = null;
    }


    // ----------------------------------------
    // 2) Declaración de variable
    // ----------------------------------------
    @Override
    public void enterDeclaracionVariable(DeclaracionVariableContext ctx) {
        String tipoVar   = ctx.tipo().getText();
        String nombreVar = ctx.ID().getText();
        Token idToken    = ctx.ID().getSymbol();
        int linea        = idToken.getLine();
        int col          = idToken.getCharPositionInLine();
        String ambito    = tabla.getAmbitoActual();

        // Verificar redeclaración en mismo ámbito
        Simbolo existente = tabla.buscarEnAmbitoExacto(ambito, nombreVar);
        if (existente != null) {
            if (!existente.getTipo().equals(tipoVar)) {
                errores.add(String.format(
                        "[Error] Línea %d:%d. Variable '%s' ya declarada con tipo '%s' en ámbito '%s'; no se puede redeclarar como '%s'.",
                        linea, col, nombreVar, existente.getTipo(), ambito, tipoVar));
                return;
            } else {
                warnings.add(String.format(
                        "[Warning] Línea %d:%d. Variable '%s' ya declarada en ámbito '%s'; ignorando redeclaración.",
                        linea, col, nombreVar, ambito));
                return;
            }
        }

        // ¿Viene inicializada?
        boolean inicializada = ctx.expresion() != null;
        Simbolo sVar = new Simbolo(
                nombreVar,
                tipoVar,
                Simbolo.Categoria.VARIABLE,
                linea,
                col,
                ambito,
                false,
                -1
        );
        sVar.setInicializada(inicializada);
        tabla.agregar(sVar);

        if (!inicializada) {
            warnings.add(String.format(
                    "[Warning] Línea %d:%d. Variable '%s' declarada sin inicializar en ámbito '%s'.",
                    linea, col, nombreVar, ambito));
        }
    }

    // ----------------------------------------
    // 3) Declaración en for: similar a declaracionVariable
    // ----------------------------------------
    @Override
    public void enterDeclaracionFor(DeclaracionForContext ctx) {
        String tipoVar   = ctx.tipo().getText();
        String nombreVar = ctx.ID().getText();
        Token idToken    = ctx.ID().getSymbol();
        int linea        = idToken.getLine();
        int col          = idToken.getCharPositionInLine();
        String ambito    = tabla.getAmbitoActual();

        Simbolo existente = tabla.buscarEnAmbitoExacto(ambito, nombreVar);
        if (existente != null) {
            if (!existente.getTipo().equals(tipoVar)) {
                errores.add(String.format(
                        "[Error] Línea %d:%d. Variable '%s' ya declarada con tipo '%s' en ámbito '%s'; no se puede redeclarar como '%s'.",
                        linea, col, nombreVar, existente.getTipo(), ambito, tipoVar));
                return;
            } else {
                warnings.add(String.format(
                        "[Warning] Línea %d:%d. Variable '%s' ya declarada en ámbito '%s'; ignorando redeclaración.",
                        linea, col, nombreVar, ambito));
                return;
            }
        }

        boolean inicializada = ctx.expresion() != null;
        Simbolo sVar = new Simbolo(
                nombreVar,
                tipoVar,
                Simbolo.Categoria.VARIABLE,
                linea,
                col,
                ambito,
                false,
                -1
        );
        sVar.setInicializada(inicializada);
        tabla.agregar(sVar);

        if (!inicializada) {
            warnings.add(String.format(
                    "[Warning] Línea %d:%d. Variable '%s' en for declarada sin inicializar en ámbito '%s'.",
                    linea, col, nombreVar, ambito));
        }
    }

    // ----------------------------------------
    // 4) Uso de identificador simple en expresiones
    // ----------------------------------------
    @Override
    public void enterExpID(ExpIDContext ctx) {
        String nombre = ctx.ID().getText();
        Token t       = ctx.ID().getSymbol();
        int linea     = t.getLine();
        int col       = t.getCharPositionInLine();

        Simbolo s = tabla.buscar(nombre);
        if (s == null) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. Identificador '%s' no declarado.",
                    linea, col, nombre));
            return;
        }

        // Marcar como usado; si no estaba inicializado, warning sólo la primera vez
        if (s.getCategoria() == Simbolo.Categoria.VARIABLE) {
            if (!s.esInicializada() && !s.esUsada()) {
                warnings.add(String.format(
                        "[Warning] Línea %d:%d. Uso de variable '%s' no inicializada.",
                        linea, col, nombre));
            }
        }
        s.setUsada(true);
    }

    // ----------------------------------------
    // 5) Asignaciones
    // ----------------------------------------
    @Override
    public void enterAsignacion(AsignacionContext ctx) {
        String nombre = ctx.ID().getText();
        Token t       = ctx.ID().getSymbol();
        int linea     = t.getLine();
        int col       = t.getCharPositionInLine();

        Simbolo sVar = tabla.buscar(nombre);
        if (sVar == null) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. Asignación a variable '%s' no declarada.",
                    linea, col, nombre));
            return;
        }

        if (sVar.esConstante()) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. No se puede reasignar a la constante '%s'.",
                    linea, col, nombre));
            return;
        }

        String tipoIzq = sVar.getTipo();
        String tipoDer = inferirTipo(ctx.expresion());
        if (tipoDer == null) {
            return; // error ya reportado
        }

        if (!esCompatible(tipoIzq, tipoDer)) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. Tipo mismatched: no se puede asignar '%s' a '%s'.",
                    linea, col, tipoDer, tipoIzq));
            return;
        }

        sVar.setInicializada(true);
        sVar.setUsada(true);
    }

    // ----------------------------------------
    // 6) Llamada a función
    // ----------------------------------------
    @Override
    public void enterLlamada(LlamadaContext ctx) {
        String nombreFun = ctx.ID().getText();
        Token t          = ctx.ID().getSymbol();
        int linea        = t.getLine();
        int col          = t.getCharPositionInLine();

        Simbolo sFun = tabla.buscar(nombreFun);
        if (sFun == null || sFun.getCategoria() != Simbolo.Categoria.FUNCION) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. Función '%s' no declarada.",
                    linea, col, nombreFun));
            return;
        }

        // Detectar llamada antes de la declaración
        if (sFun.getOrden() > getUltimoOrdenDeclarado()) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. Llamada a función '%s' antes de su declaración.",
                    linea, col, nombreFun));
        }

        sFun.setUsada(true);

        List<String> paramsEsperados = sFun.getParametros();
        List<ExpresionContext> paramsActuales =
                ctx.expresion() != null ? ctx.expresion() : List.of();

        if (paramsActuales.size() != paramsEsperados.size()) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. Llamada a '%s' con %d parámetro(s), pero se esperaban %d.",
                    linea, col, nombreFun, paramsActuales.size(), paramsEsperados.size()));
            return;
        }

        for (int i = 0; i < paramsActuales.size(); i++) {
            ExpresionContext expCtx = paramsActuales.get(i);
            String tipoAct = inferirTipo(expCtx);
            String tipoEsp = paramsEsperados.get(i);
            if (tipoAct == null) continue;
            if (!esCompatible(tipoEsp, tipoAct)) {
                Token tParam = expCtx.getStart();
                errores.add(String.format(
                        "[Error] Línea %d:%d. En llamada a '%s', parámetro %d: se esperaba '%s', se encontró '%s'.",
                        tParam.getLine(), tParam.getCharPositionInLine(),
                        nombreFun, i + 1, tipoEsp, tipoAct));
            }
        }
    }

    // ----------------------------------------
    // 7) Return
    // ----------------------------------------
    @Override
    public void enterRetorno(RetornoContext ctx) {
        Token t = ctx.getStart();
        int linea = t.getLine();
        int col   = t.getCharPositionInLine();

        if (tipoRetornoActual == null) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. 'return' fuera de una función.",
                    linea, col));
            return;
        }

        if (tipoRetornoActual.equals("void") && ctx.expresion() != null) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. Función '%s' es void y no debe retornar valor.",
                    linea, col, nombreFuncionActual));
            return;
        }
        if (!tipoRetornoActual.equals("void") && ctx.expresion() == null) {
            errores.add(String.format(
                    "[Error] Línea %d:%d. Función '%s' debe retornar tipo '%s'.",
                    linea, col, nombreFuncionActual, tipoRetornoActual));
            return;
        }
        if (ctx.expresion() != null) {
            String tipoExp = inferirTipo(ctx.expresion());
            if (tipoExp != null && !esCompatible(tipoRetornoActual, tipoExp)) {
                errores.add(String.format(
                        "[Error] Línea %d:%d. No se puede retornar tipo '%s' desde función '%s' que espera '%s'.",
                        linea, col, tipoExp, nombreFuncionActual, tipoRetornoActual));
            }
        }
    }

    @Override
    public void visitErrorNode(ErrorNode node) {
        Token t = ((TerminalNode) node).getSymbol();
        errores.add(String.format(
                "[Error sintáctico] Línea %d:%d. Símbolo inesperado '%s'.",
                t.getLine(), t.getCharPositionInLine(), t.getText()));
    }

    // ----------------------------------------
    // Inferencia de tipo para ExpresionContext
    // ----------------------------------------
    private String inferirTipo(ExpresionContext ctx) {
        if (ctx instanceof ExpEnteroContext) {
            return "int";
        }
        if (ctx instanceof ExpDecimalContext) {
            return "double";
        }
        if (ctx instanceof ExpCaracterContext) {
            return "char";
        }
        if (ctx instanceof ExpIDContext) {
            ExpIDContext idCtx = (ExpIDContext) ctx;
            String nombre = idCtx.ID().getText();
            Token t = idCtx.ID().getSymbol();
            int linea = t.getLine();
            int col   = t.getCharPositionInLine();

            Simbolo s = tabla.buscar(nombre);
            if (s == null) {
                errores.add(String.format(
                        "[Error] Línea %d:%d. Identificador '%s' no declarado.",
                        linea, col, nombre));
                return null;
            }
            if (s.getCategoria() == Simbolo.Categoria.VARIABLE) {
                if (!s.esInicializada() && !s.esUsada()) {
                    warnings.add(String.format(
                            "[Warning] Línea %d:%d. Uso de variable '%s' no inicializada.",
                            linea, col, nombre));
                }
            }
            s.setUsada(true);
            return s.getTipo();
        }
        if (ctx instanceof ExpLlamadaContext) {
            ExpLlamadaContext callCtx = (ExpLlamadaContext) ctx;
            String nombreFun = callCtx.llamada().ID().getText();
            Simbolo sFun = tabla.buscar(nombreFun);
            Token t = callCtx.llamada().ID().getSymbol();
            int linea = t.getLine();
            int col   = t.getCharPositionInLine();
            if (sFun == null || sFun.getCategoria() != Simbolo.Categoria.FUNCION) {
                errores.add(String.format(
                        "[Error] Línea %d:%d. Función '%s' no declarada.",
                        linea, col, nombreFun));
                return null;
            }
            sFun.setUsada(true);
            return sFun.getTipo();
        }
        if (ctx instanceof ExpUnariaContext) {
            return "int"; // boolean ↔ int
        }
        if (ctx instanceof ExpPreIncrementoContext) {
            ExpPreIncrementoContext pre = (ExpPreIncrementoContext) ctx;
            String nombre = pre.ID().getText();
            Token t = pre.ID().getSymbol();
            int linea = t.getLine();
            int col   = t.getCharPositionInLine();
            Simbolo s = tabla.buscar(nombre);
            if (s == null) {
                errores.add(String.format(
                        "[Error] Línea %d:%d. Identificador '%s' no declarado.",
                        linea, col, nombre));
                return null;
            }
            s.setUsada(true);
            return s.getTipo();
        }
        if (ctx instanceof ExpPreDecrementoContext) {
            ExpPreDecrementoContext pre = (ExpPreDecrementoContext) ctx;
            String nombre = pre.ID().getText();
            Token t = pre.ID().getSymbol();
            int linea = t.getLine();
            int col   = t.getCharPositionInLine();
            Simbolo s = tabla.buscar(nombre);
            if (s == null) {
                errores.add(String.format(
                        "[Error] Línea %d:%d. Identificador '%s' no declarado.",
                        linea, col, nombre));
                return null;
            }
            s.setUsada(true);
            return s.getTipo();
        }
        if (ctx instanceof ExpPostIncrementoContext) {
            ExpPostIncrementoContext post = (ExpPostIncrementoContext) ctx;
            String nombre = post.ID().getText();
            Token t = post.ID().getSymbol();
            int linea = t.getLine();
            int col   = t.getCharPositionInLine();
            Simbolo s = tabla.buscar(nombre);
            if (s == null) {
                errores.add(String.format(
                        "[Error] Línea %d:%d. Identificador '%s' no declarado.",
                        linea, col, nombre));
                return null;
            }
            s.setUsada(true);
            return s.getTipo();
        }
        if (ctx instanceof ExpPostDecrementoContext) {
            ExpPostDecrementoContext post = (ExpPostDecrementoContext) ctx;
            String nombre = post.ID().getText();
            Token t = post.ID().getSymbol();
            int linea = t.getLine();
            int col   = t.getCharPositionInLine();
            Simbolo s = tabla.buscar(nombre);
            if (s == null) {
                errores.add(String.format(
                        "[Error] Línea %d:%d. Identificador '%s' no declarado.",
                        linea, col, nombre));
                return null;
            }
            s.setUsada(true);
            return s.getTipo();
        }
        if (ctx instanceof ExpParentesisContext) {
            ExpParentesisContext par = (ExpParentesisContext) ctx;
            return inferirTipo(par.expresion());
        }
        if (ctx instanceof ExpBinariaContext) {
            ExpBinariaContext bin = (ExpBinariaContext) ctx;
            ExpresionContext leftCtx  = bin.expresion(0);
            ExpresionContext rightCtx = bin.expresion(1);

            String tipoL = inferirTipo(leftCtx);
            String tipoR = inferirTipo(rightCtx);
            if (tipoL == null || tipoR == null) return null;

            String op = bin.operadorBinario().getText();
            Token tOp  = bin.operadorBinario().getStart();

            if (tipoL.equals("double") || tipoR.equals("double")) {
                return "double";
            }
            if (tipoL.equals("int") && tipoR.equals("int")) {
                return "int";
            }
            if (tipoL.equals("char") && tipoR.equals("char") &&
                    (op.equals("==") || op.equals("!="))) {
                return "int";
            }

            errores.add(String.format(
                    "[Error] Línea %d:%d. Operación '%s' no permitida entre '%s' y '%s'.",
                    tOp.getLine(), tOp.getCharPositionInLine(), op, tipoL, tipoR));
            return null;
        }
        return null; // caso por defecto
    }

    // ----------------------------------------
    // Compatibilidad de tipos
    // ----------------------------------------
    private boolean esCompatible(String dest, String source) {
        if (dest.equals(source)) return true;
        if (dest.equals("double") &&
                (source.equals("int") || source.equals("char") || source.equals("double"))) {
            return true;
        }
        return false;
    }

    // ----------------------------------------
    // Obtener último orden declarado en la tabla
    // ----------------------------------------
    private int getUltimoOrdenDeclarado() {
        List<Simbolo> todos = tabla.getTodosLosSimbolos();
        if (todos.isEmpty()) return -1;
        return todos.get(todos.size() - 1).getOrden();
    }

    // ----------------------------------------
    // 8) Al salir del programa: verificar usos y bucles infinitos
    // ----------------------------------------
    @Override
    public void exitPrograma(ProgramaContext ctx) {
        // 8.1) Variables y funciones no usadas
        for (Simbolo s : tabla.getTodosLosSimbolos()) {
            if (!s.esUsada()) {
                if (s.getCategoria() == Simbolo.Categoria.VARIABLE) {
                    warnings.add(String.format(
                            "[Warning] Línea %d:%d. Variable '%s' declarada pero nunca usada.",
                            s.getLinea(),
                            s.getColumna(),
                            s.getNombre()));
                }
                if (s.getCategoria() == Simbolo.Categoria.FUNCION &&
                        !s.getNombre().equals("main"))    // filtrar main
                {
                    warnings.add(String.format(
                            "[Warning] Línea %d:%d. Función '%s' declarada pero nunca llamada.",
                            s.getLinea(),
                            s.getColumna(),
                            s.getNombre()));
                }
            }
        }

        // 8.2) Detectar bucles potencialmente infinitos
        verificarBucleInfinito(ctx);
    }

    private void verificarBucleInfinito(ProgramaContext ctx) {
        if (ctx.sentencia() != null) {
            for (SentenciaContext sCtx : ctx.sentencia()) {
                // Revisar while
                if (sCtx.whileStmt() != null) {
                    WhileStmtContext w = sCtx.whileStmt();
                    ExpresionContext cond = w.expresion();
                    if (cond instanceof ExpEnteroContext) {
                        String val = ((ExpEnteroContext) cond).INTEGER().getText();
                        if (val.equals("1")) {
                            warnings.add(String.format(
                                    "[Warning] Línea %d:%d. Bucle 'while' potencialmente infinito.",
                                    cond.getStart().getLine(),
                                    cond.getStart().getCharPositionInLine()));
                        }
                    }
                }
                // Revisar for
                if (sCtx.forStmt() != null) {
                    ForStmtContext f = sCtx.forStmt();
                    boolean initVacio = f.forInit().getText().isEmpty();
                    boolean condVacia = f.expresion().getText().isEmpty();
                    boolean updVacia  = f.actualizacionFor().getText().isEmpty();

                    if (initVacio && condVacia && updVacia) {
                        Token tFor = f.getStart();
                        warnings.add(String.format(
                                "[Warning] Línea %d:%d. Bucle 'for' potencialmente infinito.",
                                tFor.getLine(),
                                tFor.getCharPositionInLine()));
                    }
                }
            }
        }
    }

    // ----------------------------------------
    // Getters para App.java
    // ----------------------------------------
    public TablaSimbolos getTablaSimbolos() {
        return tabla;
    }

    public List<String> getErrores() {
        return errores;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
