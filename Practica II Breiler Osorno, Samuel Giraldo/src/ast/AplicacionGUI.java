package ast;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

// Ventana principal de la aplicacion
public class AplicacionGUI extends JFrame {

    private final JTextArea  textGramatica;
    private final JTextField fieldExpresion;
    private final JRadioButton rbIzquierda, rbDerecha;
    private final JTextArea  textResultados;

    public AplicacionGUI() {
        super("Generador de Arboles Sintacticos (POO) - Java");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 620);
        setLocationRelativeTo(null);

        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        main.setBackground(new Color(245, 247, 250));

        // Seccion gramatica
        main.add(label("1. Ingresa la Gramatica (Puedes usar [a-z], [A-Z], [0-9]):"));
        textGramatica = new JTextArea(7, 70);
        textGramatica.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textGramatica.setText(
                "E -> E '+' T | E '-' T | T\n" +
                "T -> T '*' F | T '/' F | F\n" +
                "F -> '(' E ')' | [a-z] [A-Z] [0-9]"
        );
        main.add(new JScrollPane(textGramatica));
        main.add(Box.createVerticalStrut(8));

        // Seccion expresion
        main.add(label("2. Ingresa la Expresion a evaluar (separada por espacios):"));
        fieldExpresion = new JTextField("( x + s ) * 6", 70);
        fieldExpresion.setFont(new Font("Monospaced", Font.PLAIN, 12));
        main.add(fieldExpresion);
        main.add(Box.createVerticalStrut(8));

        // Tipo de derivacion
        main.add(label("3. Tipo de Derivacion:"));
        rbIzquierda = new JRadioButton("Derivacion por la Izquierda", true);
        rbDerecha   = new JRadioButton("Derivacion por la Derecha",   false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbIzquierda); bg.add(rbDerecha);
        JPanel panelRadios = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelRadios.setOpaque(false);
        panelRadios.add(rbIzquierda);
        panelRadios.add(Box.createHorizontalStrut(20));
        panelRadios.add(rbDerecha);
        main.add(panelRadios);
        main.add(Box.createVerticalStrut(8));

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panelBotones.setOpaque(false);
        panelBotones.add(boton("Mostrar Derivacion",          e -> mostrarDerivacion()));
        panelBotones.add(boton("Generar Arbol de Derivacion", e -> mostrarArbol()));
        panelBotones.add(boton("Generar AST",                 e -> mostrarAST()));
        main.add(panelBotones);
        main.add(Box.createVerticalStrut(8));

        // Panel de resultados
        main.add(label("Resultados:"));
        textResultados = new JTextArea(10, 70);
        textResultados.setFont(new Font("Monospaced", Font.PLAIN, 11));
        textResultados.setEditable(false);
        textResultados.setBackground(new Color(30, 30, 30));
        textResultados.setForeground(new Color(220, 220, 180));
        main.add(new JScrollPane(textResultados));

        setContentPane(new JScrollPane(main));
        setVisible(true);
    }

    // --- helpers de UI ---

    private JLabel label(String txt) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JButton boton(String txt, java.awt.event.ActionListener al) {
        JButton b = new JButton(txt);
        b.addActionListener(al);
        return b;
    }

    private void escribirResultado(String texto) {
        textResultados.setText(texto);
        textResultados.setCaretPosition(0);
    }

    // Obtiene y valida la entrada del usuario
    private record Entrada(AnalizadorGramatical analizador, List<String> tokens) {}

    private Entrada procesarEntrada() {
        String gramaticaStr = textGramatica.getText().trim();
        String expresionStr = fieldExpresion.getText().trim();

        if (gramaticaStr.isEmpty() || expresionStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingresa la gramatica y la expresion.",
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        List<String> tokens = Arrays.asList(expresionStr.split("\\s+"));

        try {
            AnalizadorGramatical analizador = new AnalizadorGramatical(gramaticaStr, tokens);
            return new Entrada(analizador, tokens);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error en la gramatica:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    // --- acciones de los botones ---

    private void mostrarDerivacion() {
        Entrada entrada = procesarEntrada();
        if (entrada == null) return;

        String tipo = rbIzquierda.isSelected() ? "Izquierda" : "Derecha";
        List<String> pasos = entrada.analizador().derivar(entrada.tokens(), tipo);

        StringBuilder sb = new StringBuilder("Derivacion por la " + tipo + ":\n\n");
        for (String p : pasos) sb.append(p).append("\n");
        escribirResultado(sb.toString());
    }

    private void mostrarArbol() {
        Entrada entrada = procesarEntrada();
        if (entrada == null) return;

        AnalizadorGramatical.ParseTreeNode arbol =
                entrada.analizador().construirParseTree(entrada.tokens());

        if (arbol == null) {
            JOptionPane.showMessageDialog(this, "La expresion no es valida para la gramatica dada.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        escribirResultado("Arbol de Derivacion:\n\n" + arbol.toTexto(0));
        ArbolPanel.desdeParseTree(arbol).mostrarEnVentana("Arbol de Derivacion");
    }

    private void mostrarAST() {
        Entrada entrada = procesarEntrada();
        if (entrada == null) return;

        try {
            NodoAST ast = entrada.analizador().construirAST(entrada.tokens());
            escribirResultado("AST Semantico:\n\n" + ast.toTexto(0) +
                    "\n\n(Abriendo visualizacion grafica...)");
            ArbolPanel.desdeAST(ast).mostrarEnVentana("Abstract Syntax Tree (AST)");
        } catch (Parser.ParseException e) {
            JOptionPane.showMessageDialog(this, "Error al construir AST:\n" + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AplicacionGUI::new);
    }
}
