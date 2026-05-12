package ast;

// Clase base de todos los nodos del AST
public abstract class NodoAST {
    public abstract String toTexto(int indent);

    @Override
    public String toString() {
        return toTexto(0);
    }
}
