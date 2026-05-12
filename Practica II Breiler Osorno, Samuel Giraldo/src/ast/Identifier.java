package ast;

// Nodo hoja: representa una variable como x, h, z
public class Identifier extends NodoAST {
    private final String nombre;

    public Identifier(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toTexto(int indent) {
        return "  ".repeat(indent) + "[Identifier: " + nombre + "]";
    }
}
