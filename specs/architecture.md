# Arquitetura

## Visão de Camadas

```
┌────────────────────────────────────────────────┐
│  UI (React + Vite)                             │
│  - Componentes: DistanceSelector, TimeInput,   │
│    ResultCard, SplitsTable, HistoryDrawer      │
│  - Estado local (Zustand) + persist localStorage│
└──────────────┬─────────────────────────────────┘
               │ HTTP / fetch
               ▼
┌────────────────────────────────────────────────┐
│  API REST (Spring Boot 3 / Java 17)            │
│  Controllers → Services → Domain               │
│  - PaceController                              │
│  - PaceCalculatorService                       │
│  - HistoryService (futuro)                     │
└──────────────┬─────────────────────────────────┘
               │ MongoDB Driver Sync 5.x
               ▼
┌────────────────────────────────────────────────┐
│  MongoDB (apenas para histórico — futuro)      │
│  Coleção: pace_history                         │
└────────────────────────────────────────────────┘
```

## Decisões arquiteturais (ADRs resumidos)

### ADR-001 — Cálculo no client e no server
- **Decisão:** lógica duplicada client e server, com server como fonte de verdade
- **Por quê:** UX instantânea sem latência + segurança/confiabilidade
- **Consequência:** suíte de testes compartilhada por contrato (mesmos oráculos)

### ADR-002 — `BigDecimal` para cálculos no server
- **Decisão:** usar `BigDecimal` com `MathContext.DECIMAL64`
- **Por quê:** evitar erros de ponto flutuante em conversões repetidas
- **Consequência:** ligeira penalidade de performance (negligível para essa carga)

### ADR-003 — Sem framework pesado no front
- **Decisão:** React puro + Zustand; não usar Redux, MUI, etc.
- **Por quê:** bundle pequeno (< 80 KB gzip), foco em performance mobile

### ADR-004 — Histórico opcional fora do MVP
- **Decisão:** persistência apenas em `localStorage` no MVP
- **Por quê:** evita escopo de auth/perfil; entrega valor imediato
- **Consequência:** quando vier MongoDB, schema já está rascunhado em `data-model.md`

### ADR-005 — Stack Java consistente com `TesteMongo`
- **Decisão:** Java 17, Maven, JUnit 5, Logback, MongoDB Driver 5.x
- **Por quê:** reuso de infraestrutura/CI já existente no `justoeu/TesteMongo`

## Estrutura de pastas (proposta)

```
start-runners/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/java/br/com/justo/runners/
│       │   ├── api/          (Controllers + DTOs)
│       │   ├── domain/       (Pace, Distance, Speed, Time — value objects)
│       │   ├── service/      (PaceCalculatorService)
│       │   └── infra/        (config, exception handlers)
│       └── test/java/...     (mesma estrutura)
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── components/
│       ├── hooks/
│       ├── domain/           (cálculo client-side)
│       └── pages/
└── specs/                    (esta pasta)
```

## Padrões aplicados

- **Hexagonal/Ports & Adapters** no backend: domínio puro, sem dependências de framework
- **Value Objects imutáveis** para `Distance`, `Pace`, `Speed`, `RaceTime`
- **DTOs** separados das entidades de domínio
- **Strategy** para conversões (FromTime, FromPace, FromSpeed)

## Observabilidade

- Logs estruturados (JSON) via Logback + MDC com `requestId`
- Métricas Prometheus: `http_request_duration_seconds`, `calc_total`
- Sem PII nos logs (corrida não é dado sensível, mas manter higiene)

## Pipeline CI/CD (proposto)

1. `mvn -B verify` (testes + cobertura)
2. `mvn -Pmutation-testing verify` (mutation score ≥ 60%)
3. `mvn -Psecurity-check verify` (OWASP + SpotBugs/FindSecBugs)
4. Build front: `npm ci && npm run build && npm test`
5. Container: `docker build` (multi-stage)
6. Deploy: ambiente staging → smoke tests → prod
