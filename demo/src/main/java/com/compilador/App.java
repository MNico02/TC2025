package com.compilador;

import com.compilador.semantico.SimbolosListener;
import com.compilador.semantico.TablaSimbolos;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.*;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class App {
    // ANSI colors
    private static final String RESET  = "\u001B[0m";
    private static final String RED    = "\u001B[31m";
    private static final String GREEN  = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE   = "\u001B[34m";
    private static final String CYAN   = "\u001B[36m";

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println(RED + "Uso: java -jar compilador.jar <archivo.txt> [--no-ast]" + RESET);
            System.exit(1);
        }
        String inputPath = args[0];
        boolean showAst  = !Arrays.asList(args).contains("--no-ast");
        String baseName  = getBaseName(inputPath);

        try {
            System.out.println(CYAN + "🚀 Iniciando compilación: " + inputPath + RESET);
            long tStart = System.nanoTime();

            // 1. Léxico
            long t0 = System.nanoTime();
            CommonTokenStream tokens = analizarLexico(inputPath);
            long t1 = System.nanoTime();
            System.out.println(GREEN + "✅ Léxico OK" + RESET +
                    " (" + ms(t1 - t0) + " ms, " + (tokens.size()-1) + " tokens)");

            // 2. Sintáctico
            t0 = System.nanoTime();
            ParseTree tree = analizarSintaxis(tokens);
            long t2 = System.nanoTime();
            System.out.println(GREEN + "✅ Sintaxis OK" + RESET +
                    " (" + ms(t2 - t0) + " ms)");

            // 3. AST
            if (showAst) {
                System.out.println(BLUE + "\n=== Visualización AST ===" + RESET);
                mostrarAST(tree, new MiniLenguajeParser(tokens));
            }

            // 4. Semántico
            t0 = System.nanoTime();
            SimbolosListener sem = analizarSemantica(tree);
            long t3 = System.nanoTime();
            System.out.println(GREEN + "✅ Semántico OK" + RESET +
                    " (" + ms(t3 - t0) + " ms)");

            TablaSimbolos tabla = sem.getTablaSimbolos();
            System.out.println("\n" + BLUE + "📋 Tabla de Símbolos" + RESET);
            tabla.imprimir();

            // 5. Generación de C3D
            t0 = System.nanoTime();
            CodigoVisitor cv = new CodigoVisitor(tabla);
            cv.visit(tree);
            GeneradorCodigo gen = cv.getGenerador();
            long t4 = System.nanoTime();
            System.out.println(GREEN + "✅ Generación de C3D OK" + RESET +
                    " (" + ms(t4 - t0) + " ms)");

            // 6. Imprimir y guardar C3D “raw”
            List<String> codigoRaw = gen.getCodigo();
            System.out.println("\n" + BLUE + "📝 C3D (RAW)" + RESET);
            codigoRaw.forEach((instr) -> System.out.println(instr));
            String rawOut = baseName + "_c3d.txt";
            guardarCodigo(codigoRaw, rawOut);

            // 7. Optimización de C3D
            Optimizador opt = new Optimizador(codigoRaw);
            List<String> codigoOpt = opt.optimizar();

            System.out.println("\n" + BLUE + "🛠️ C3D (OPTIMIZADO)" + RESET);
            codigoOpt.forEach((instr) -> System.out.println(instr));
            String optOut = baseName + "_c3d_opt.txt";
            guardarCodigo(codigoOpt, optOut);

            // 8. Resumen
            long tEnd = System.nanoTime();
            System.out.println("\n" + BLUE + "=== Resumen ===" + RESET);
            System.out.println("Tiempo total: " + ms(tEnd - tStart) + " ms");
            System.out.println("Tokens: " + (tokens.size()-1));
            System.out.println("Instrucciones RAW: " + codigoRaw.size());
            System.out.println("Instrucciones OPT: " + codigoOpt.size());
            System.out.println("Archivo RAW: " + rawOut);
            System.out.println("Archivo OPT: " + optOut);
            System.out.println(GREEN + "\n🎉 ¡COMPILACIÓN EXITOSA! 🎉" + RESET);

        } catch (ParseCancellationException ex) {
            System.err.println(RED + "❌ Error léxico: " + ex.getMessage() + RESET);
        } catch (IllegalArgumentException ex) {
            System.err.println(RED + "❌ Error sintáctico: " + ex.getMessage() + RESET);
        } catch (RuntimeException ex) {
            System.err.println(RED + "❌ Error semántico: " + ex.getMessage() + RESET);
        } catch (Exception ex) {
            System.err.println(RED + "❌ Error inesperado: " + RESET);
            ex.printStackTrace();
        }
    }

    private static CommonTokenStream analizarLexico(String inputPath) throws IOException {
        CharStream input = CharStreams.fromFileName(inputPath);
        MiniLenguajeLexer lexer = new MiniLenguajeLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                  int line, int charPositionInLine, String msg,
                                  RecognitionException e) {
                throw new ParseCancellationException("Línea " + line + ":" + charPositionInLine + " " + msg);
            }
        });
        return new CommonTokenStream(lexer);
    }

    private static ParseTree analizarSintaxis(CommonTokenStream tokens) {
        MiniLenguajeParser parser = new MiniLenguajeParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                  int line, int charPositionInLine, String msg,
                                  RecognitionException e) {
                throw new IllegalArgumentException("Línea " + line + ":" + charPositionInLine + " " + msg);
            }
        });
        return parser.programa();
    }

    private static void mostrarAST(ParseTree tree, MiniLenguajeParser parser) {
        try {
            JFrame frame = new JFrame("AST - " + parser.getClass().getSimpleName());
            TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
            viewer.setScale(1.5);
            frame.add(viewer);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            System.out.println(YELLOW + "⚠️  Ventana AST abierta. Ciérrala para continuar..." + RESET);
        } catch (Exception ex) {
            System.err.println(YELLOW + "⚠️  No se pudo mostrar AST gráfico: " + ex.getMessage() + RESET);
        }
    }

    private static SimbolosListener analizarSemantica(ParseTree tree) {
        SimbolosListener listener = new SimbolosListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);
        return listener;
    }

    private static void guardarCodigo(List<String> codigo, String ruta) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(ruta))) {
            w.write("// Código intermedio de tres direcciones\n\n");
            for (int i = 0; i < codigo.size(); i++) {
                w.write(String.format("%3d: %s%n", i, codigo.get(i)));
            }
        }
        System.out.println(GREEN + "✅ Guardado en " + ruta + RESET);
    }

    private static String getBaseName(String path) {
        String name = new File(path).getName();
        return name.contains(".")
                ? name.substring(0, name.lastIndexOf('.'))
                : name;
    }

    private static long ms(long nano) {
        return Math.round(nano / 1_000_000.0);
    }
}
