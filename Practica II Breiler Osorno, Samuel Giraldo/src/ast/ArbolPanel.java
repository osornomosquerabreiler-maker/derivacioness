package ast;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;

// Panel Swing que dibuja un arbol (parse tree o AST) con nodos y aristas
public class ArbolPanel extends JPanel {

    // Nodo de layout interno con posicion calculada
    private static class NodoLayout {
        final String etiqueta;
        final List<NodoLayout> hijos = new ArrayList<>();
        double x, y;
        double xFinal, yFinal;

        NodoLayout(String etiqueta) { this.etiqueta = etiqueta; }
    }

    // Dimensiones y colores
    private static final int NODE_W  = 54;
    private static final int NODE_H  = 30;
    private static final int H_GAP   = 18;
    private static final int V_GAP   = 55;
    private static final int MARGIN  = 40;

    private static final Color COLOR_BG   = new Color(245, 247, 250);
    private static final Color COLOR_NODE = new Color(66, 133, 244);
    private static final Color COLOR_LEAF = new Color(52, 168, 83);
    private static final Color COLOR_EDGE = new Color(120, 120, 140);
    private static final Color COLOR_TEXT = Color.WHITE;
    private static final Font  FONT_NODE  = new Font("SansSerif", Font.BOLD, 13);

    private NodoLayout raiz;
    private int totalAncho, totalAlto;

    // Crea el panel desde un parse tree
    public static ArbolPanel desdeParseTree(AnalizadorGramatical.ParseTreeNode nodo) {
        ArbolPanel p = new ArbolPanel();
        p.raiz = p.convertirParseTree(nodo);
        p.calcularLayout();
        return p;
    }

    // Crea el panel desde un AST semantico
    public static ArbolPanel desdeAST(NodoAST nodo) {
        ArbolPanel p = new ArbolPanel();
        p.raiz = p.convertirAST(nodo);
        p.calcularLayout();
        return p;
    }

    // Convierte ParseTreeNode a NodoLayout
    private NodoLayout convertirParseTree(AnalizadorGramatical.ParseTreeNode src) {
        NodoLayout n = new NodoLayout(src.etiqueta);
        for (AnalizadorGramatical.ParseTreeNode h : src.hijos)
            n.hijos.add(convertirParseTree(h));
        return n;
    }

    // Convierte NodoAST a NodoLayout
    private NodoLayout convertirAST(NodoAST src) {
        if (src instanceof Identifier id) return new NodoLayout(id.getNombre());
        if (src instanceof BinaryExpression be) {
            NodoLayout n = new NodoLayout(be.getOperador());
            n.hijos.add(convertirAST(be.getIzquierda()));
            n.hijos.add(convertirAST(be.getDerecha()));
            return n;
        }
        return new NodoLayout("?");
    }

    // Calcula posiciones x/y para cada nodo
    private double[] contadorHoja = {0};

    private void calcularLayout() {
        contadorHoja[0] = 0;
        asignarX(raiz);
        asignarY(raiz, 0);

        double[] minX = {Double.MAX_VALUE}, maxX = {Double.MIN_VALUE}, maxY = {0};
        recorrer(raiz, n -> {
            if (n.x < minX[0]) minX[0] = n.x;
            if (n.x > maxX[0]) maxX[0] = n.x;
            if (n.y > maxY[0]) maxY[0] = n.y;
        });

        double escalaX = NODE_W + H_GAP;
        totalAncho = (int)((maxX[0] - minX[0]) * escalaX) + NODE_W + MARGIN * 2;
        totalAlto  = (int)(maxY[0] * V_GAP) + NODE_H + MARGIN * 2;

        recorrer(raiz, n -> {
            n.xFinal = (n.x - minX[0]) * escalaX + MARGIN;
            n.yFinal = n.y * V_GAP + MARGIN;
        });

        setPreferredSize(new Dimension(totalAncho, totalAlto));
    }

    // Las hojas toman posicion secuencial; los internos quedan centrados
    private double asignarX(NodoLayout n) {
        if (n.hijos.isEmpty()) { n.x = contadorHoja[0]++; return n.x; }
        double sum = 0;
        for (NodoLayout h : n.hijos) sum += asignarX(h);
        n.x = sum / n.hijos.size();
        return n.x;
    }

    private void asignarY(NodoLayout n, int nivel) {
        n.y = nivel;
        for (NodoLayout h : n.hijos) asignarY(h, nivel + 1);
    }

    @FunctionalInterface interface NodoConsumer { void accept(NodoLayout n); }

    private void recorrer(NodoLayout n, NodoConsumer fn) {
        fn.accept(n);
        for (NodoLayout h : n.hijos) recorrer(h, fn);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(COLOR_BG);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (raiz == null) return;

        // Dibuja aristas primero, nodos encima
        g2.setColor(COLOR_EDGE);
        g2.setStroke(new BasicStroke(1.8f));
        dibujarAristas(g2, raiz);
        dibujarNodos(g2, raiz);
    }

    private void dibujarAristas(Graphics2D g2, NodoLayout n) {
        int cx = (int)(n.xFinal + NODE_W / 2.0);
        int cy = (int)(n.yFinal + NODE_H / 2.0);
        for (NodoLayout h : n.hijos) {
            int hx = (int)(h.xFinal + NODE_W / 2.0);
            int hy = (int)(h.yFinal + NODE_H / 2.0);
            g2.drawLine(cx, cy + NODE_H / 2, hx, hy - NODE_H / 2);
            dibujarAristas(g2, h);
        }
    }

    private void dibujarNodos(Graphics2D g2, NodoLayout n) {
        boolean esHoja = n.hijos.isEmpty();
        int x = (int) n.xFinal;
        int y = (int) n.yFinal;

        // Sombra
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillRoundRect(x + 3, y + 3, NODE_W, NODE_H, 14, 14);

        // Fondo: azul para nodos internos, verde para hojas
        g2.setColor(esHoja ? COLOR_LEAF : COLOR_NODE);
        g2.fillRoundRect(x, y, NODE_W, NODE_H, 14, 14);

        g2.setColor(esHoja ? COLOR_LEAF.darker() : COLOR_NODE.darker());
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, NODE_W, NODE_H, 14, 14);

        // Texto centrado
        g2.setFont(FONT_NODE);
        g2.setColor(COLOR_TEXT);
        FontMetrics fm = g2.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(n.etiqueta, g2);
        g2.drawString(n.etiqueta,
                x + (NODE_W - (int) r.getWidth()) / 2,
                y + (NODE_H - (int) r.getHeight()) / 2 + fm.getAscent());

        for (NodoLayout h : n.hijos) dibujarNodos(g2, h);
    }

    // Abre el panel en una ventana con scroll
    public void mostrarEnVentana(String titulo) {
        JFrame frame = new JFrame(titulo);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JScrollPane scroll = new JScrollPane(this);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getHorizontalScrollBar().setUnitIncrement(16);
        frame.add(scroll);
        frame.setSize(Math.min(totalAncho + 40, 900), Math.min(totalAlto + 60, 650));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
