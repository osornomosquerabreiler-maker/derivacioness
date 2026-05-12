package ast;

import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;

/*
 * Parser recursivo descendente LL(1).
 *
 * Gramatica interna (sin recursion izquierda):
 *   E  -> T E'
 *   E' -> '+' T E' | '-' T E' | vacio
 *   T  -> F T'
 *   T' -> '*' F T' | '/' F T' | vacio
 *   F  -> '(' E ')' | identificador
 *
 * Precedencia: * y / tienen mayor prioridad que + y -
 * Asociatividad: izquierda para todos los operadores
 */
public class Parser {

    private List<String> tokens;
    private int pos;
    private final List<Pattern> patronesId = new ArrayList<>();

    public Parser() {}

    // Registra un patron valido para identificadores, ej: [a-z]
    public void agregarPatronIdentificador(String regex) {
        patronesId.add(Pattern.compile("^" + regex + "$"));
    }

    // Punto de entrada: recibe los tokens y devuelve el AST raiz
    public NodoAST parse(List<String> tokensEntrada) throws ParseException {
        this.tokens = tokensEntrada;
        this.pos = 0;
        NodoAST resultado = parseE();
        if (pos < tokens.size()) {
            throw new ParseException("Token inesperado: '" + tokens.get(pos) + "'");
        }
        return resultado;
    }

    // E -> T E'
    private NodoAST parseE() throws ParseException {
        NodoAST izq = parseT();
        return parseEPrima(izq);
    }

    // E' -> '+' T E' | '-' T E' | vacio
    private NodoAST parseEPrima(NodoAST izq) throws ParseException {
        if (hayToken() && (peek().equals("+") || peek().equals("-"))) {
            String op = consume();
            NodoAST der = parseT();
            return parseEPrima(new BinaryExpression(op, izq, der));
        }
        return izq;
    }

    // T -> F T'
    private NodoAST parseT() throws ParseException {
        NodoAST izq = parseF();
        return parseTPrima(izq);
    }

    // T' -> '*' F T' | '/' F T' | vacio
    private NodoAST parseTPrima(NodoAST izq) throws ParseException {
        if (hayToken() && (peek().equals("*") || peek().equals("/"))) {
            String op = consume();
            NodoAST der = parseF();
            return parseTPrima(new BinaryExpression(op, izq, der));
        }
        return izq;
    }

    // F -> '(' E ')' | identificador
    // Los parentesis se descartan: no aparecen en el AST
    private NodoAST parseF() throws ParseException {
        if (!hayToken()) throw new ParseException("Se esperaba un valor pero llego al final.");

        String t = peek();

        if (t.equals("(")) {
            consume();
            NodoAST inner = parseE();
            expect(")");
            return inner;
        }

        if (esIdentificador(t)) {
            consume();
            return new Identifier(t);
        }

        throw new ParseException("Token inesperado: '" + t + "'");
    }

    // --- utilidades ---

    private boolean hayToken()          { return pos < tokens.size(); }
    private String  peek()              { return tokens.get(pos); }
    private String  consume()           { return tokens.get(pos++); }

    private void expect(String esperado) throws ParseException {
        if (!hayToken() || !peek().equals(esperado)) {
            String encontrado = hayToken() ? "'" + peek() + "'" : "fin de expresion";
            throw new ParseException("Se esperaba '" + esperado + "' pero se encontro " + encontrado);
        }
        consume();
    }

    private boolean esIdentificador(String token) {
        for (Pattern p : patronesId) if (p.matcher(token).matches()) return true;
        return false;
    }

    // Excepcion de parseo
    public static class ParseException extends Exception {
        public ParseException(String msg) { super(msg); }
    }
}
