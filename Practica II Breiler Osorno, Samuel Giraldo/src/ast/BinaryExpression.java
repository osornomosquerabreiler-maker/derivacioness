package ast;

// Nodo operacion binaria: representa algo como h + o
public class BinaryExpression extends NodoAST {
    private final String operador;
    private final NodoAST izquierda;
    private final NodoAST derecha;

    public BinaryExpression(String operador, NodoAST izquierda, NodoAST derecha) {
        this.operador = operador;
        this.izquierda = izquierda;
        this.derecha = derecha;
    }

    public String getOperador()   { return operador;  }
    public NodoAST getIzquierda() { return izquierda; }
    public NodoAST getDerecha()   { return derecha;   }

    @Override
    public String toTexto(int indent) {
        String p = "  ".repeat(indent);
        return p + "[BinaryExpression: " + operador + "]\n"
             + izquierda.toTexto(indent + 1) + "\n"
             + derecha.toTexto(indent + 1);
    }
}
