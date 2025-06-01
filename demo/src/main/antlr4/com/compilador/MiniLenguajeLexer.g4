lexer grammar MiniLenguajeLexer;

// Fragmentos auxiliares
fragment LETRA   : [A-Za-z] ;
fragment DIGITO  : [0-9] ;

// ---------- Separadores ----------
PA       : '(' ;
PC       : ')' ;
CA       : '[' ;
CC       : ']' ;
LLAVE_A  : '{' ;
LLAVE_C  : '}' ;
PYC      : ';' ;
COMA     : ',' ;

// ---------- Operadores simples ----------
IGUAL            : '=' ;
SUMA             : '+' ;
RESTA            : '-' ;
MULTIPLICACION   : '*' ;
DIVISION         : '/' ;
MODULO           : '%' ;
NOT_LOGICO       : '!' ;

// ---------- Operadores relacionales ----------
MAYOR            : '>' ;
MAYOR_IGUAL      : '>=' ;
MENOR            : '<' ;
MENOR_IGUAL      : '<=' ;
IGUAL_IGUAL      : '==' ;
DISTINTO         : '!=' ;

// ---------- Operadores lógicos ----------
AND_LOGICO       : '&&' ;
OR_LOGICO        : '||' ;

// ---------- Incremento / Decremento ----------
PLUSPLUS         : '++' ;
MINUSMINUS       : '--' ;

// ---------- Palabras clave ----------
IF        : 'if' ;
ELSE      : 'else' ;
FOR       : 'for' ;
WHILE     : 'while' ;
BREAK     : 'break' ;
CONTINUE  : 'continue' ;
RETURN    : 'return' ;

INT       : 'int' ;
CHAR      : 'char' ;
DOUBLE    : 'double' ;
VOID      : 'void' ;

// ---------- Literales ----------
// Números enteros
INTEGER   : DIGITO+ ;

// Números decimales (doble precisión)
DECIMAL   : DIGITO+ '.' DIGITO+ ;

// Caracter (incluye escapes tipo '\n', '\t', '\\', etc.)
CHARACTER
    : '\'' ( ~['\\\r\n] | '\\' . ) '\''
    ;

// Cadena de texto (entre dobles comillas)
/*
STRING_literal
    : '"' ( ~["\\\r\n] | '\\' . )* '"'
    ;
*/
// (Si no necesitas strings en este subconjunto, puedes comentar o eliminar la regla STRING_literal)

// ---------- Identificador ----------
ID        : (LETRA | '_') (LETRA | DIGITO | '_')* ;

// ---------- Comentarios y espacios ----------
// Comentario de línea: '//' hasta fin de línea
COMENTARIO_LINEA
    : '//' ~[\r\n]* -> skip
    ;

// Comentario de bloque: '/* */'
COMENTARIO_BLOQUE
    : '/*' .*? '*/' -> skip
    ;

// Espacios en blanco (tab, espacio, newline)
WS
    : [ \t\r\n]+ -> skip
    ;

// ---------- Token de error léxico (cualquier otro carácter) ----------
ERROR
    : .
    ;
