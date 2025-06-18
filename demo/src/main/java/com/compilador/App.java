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
import java.util.stream.Collectors;

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
        boolean showAst  = Arrays.asList(args).contains("--no-ast") == false;
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

            // 5. Código Intermedio
            t0 = System.nanoTime();
            CodigoVisitor cv = new CodigoVisitor(tabla);
            cv.visit(tree);
            GeneradorCodigo gen = cv.getGenerador();
            long t4 = System.nanoTime();
            System.out.println(GREEN + "✅ Generación de C3D OK" + RESET +
                    " (" + ms(t4 - t0) + " ms)");

            System.out.println("\n" + BLUE + "📝 Código de Tres Direcciones" + RESET);
            gen.imprimirCodigo();

            // 6. Guardar C3D
            String outFile = baseName + "_c3d.txt";
            guardarCodigo(gen.getCodigo(), outFile);

            // 7. Resumen
            long tEnd = System.nanoTime();
            System.out.println("\n" + BLUE + "=== Resumen ===" + RESET);
            System.out.println("Tiempo total: " + ms(tEnd - tStart) + " ms");
            System.out.println("Tokens: " + (tokens.size()-1));
           // System.out.println("Símbolos: " + tabla.size());
            System.out.println("Instrucciones: " + gen.getCodigo().size());
            System.out.println("Archivo C3D: " + outFile);
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

    private static CommonTokenStream analizarLexico(String path) throws IOException {
        CharStream in = CharStreams.fromFileName(path);
        MiniLenguajeLexer lexer = new MiniLenguajeLexer(in);
        List<String> errs = new ArrayList<>();
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener(){
            @Override
            public void syntaxError(Recognizer<?,?> r, Object o, int l, int c, String m, RecognitionException e){
                errs.add("Léxico " + l + ":" + c + " " + m);
                throw new ParseCancellationException(m);
            }
        });
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        if (!errs.isEmpty()) {
            errs.forEach(msg -> { throw new ParseCancellationException(msg); });
        }
        return tokens;
    }

    private static ParseTree analizarSintaxis(CommonTokenStream tokens) {
        MiniLenguajeParser parser = new MiniLenguajeParser(tokens);
        List<String> errs = new ArrayList<>();
        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener(){
            @Override
            public void syntaxError(Recognizer<?,?> r, Object o, int l, int c, String m, RecognitionException e){
                errs.add("Sintaxis " + l + ":" + c + " " + m);
            }
        });
        ParseTree tree = parser.programa();
        if (!errs.isEmpty()) {
            errs.forEach(msg -> System.err.println(RED + msg + RESET));
            throw new IllegalArgumentException("Errores sintácticos detectados");
        }
        return tree;
    }

    private static SimbolosListener analizarSemantica(ParseTree tree) {
        SimbolosListener listener = new SimbolosListener();
        ParseTreeWalker.DEFAULT.walk(listener, tree);
        if (!listener.getErrores().isEmpty()) {
            listener.getErrores().forEach(e -> System.err.println(RED + e + RESET));
            throw new RuntimeException("Errores semánticos detectados");
        }
        if (!listener.getWarnings().isEmpty()) {
            System.out.println(YELLOW + "⚠ Warnings semánticos:" + RESET);
            listener.getWarnings().forEach(w -> System.out.println(YELLOW + w + RESET));
        }
        return listener;
    }

    private static void mostrarAST(ParseTree tree, Parser parser) {
        JFrame frame = new JFrame("AST");
        List<String> names = Arrays.asList(parser.getRuleNames());
        TreeViewer viewer = new TreeViewer(names, tree);
        viewer.setScale(1.2);
        frame.add(new JScrollPane(viewer));
        frame.setSize(800, 600);
        viewer.open(); // no bloquea
    }

    private static void guardarCodigo(List<String> codigo, String ruta) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(ruta))) {
            w.write("// Código intermedio de tres direcciones\n\n");
            for (int i = 0; i < codigo.size(); i++) {
                w.write(String.format("%3d: %s%n", i, codigo.get(i)));
            }
        }
        System.out.println(GREEN + "✅ Guardado C3D en " + ruta + RESET);
    }

    private static String getBaseName(String path) {
        String name = new File(path).getName();
        return name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
    }

    private static long ms(long nano) {
        return Math.round(nano / 1_000_000.0);
    }
}
