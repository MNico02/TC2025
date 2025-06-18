package com.compilador;

//import com.compilador.semantico.SimbolosListener;
//import com.compilador.semantico.TablaSimbolos;
import org.antlr.v4.runtime.*;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.tree.*;


import javax.swing.JFrame;
import javax.swing.JScrollPane;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class App {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Uso: java -jar demo.jar <archivo>");
            return;
        }

        String archivo = args[0];
        InputStream is = new FileInputStream(archivo);
        CharStream input = CharStreams.fromStream(is);

        // 1) Crear Lexer y TokenStream
        MiniLenguajeLexer lexer = new MiniLenguajeLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();

        // 2) Mostrar tabla de tokens (opcional)
        System.out.printf("%-20s %-25s %-10s %-10s%n", "TIPO", "LEXEMA", "LÍNEA", "COLUMNA");
        System.out.println("-------------------------------------------------------------------");
        for (Token token : tokens.getTokens()) {
            String tipo = lexer.getVocabulary().getSymbolicName(token.getType());
            if (tipo != null) {
                System.out.printf("%-20s %-25s %-10d %-10d%n",
                        tipo, token.getText(), token.getLine(), token.getCharPositionInLine());
            }
        }

        // 3) Crear Parser y generar AST
        MiniLenguajeParser parser = new MiniLenguajeParser(tokens);
        ParseTree tree = parser.programa();


            List<String> reglaNombres = Arrays.asList(parser.getRuleNames());
            TreeViewer tv = new TreeViewer(reglaNombres, tree);
            JFrame frame = new JFrame("Árbol de Sintaxis");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new JScrollPane(tv));
            frame.setSize(800, 600);
            frame.setVisible(true);

        // 4) Invocar el listener semántico
        SimbolosListener listener = new SimbolosListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, tree);

        // 5) Obtener y mostrar errores y warnings semánticos
        List<String> errores  = listener.getErrores();
        List<String> warnings = listener.getWarnings();

        if (!errores.isEmpty()) {
            System.err.println("\n=== ERRORES SEMÁNTICOS ===");
            errores.forEach(System.err::println);
        }
        if (!warnings.isEmpty()) {
            System.out.println("\n=== WARNINGS SEMÁNTICOS ===");
            warnings.forEach(System.out::println);
        }

        // 6) Si no hay errores, mostrar la tabla de símbolos
        if (errores.isEmpty()) {
            TablaSimbolos tabla = listener.getTablaSimbolos();
            tabla.imprimir();
        }
    }
}
