package com.compilador;

import com.compilador.semantico.TablaSimbolos;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import java.util.List;

/**
 * Visitor que recorre el AST y, apoyándose en {@link GeneradorCodigo},
 * produce código intermedio de tres direcciones (C3D).
 * <p>
 * Las novedades respecto de la versión anterior son:
 * <ul>
 *     <li>Soporte para declaraciones de variables con inicialización en la misma línea
 *     (por ejemplo: <code>int y = 10;</code>).</li>
 *     <li>Soporte para la variante usada en la cabecera de un <code>for</code>.</li>
 *     <li>Se mantiene compatibilidad con el resto de las construcciones ya procesadas.</li>
 * </ul>
 */
public class CodigoVisitor extends MiniLenguajeParserBaseVisitor<String> {
    private final GeneradorCodigo gen = new GeneradorCodigo();
    private final TablaSimbolos tabla;

    public CodigoVisitor(TablaSimbolos tabla) {
        this.tabla = tabla;
        System.out.println("🎯 VISITOR: Iniciado con tabla de símbolos");
    }

    public GeneradorCodigo getGenerador() {
        return gen;
    }

    // ------------------------------------------------------------
    // Programa y funciones
    // ------------------------------------------------------------

    @Override
    public String visitPrograma(MiniLenguajeParser.ProgramaContext ctx) {
        for (MiniLenguajeParser.DeclaracionFuncionContext fCtx : ctx.declaracionFuncion()) {
            visit(fCtx);
        }
        return null;
    }

    @Override
    public String visitDeclaracionFuncion(MiniLenguajeParser.DeclaracionFuncionContext ctx) {
        String fn = ctx.ID().getText();
        System.out.println("🎯 VISITOR: Encontré función -> " + fn);
        gen.genLabel("func_" + fn);
        visit(ctx.bloque());
        return null;
    }

    // ------------------------------------------------------------
    // Declaraciones de variables (¡novedad!)
    // ------------------------------------------------------------

    /**
     * Maneja sentencias del tipo:
     * <pre>
     *     int y = 10;
     *     double z;
     * </pre>
     * Si la declaración incluye una expresión de inicialización generamos una
     * asignación explícita en C3D.  De esta forma el backend no depende de
     * que el front‑end "recuerde" el valor inicial.
     */
    @Override
    public String visitDeclaracionVariable(MiniLenguajeParser.DeclaracionVariableContext ctx) {
        // Caso: sólo se declara (sin expresión) → no se genera código
        if (ctx.expresion() == null) {
            return null;
        }

        String nombre = ctx.ID().getText();
        String valor  = visit(ctx.expresion()); // la expresión ya recursó y dejó C3D listo

        System.out.println("🎯 VISITOR: Declaración con init -> " + nombre + " = " + valor);
        gen.genAsignacion(nombre, valor);
        return null;
    }

    /**
     * Variante específica usada en la sección de inicialización de un <code>for</code>:
     * <pre>
     *     for (int i = 0; i < 10; i++) { ... }
     * </pre>
     */
    @Override
    public String visitDeclaracionFor(MiniLenguajeParser.DeclaracionForContext ctx) {
        // Si no hay expresión no hay código.
        if (ctx.expresion() == null) {
            return null;
        }
        String nombre = ctx.ID().getText();
        String valor  = visit(ctx.expresion());

        System.out.println("🎯 VISITOR: Declaración‑for con init -> " + nombre + " = " + valor);
        gen.genAsignacion(nombre, valor);
        return null;
    }

    // ------------------------------------------------------------
    // Asignaciones y sentencias de control
    // ------------------------------------------------------------

    @Override
    public String visitAsignacion(MiniLenguajeParser.AsignacionContext ctx) {
        String var = ctx.ID().getText();
        String val = visit(ctx.expresion());
        System.out.println("🎯 VISITOR: Generando asignación -> " + var + " = " + val);
        gen.genAsignacion(var, val);
        return null;
    }

    @Override
    public String visitIfStmt(MiniLenguajeParser.IfStmtContext ctx) {
        System.out.println("🎯 VISITOR: Encontré sentencia IF");
        String cond  = visit(ctx.expresion());
        String elseL = gen.newLabel();
        String endL  = gen.newLabel();

        gen.genIfFalse(cond, elseL);
        visit(ctx.bloque(0));            // bloque del IF
        gen.genGoto(endL);
        gen.genLabel(elseL);

        if (ctx.ELSE() != null) {
            visit(ctx.bloque(1));        // bloque del ELSE
        }
        gen.genLabel(endL);
        return null;
    }

    @Override
    public String visitWhileStmt(MiniLenguajeParser.WhileStmtContext ctx) {
        System.out.println("🎯 VISITOR: Encontré sentencia WHILE");
        String startL = gen.newLabel();
        String endL   = gen.newLabel();
        gen.genLabel(startL);

        String cond = visit(ctx.expresion());
        gen.genIfFalse(cond, endL);

        visit(ctx.bloque());
        gen.genGoto(startL);
        gen.genLabel(endL);
        return null;
    }

    @Override
    public String visitForStmt(MiniLenguajeParser.ForStmtContext ctx) {
        System.out.println("🎯 VISITOR: Encontré sentencia FOR");
        String startL = gen.newLabel();
        String endL   = gen.newLabel();

        // inicialización (puede ser asignación o declaración)
        visit(ctx.forInit());
        gen.genLabel(startL);

        // condición
        String cond = visit(ctx.expresion());
        gen.genIfFalse(cond, endL);

        // cuerpo
        visit(ctx.bloque());

        // actualización
        visit(ctx.actualizacionFor());
        gen.genGoto(startL);
        gen.genLabel(endL);
        return null;
    }

    // ------------------------------------------------------------
    // Expresiones
    // ------------------------------------------------------------

    @Override
    public String visitExpBinaria(MiniLenguajeParser.ExpBinariaContext ctx) {
        String op    = ctx.operadorBinario().getText();
        String left  = visit(ctx.expresion(0));
        String right = visit(ctx.expresion(1));
        return gen.genOperacionBinaria(op, left, right);
    }

    @Override
    public String visitExpID(MiniLenguajeParser.ExpIDContext ctx) {
        return ctx.ID().getText();
    }

    @Override
    public String visitExpEntero(MiniLenguajeParser.ExpEnteroContext ctx) {
        return ctx.INTEGER().getText();
    }

    @Override
    public String visitExpParentesis(MiniLenguajeParser.ExpParentesisContext ctx) {
        return visit(ctx.expresion());
    }

    // ------------------------------------------------------------
    // Return y llamadas
    // ------------------------------------------------------------

    @Override
    public String visitRetorno(MiniLenguajeParser.RetornoContext ctx) {
        System.out.println("🎯 VISITOR: Encontré RETURN");
        if (ctx.expresion() != null) {
            String v = visit(ctx.expresion());
            gen.genAsignacion("return", v);
        } else {
            gen.genAsignacion("return", "");
        }
        return null;
    }

    @Override
    public String visitLlamada(MiniLenguajeParser.LlamadaContext ctx) {
        System.out.println("🎯 VISITOR: Encontré LLAMADA a función");
        String fn = ctx.ID().getText();
        List<MiniLenguajeParser.ExpresionContext> args = ctx.expresion();
        for (MiniLenguajeParser.ExpresionContext e : args) {
            String aVal = visit(e);
            gen.genAsignacion("param", aVal);
        }
        String temp = gen.newTemp();
        // Aquí se podría generar una instrucción específica de llamada "call" si se necesitara
        System.out.printf("🔧 GENERADOR: Generando llamada %s -> %s%n", fn, temp);
        return temp;
    }
}
