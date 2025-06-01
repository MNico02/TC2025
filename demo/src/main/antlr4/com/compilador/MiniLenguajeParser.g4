parser grammar MiniLenguajeParser;

options { tokenVocab=MiniLenguajeLexer; }

programa
    : (sentencia)* EOF
    ;

// ---------- Sentencias principales ----------
sentencia
    : declaracionVariable
    | declaracionFuncion
    | asignacion
    | ifStmt
    | whileStmt
    | forStmt
    | breakStmt
    | continueStmt
    | retorno
    | llamada PYC
    ;

// ---------- Regla "tipo" añadida ----------
tipo
    : INT
    | CHAR
    | DOUBLE
    | VOID
    ;

// ---------- Declaraciones de variables y funciones ----------
declaracionVariable
    : tipo ID ( IGUAL expresion )? PYC
    ;

declaracionFuncion
    : tipo ID PA parametros? PC bloque
    ;

// Parámetros de función separados por comas
parametros
    : parametro ( COMA parametro )*
    ;

parametro
    : tipo ID
    ;

// ---------- Asignación simple ----------
asignacion
    : ID IGUAL expresion PYC
    ;

// ---------- if-else ----------
ifStmt
    : IF PA expresion PC bloque ( ELSE bloque )?
    ;

// ---------- while ----------
whileStmt
    : WHILE PA expresion PC bloque
    ;

// ---------- for (incluye inicialización, condición y actualización) ----------
forStmt
    : FOR PA forInit PYC expresion PYC actualizacionFor PC bloque
    ;

forInit
    : declaracionFor      // “int i = expr” o “i = expr”
    | asignacionFor       // “i = expr”
    ;

declaracionFor
    : tipo ID ( IGUAL expresion )?
    ;

asignacionFor
    : ID IGUAL expresion
    ;

// Soporta tanto i = expr  como  i++ / i--  y  ++i / --i
actualizacionFor
    : asignacionFor          // “i = expr”
    | ID PLUSPLUS            // “i++”
    | ID MINUSMINUS          // “i--”
    | PLUSPLUS ID            // “++i”
    | MINUSMINUS ID          // “--i”
    ;

// ---------- break y continue ----------
breakStmt
    : BREAK PYC
    ;

continueStmt
    : CONTINUE PYC
    ;

// ---------- return (puede o no llevar expresión) ----------
retorno
    : RETURN expresion? PYC
    ;

// ---------- llamada a función sin asignar (ej: foo(a, b); ) ----------
llamada
    : ID PA ( expresion ( COMA expresion )* )? PC
    ;

// ---------- Bloque de sentencias entre llaves ----------
bloque
    : LLAVE_A ( sentencia )* LLAVE_C
    ;

// ---------- Expresiones ----------
expresion
    : expresion operadorBinario expresion       #expBinaria
    | NOT_LOGICO expresion                             #expUnaria
    | PLUSPLUS ID                               #expPreIncremento
    | MINUSMINUS ID                             #expPreDecremento
    | ID PLUSPLUS                               #expPostIncremento
    | ID MINUSMINUS                             #expPostDecremento
    | PA expresion PC                           #expParentesis
    | llamada                                   #expLlamada
    | ID                                        #expID
    | INTEGER                                   #expEntero
    | DECIMAL                                   #expDecimal
    | CHARACTER                                 #expCaracter
    ;

// ---------- Operadores binarios (aritméticos, relacionales y lógicos) ----------
operadorBinario
    : SUMA
    | RESTA
    | MULTIPLICACION
    | DIVISION
    | MODULO
    | MAYOR
    | MENOR
    | MAYOR_IGUAL
    | MENOR_IGUAL
    | IGUAL_IGUAL
    | DISTINTO
    | AND_LOGICO
    | OR_LOGICO
    ;
