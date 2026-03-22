# ⚡ zEngine — Framework de Investigación Cuantitativa para Trading

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![License](https://img.shields.io/badge/Licencia-MIT-blue?style=flat-square)

**zEngine** es un framework profesional de backtesting y optimización para estrategias de trading cuantitativo. A diferencia de los bots de trading convencionales que buscan el "mejor resultado puntual", zEngine identifica **Mesetas de Robustez** — zonas de parámetros donde la estrategia sobrevive a diferentes ciclos de mercado sin caer en el overfitting (sobre-optimización).

---

## 🎯 Características Principales

- **🔬 Fuerza Bruta (Grid Search)** — Prueba miles de combinaciones de parámetros en segundos
- **📊 Heatmap 2D de Robustez** — Mapa de calor interactivo con escala de color divergente (Rojo → Gris → Verde) mapeando el Beneficio Neto al color de cada celda
- **🧬 Dimension Slicing** — Explora espacios de parámetros N-dimensionales mediante sliders dinámicos que filtran cortes 2D del heatmap
- **💰 Gestión de Riesgo Realista** — Position sizing con riesgo porcentual fijo e interés compuesto
- **🧩 Arquitectura Agnóstica de Estrategias** — Agrega nuevas estrategias como beans `@Component` de Spring — sin modificar el núcleo
- **📈 Dashboard Full-Stack** — Interfaz React con estética Glassmorphism para configurar, ejecutar y visualizar optimizaciones

---

## 🏗️ Arquitectura

```
zEngine/
├── src/main/java/com/zFrameWork/zEngine/
│   ├── core/                    # Abstracciones de dominio
│   │   ├── indicator/           #   Interfaz Indicator<T,R> + implementaciones EMA, RSI, Wilder
│   │   ├── strategy/            #   Interfaz TradingStrategy, MarketTick, TradeDirection
│   │   └── risk/                #   RiskManager (position sizing, stop-loss)
│   ├── engine/                  # Capa de ejecución
│   │   ├── backtest/            #   BacktestEngine — simulador de corrida única
│   │   ├── optimizer/           #   OptimizationOrchestrator — coordinador del grid search
│   │   └── data/                #   CsvMarketDataProvider — cargador de datos históricos
│   ├── examples/                # Estrategias plug-and-play
│   │   ├── TripleEmaStrategy    #   Cruce de 3 EMAs (3 params, 462 combos)
│   │   ├── RsiStrategy          #   RSI sobrecompra/sobreventa (3 params, 96 combos)
│   │   └── TripleWilderStrategy #   Variante con Wilder's Smoothing
│   ├── model/entity/            # Entidades JPA (OptimizationJob, OptimizationResult)
│   ├── repository/              # Repositorios Spring Data JPA
│   ├── web/                     # Controladores REST + DTOs
│   └── config/                  # CORS, WebConfig
├── src/main/resources/
│   ├── application.properties   # Configuración BD vía variables de entorno
│   └── historical_data/         # Velas BTC/USDT 15min (2019)
└── zEngine-ui/                  # Frontend React + Vite
    └── src/
        ├── components/          # PlateauVisualizer, ExecutionModal, StrategyCard
        ├── services/            # apiClient.js (consumidor REST)
        └── index.css            # Sistema de diseño Glassmorphism
```

### Principios de Diseño

| Principio | Aplicación |
|---|---|
| **Abierto/Cerrado** | El motor está cerrado a modificación, abierto a extensión mediante nuevos beans `TradingStrategy` |
| **Segregación de Interfaces** | El motor depende de la interfaz `TradingStrategy`, nunca de implementaciones concretas |
| **Strategy Pattern** | Cada algoritmo de trading es un componente autocontenido e intercambiable |
| **Orchestrator Pattern** | `OptimizationOrchestrator` coordina el ciclo de vida del grid search y la persistencia de jobs |

---

## 🚀 Cómo Empezar

### Requisitos Previos

- **Java 21+** y **Maven 3.6+**
- **PostgreSQL 14+** — crear una base de datos llamada `zengine`
- **Node.js 18+** y **npm**

### 1. Clonar y Configurar

```bash
git clone https://github.com/TU_USUARIO/zEngine.git
cd zEngine
```

Configura tus credenciales de base de datos mediante variables de entorno:

```bash
export DB_URL=jdbc:postgresql://localhost:5432/zengine
export DB_USERNAME=postgres
export DB_PASSWORD=tu_password_seguro
```

> **Tip:** En Windows, usa `set` en lugar de `export`, o crea un archivo `.env` (ignorado por git).

### 2. Iniciar el Backend

```bash
mvn clean install
mvn spring-boot:run
```

El servidor API se levantará en `http://localhost:8080`.

### 3. Iniciar el Frontend

```bash
cd zEngine-ui
npm install
npm run dev
```

Abre `http://localhost:5173` en tu navegador.

### 4. Ejecutar Tu Primera Optimización

1. Haz clic en cualquier tarjeta de estrategia en el dashboard
2. Configura los rangos de parámetros (min/max/step)
3. Presiona **"Ejecutar Fuerza Bruta"**
4. Una vez completada, haz clic en **"Abrir Meseta de Robustez"** para ver el heatmap
5. Para estrategias de 3+ parámetros, usa los sliders de **Dimension Slice** para explorar diferentes cortes

---

## 🧩 Crea Tu Propia Estrategia

Implementa `TradingStrategy` y anota con `@Component`:

```java
@Component
public class MiEstrategia implements TradingStrategy {

    @Override
    public String getStrategyName() { return "Mi_Estrategia_Custom"; }

    @Override
    public List<ParameterRange> getParameterDefinitions() {
        return List.of(
            new ParameterRange("param1", new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("5")),
            new ParameterRange("param2", new BigDecimal("1"),  new BigDecimal("10"), new BigDecimal("1"))
        );
    }

    @Override
    public void applyParameters(Map<String, BigDecimal> params) { /* ... */ }

    @Override
    public TradeDirection evaluateEntry(MarketTick tick) { /* ... */ }

    @Override
    public boolean evaluateExit(MarketTick tick, TradeDirection pos) { /* ... */ }
}
```

Reinicia el backend — tu estrategia aparecerá automáticamente en el dashboard.

---

## ⚙️ Configuración

Todas las propiedades configurables viven en `src/main/resources/application.properties`:

| Propiedad | Valor por Defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/zengine` | URL de conexión a PostgreSQL |
| `DB_USERNAME` | `postgres` | Usuario de base de datos |
| `DB_PASSWORD` | `changeme` | Contraseña de base de datos |
| `zengine.backtest.initial-capital` | `1300` | Capital inicial (USD) para los backtests |

---

## 📊 Datos Históricos

El repositorio incluye 12 meses de velas de 15 minutos de BTC/USDT (2019) en `src/main/resources/historical_data/`. Los archivos siguen el formato CSV de Binance:

```
Timestamp, Open, High, Low, Close, Volume
```

`CsvMarketDataProvider` carga automáticamente y ordena cronológicamente todos los archivos `.csv` de ese directorio.

---

## 📄 Licencia

Este proyecto está licenciado bajo la Licencia MIT — ver [LICENSE](LICENSE) para más detalles.


Proyecto de investigación cuantitativa para trading generado mediante Agentes de IA.

-Gemini Pro 3.1 high
-Claude Opus 4.6