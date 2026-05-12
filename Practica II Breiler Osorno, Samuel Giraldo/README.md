# Generador de Árboles Sintácticos

Proyecto de la asignatura ST0244 - Lenguajes de Programación y Paradigmas de Computación  
Universidad EAFIT

**Integrantes:**
- Breiler Smith Osorno Mosquera 
- Samuel Giraldo Arcila

**Lenguaje:** Java 21  
**Compilador:** OpenJDK 21  
**IDE utilizado:** IntelliJ IDEA  
**UI:** Java Swing (sin dependencias externas)

---

## Estructura del proyecto

```
src/ast/
├── NodoAST.java               clase base abstracta del AST
├── Identifier.java            nodo hoja (variable: x, h, z...)
├── BinaryExpression.java      nodo operacion binaria (+, -, *, /)
├── Parser.java                parser LL(1) recursivo-descendente, construye el AST
├── AnalizadorGramatical.java  parse tree + pasos de derivacion
├── ArbolPanel.java            panel Swing que dibuja los arboles
└── AplicacionGUI.java         ventana principal (main)
```

---

## Como ejecutar

**Opcion A — JAR ejecutable:**
```bash
java -jar ArbolSintactico.jar
```

**Opcion B — Compilar desde fuente:**
```bash
javac -d bin -sourcepath src src/ast/*.java
java -cp bin ast.AplicacionGUI
```

---

## Gramatica por defecto

```
E -> E '+' T | E '-' T | T
T -> T '*' F | T '/' F | F
F -> '(' E ')' | [a-z] [A-Z] [0-9]
```

Los rangos `[a-z]`, `[A-Z]`, `[0-9]` se expanden automaticamente con los tokens de la expresion ingresada.

## Funciones disponibles

| Boton | Descripcion |
|---|---|
| Mostrar Derivacion | Muestra los pasos de la derivacion izquierda o derecha |
| Generar Arbol de Derivacion | Abre ventana con el parse tree completo (incluye E, T, F) |
| Generar AST | Abre ventana con el AST limpio (solo operadores e identificadores) |
