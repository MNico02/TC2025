# 📚 TC2025: Compilador Subconjunto C++ con ANTLR4

¡Bienvenidos al repositorio del **Proyecto de Técnicas de Compilación 2025**! 🚀

Este compilador en Java, basado en ANTLR4, procesa un subconjunto de C++ y cubre todas las fases clásicas: léxico, sintáctico, semántico, generación de código intermedio de tres direcciones y optimización.

---

## 📝 Tabla de Contenidos

* [🎯 Objetivos](#-objetivos)
* [⚙️ Estructura del Proyecto](#️-estructura-del-proyecto)
* [🚀 Instalación y Uso](#-instalación-y-uso)
* [🔍 Fases del Compilador](#-fases-del-compilador)

    * Análisis Léxico
    * Análisis Sintáctico & AST
    * Análisis Semántico
    * Generación de Código Intermedio
    * Optimización de Código
* [📂 Gramática ANTLR4](#-gramática-antlr4)
* [🛠️ Ejemplos de Prueba](#️-ejemplos-de-prueba)
* [👥 Equipo](#-equipo)

---

## 🎯 Objetivos

* **Implementar** un compilador completo para un subconjunto de C++ (`int`, `char`, `double`, `void`).
* **Aplicar** las fases de compilación:

    1. Léxico
    2. Sintáctico (+AST)
    3. Semántico
    4. Código intermedio (3 direcciones)
    5. Optimización
* **Generar** salidas coloreadas:

    * ✅ Verde para éxitos
    * ⚠️ Amarillo para warnings
    * ❌ Rojo para errores

Este trabajo cumple con la consigna oficial .

---

## ⚙️ Estructura del Proyecto

```bash
TC2025/
├── src/
│   ├── main/
│   │   ├── antlr4/
│   │   │   └── com/compilador/
│   │   │       ├── MiniLenguajeLexer.g4
│   │   │       └── MiniLenguajeParser.g4
│   │   └── java/
│   │       └── com/compilador/
│   │           ├── App.java
│   │           ├── CodigoVisitor.java
│   │           ├── GeneradorCodigo.java
│   │           ├── Optimizador.java
│   │           └── semantico/
│   │               ├── SimbolosListener.java
│   │               └── TablaSimbolos.java
├── ejemplos/
│   ├── ejemplo1.txt
│   ├── ejemplo2.txt
│   └── ...
├── pom.xml
└── README.md
```

---

## 🚀 Instalación y Uso

1. **Clonar** el repositorio

   ```bash
   git clone https://github.com/MNico02/TC2025.git
   cd TC2025 cd demo
   ```

2. **Compilar** con Maven

   ```bash
   mvn clean generate-sources compile 
   mvn package assembly:single
   ```

3. **Ejecutar** el compilador

   ```bash
   java -jar target/demo-1.0-SNAPSHOT-jar-with-dependencies.jar ejemplo.txt     
   ```

    * `--no-ast` desactiva la visualización gráfica del AST.

4. **Archivos de salida** generados:

    * `ejemplo1_c3d.txt` → Código intermedio
    * `ejemplo1_opt.txt` → Código optimizado

---

## 🔍 Fases del Compilador

### 1. Análisis Léxico

* Implementado con ANTLR4 (archivo `.g4`).
* Reconoce tokens: identificadores, literales, operadores, separadores.
* Captura y reporta errores léxicos.

### 2. Análisis Sintáctico & AST

* Genera `ParseTree` con ANTLR4.
* Reporta errores sintácticos y, opcionalmente, muestra el AST en una ventana Swing.

### 3. Análisis Semántico

* `SimbolosListener` construye la tabla de símbolos.
* Verifica:

    * Declaraciones duplicadas
    * Uso de variables no inicializadas
    * Tipos y compatibilidad
    * Ámbitos y parámetros
* Diferencia entre **errores críticos** y **warnings**.

### 4. Generación de Código Intermedio

* **Visitor + Generador**:

    * `CodigoVisitor` recorre el AST (¿qué construir?).
    * `GeneradorCodigo` emite instrucciones de tres direcciones (¿cómo construir?).
* Maneja expresiones, control de flujo, funciones y retornos.
* Salida: lista numerada de instrucciones C3D.

### 5. Optimización de Código

* `Optimizador` aplica:

    1. ✂️ Eliminación de código muerto
    2. 🔢 Propagación de constantes
    3. 🔄 Simplificación de expresiones
    4. ♻️ Eliminación de subexpresiones comunes
    5. 🔁 Optimización de bucles
* Genera un archivo `_opt.txt` con el C3D optimizado.

---


## 🛠️ Ejemplos de Prueba

* **`ejemplo1.txt`**

  ```c++
  int calculaMax(int a, int b) {
      if (a > b) return a;
      else return b;
  }
  void main() {
      int r = calculaMax(5, 3);
  }
  ```

    * C3D y optimizado en `ejemplo1_c3d.txt` y `ejemplo1_opt.txt`.

* **`ejemplo2.txt`**

  ```c++
  void main() {
      int a = 0, b = 0;
      int res = a + b;
  }
  ```

    * Detecta warnings de uso sin inicializar y genera AST, C3D, optimizado.

---

## 👥 Equipo

* Moresco Nicolas
* Joaquín Velez

---

