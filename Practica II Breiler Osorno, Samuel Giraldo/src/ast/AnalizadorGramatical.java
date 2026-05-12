package ast;

import java.util.*;
import java.util.regex.*;

// Maneja la gramatica, genera derivaciones y construye parse tree y AST
public class AnalizadorGramatical {

    // Una produccion: LHS -> rhs
    public record Produccion(String lhs, List<String> rhs) {
        @Override public String toString() {
            return lhs + " -> " + String.join(" ", rhs);
        }
    }

    // Nodo del arbol de derivacion (parse tree)
    public static class ParseTreeNode {
        public final String etiqueta;
        public final List<ParseTreeNode> hijos = new ArrayList<>();

        public ParseTreeNode(String etiqueta) { this.etiqueta = etiqueta; }

        public boolean esHoja() { return hijos.isEmpty(); }

        public String toTexto(int indent) {
            StringBuilder sb = new StringBuilder();
            sb.append("  ".repeat(indent)).append("[").append(etiqueta).append("]\n");
            for (ParseTreeNode h : hijos) sb.append(h.toTexto(indent + 1));
            return sb.toString();
        }
    }

    private final Map<String, List<List<String>>> reglas = new LinkedHashMap<>();
    private String simboloInicial;
    private final Set<String> noTerminales = new LinkedHashSet<>();
    private final List<String> patronesRangos = new ArrayList<>();

    // Constructor: recibe el texto de la gramatica y los tokens de la expresion
    public AnalizadorGramatical(String textoGramatica, List<String> tokensExpresion) {
        // Extrae rangos como [a-z] antes de expandir
        Matcher m = Pattern.compile("\\[.*?\\]").matcher(textoGramatica);
        while (m.find()) patronesRangos.add(m.group());

        String gramaticaExpandida = expandirRangos(textoGramatica, tokensExpresion);

        for (String linea : gramaticaExpandida.split("\n")) {
            linea = linea.trim();
            if (linea.isEmpty()) continue;

            String[] partes = linea.split("->", 2);
            if (partes.length < 2) throw new IllegalArgumentException("Linea invalida: " + linea);

            String lhs = partes[0].trim();
            if (simboloInicial == null) simboloInicial = lhs;
            noTerminales.add(lhs);

            reglas.putIfAbsent(lhs, new ArrayList<>());
            for (String alt : partes[1].split("\\|")) {
                reglas.get(lhs).add(tokenizarRHS(alt.trim()));
            }
        }
    }

    // Separa el lado derecho de una produccion en simbolos
    private List<String> tokenizarRHS(String rhs) {
        List<String> result = new ArrayList<>();
        Matcher m = Pattern.compile("'([^']*)'|([A-Z][A-Z0-9]*)").matcher(rhs);
        while (m.find()) {
            if (m.group(1) != null) result.add(m.group(1));
            else result.add(m.group(2));
        }
        return result;
    }

    // Reemplaza [a-z] por los terminales reales encontrados en la expresion
    private String expandirRangos(String gramatica, List<String> tokens) {
        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile("\\[(.*?)\\]").matcher(gramatica);
        while (m.find()) {
            String rango = "[" + m.group(1) + "]";
            Pattern pat = Pattern.compile("^" + rango + "$");
            List<String> coincidencias = new ArrayList<>();
            for (String t : tokens) if (pat.matcher(t).matches()) coincidencias.add(t);

            String reemplazo;
            if (coincidencias.isEmpty()) {
                reemplazo = "'__NONE__'";
            } else {
                List<String> unicos = new ArrayList<>(new LinkedHashSet<>(coincidencias));
                StringBuilder rep = new StringBuilder();
                for (int i = 0; i < unicos.size(); i++) {
                    if (i > 0) rep.append(" | ");
                    rep.append("'").append(unicos.get(i)).append("'");
                }
                reemplazo = rep.toString();
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(reemplazo));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // Genera los pasos de derivacion izquierda o derecha
    public List<String> derivar(List<String> tokens, String tipo) {
        ParseTreeNode arbol = construirParseTree(tokens);
        if (arbol == null) return List.of("(La expresion no es valida para esta gramatica)");

        List<Produccion> producciones = tipo.equals("Izquierda")
                ? recolectarProduccionesIzq(arbol)
                : recolectarProduccionesDer(arbol);

        List<String> pasos = new ArrayList<>();
        List<String> cadena = new ArrayList<>();
        cadena.add(simboloInicial);
        pasos.add("=> " + simboloInicial);

        for (Produccion p : producciones) {
            if (tipo.equals("Izquierda")) {
                for (int i = 0; i < cadena.size(); i++) {
                    if (cadena.get(i).equals(p.lhs())) {
                        cadena.remove(i);
                        cadena.addAll(i, p.rhs());
                        break;
                    }
                }
            } else {
                for (int i = cadena.size() - 1; i >= 0; i--) {
                    if (cadena.get(i).equals(p.lhs())) {
                        cadena.remove(i);
                        cadena.addAll(i, p.rhs());
                        break;
                    }
                }
            }
            pasos.add(String.format("=> %-30s (Regla: %s)", String.join(" ", cadena), p));
        }
        return pasos;
    }

    // Recorre el arbol en preorden (izquierda primero)
    private List<Produccion> recolectarProduccionesIzq(ParseTreeNode nodo) {
        List<Produccion> lista = new ArrayList<>();
        if (nodo.esHoja()) return lista;
        List<String> rhs = new ArrayList<>();
        for (ParseTreeNode h : nodo.hijos) rhs.add(h.etiqueta);
        lista.add(new Produccion(nodo.etiqueta, rhs));
        for (ParseTreeNode h : nodo.hijos) lista.addAll(recolectarProduccionesIzq(h));
        return lista;
    }

    // Recorre el arbol en preorden (derecha primero)
    private List<Produccion> recolectarProduccionesDer(ParseTreeNode nodo) {
        List<Produccion> lista = new ArrayList<>();
        if (nodo.esHoja()) return lista;
        List<String> rhs = new ArrayList<>();
        for (ParseTreeNode h : nodo.hijos) rhs.add(h.etiqueta);
        lista.add(new Produccion(nodo.etiqueta, rhs));
        List<ParseTreeNode> rev = new ArrayList<>(nodo.hijos);
        Collections.reverse(rev);
        for (ParseTreeNode h : rev) lista.addAll(recolectarProduccionesDer(h));
        return lista;
    }

    // Construye el parse tree usando un parser LL(1) interno
    public ParseTreeNode construirParseTree(List<String> tokens) {
        TreeParser tp = new TreeParser(tokens, patronesRangos);
        try {
            ParseTreeNode raiz = tp.parseE();
            if (tp.pos < tokens.size()) return null;
            return raiz;
        } catch (Exception e) {
            return null;
        }
    }

    /*
     * Parser LL(1) que construye el parse tree con nodos E, T, F visibles.
     * Usa la misma gramatica sin recursion izquierda que el Parser del AST.
     */
    private static class TreeParser {
        final List<String> tokens;
        int pos = 0;
        final List<Pattern> patrones = new ArrayList<>();

        TreeParser(List<String> tokens, List<String> rangos) {
            this.tokens = tokens;
            for (String r : rangos) patrones.add(Pattern.compile("^" + r + "$"));
            if (patrones.isEmpty()) patrones.add(Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$"));
        }

        boolean hayToken() { return pos < tokens.size(); }
        String  peek()     { return tokens.get(pos); }
        String  consume()  { return tokens.get(pos++); }

        boolean esId(String t) {
            for (var p : patrones) if (p.matcher(t).matches()) return true;
            return false;
        }

        // E -> T E'
        ParseTreeNode parseE() throws Exception {
            ParseTreeNode e = new ParseTreeNode("E");
            e.hijos.add(parseT());
            ParseTreeNode ep = parseEPrima();
            if (ep != null) e.hijos.add(ep);
            return e;
        }

        // E' -> '+' T E' | '-' T E' | vacio
        ParseTreeNode parseEPrima() throws Exception {
            if (hayToken() && (peek().equals("+") || peek().equals("-"))) {
                ParseTreeNode ep = new ParseTreeNode("E'");
                ep.hijos.add(new ParseTreeNode(consume()));
                ep.hijos.add(parseT());
                ParseTreeNode ep2 = parseEPrima();
                if (ep2 != null) ep.hijos.add(ep2);
                return ep;
            }
            return null;
        }

        // T -> F T'
        ParseTreeNode parseT() throws Exception {
            ParseTreeNode t = new ParseTreeNode("T");
            t.hijos.add(parseF());
            ParseTreeNode tp = parseTPrima();
            if (tp != null) t.hijos.add(tp);
            return t;
        }

        // T' -> '*' F T' | '/' F T' | vacio
        ParseTreeNode parseTPrima() throws Exception {
            if (hayToken() && (peek().equals("*") || peek().equals("/"))) {
                ParseTreeNode tp = new ParseTreeNode("T'");
                tp.hijos.add(new ParseTreeNode(consume()));
                tp.hijos.add(parseF());
                ParseTreeNode tp2 = parseTPrima();
                if (tp2 != null) tp.hijos.add(tp2);
                return tp;
            }
            return null;
        }

        // F -> '(' E ')' | identificador
        ParseTreeNode parseF() throws Exception {
            if (!hayToken()) throw new Exception("EOF inesperado");
            ParseTreeNode f = new ParseTreeNode("F");
            if (peek().equals("(")) {
                f.hijos.add(new ParseTreeNode(consume()));
                f.hijos.add(parseE());
                if (!hayToken() || !peek().equals(")")) throw new Exception("Se esperaba ')'");
                f.hijos.add(new ParseTreeNode(consume()));
                return f;
            }
            if (esId(peek())) {
                f.hijos.add(new ParseTreeNode(consume()));
                return f;
            }
            throw new Exception("Token inesperado: " + peek());
        }
    }

    // Construye el AST semantico usando el Parser LL(1)
    public NodoAST construirAST(List<String> tokens) throws Parser.ParseException {
        Parser p = new Parser();
        for (String patron : patronesRangos) p.agregarPatronIdentificador(patron);
        if (patronesRangos.isEmpty()) p.agregarPatronIdentificador("[a-zA-Z_][a-zA-Z0-9_]*");
        return p.parse(tokens);
    }

    public String getSimboloInicial() { return simboloInicial; }
}
