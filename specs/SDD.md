# Software Design Document (SDD)
## Calculadora Interativa de Ritmo de Corrida — Start Runners

**Versão:** 1.0
**Data:** 2026-04-24
**Autor:** Equipe Start Runners (especificação assistida)
**Status:** Aprovado para implementação

---

## 1. Resumo Executivo

A **Calculadora Interativa de Ritmo de Corrida** é uma aplicação web que
permite a corredores planejar provas e treinos calculando, em tempo real,
**pace (min/km)**, **velocidade média (km/h)** e **tempo total** para
distâncias canônicas (5 km, 10 km, 21,0975 km, 42,195 km) e personalizadas.

Três modos de cálculo são suportados:

1. **A partir do tempo alvo** → calcula pace e velocidade
2. **A partir do pace** → calcula tempo e velocidade
3. **A partir da velocidade** → calcula tempo e pace

A solução é entregue como aplicação web (React + Spring Boot), com lógica de
cálculo duplicada client/server para entregar UX instantânea com server como
fonte de verdade.

> **Nota:** este SDD é o documento integrador. Detalhes específicos vivem em
> [`requirements.md`](./requirements.md), [`use-cases.md`](./use-cases.md),
> [`formulas.md`](./formulas.md), [`api.md`](./api.md),
> [`architecture.md`](./architecture.md), [`ui-mockups.md`](./ui-mockups.md) e
> [`test-plan.md`](./test-plan.md).

---

## 2. Objetivos e Escopo

### 2.1. Objetivos
- Entregar resultado em < 5 segundos sem cadastro
- Cobrir os 4 cenários mais comuns de planejamento (5/10/21/42 km)
- Garantir precisão matemática (validada por tabela-oráculo)
- Atingir mutation score ≥ 60% e cobertura ≥ 80%

### 2.2. Escopo MVP
- Cálculos nos 3 modos (tempo, pace, velocidade)
- Tabela de splits
- Histórico local (`localStorage`)
- Botão "copiar resultado"

### 2.3. Fora do escopo MVP
- Login, perfis, sincronização com Strava/Garmin
- Predição por VO2max ou fórmulas Riegel/Cameron
- Planos de treino

---

## 3. Visão de Arquitetura

```
[ Browser (React + Zustand) ]
        │  HTTP/JSON
        ▼
[ Spring Boot 3 / Java 17 ]
   Controllers ─► Services ─► Domain (BigDecimal)
                              │
                              ▼
                       [ MongoDB ]  ← apenas histórico (pós-MVP)
```

Arquitetura **hexagonal** no backend: domínio puro (value objects + serviço de
cálculo) sem dependências de framework. Adapters de entrada (REST) e saída
(MongoDB futuro) ficam isolados.

Detalhes em [`architecture.md`](./architecture.md).

---

## 4. Modelo de Domínio

### 4.1. Value Objects (imutáveis)

```
Distance   { kilometers: BigDecimal }     // > 0, ≤ 500
RaceTime   { totalSeconds: long }         // > 0, ≤ 86_400
Pace       { secondsPerKm: int }          // 120..900
Speed      { kmh: BigDecimal }            // 4..30
Split      { km: int, cumulativeSeconds: long }
```

### 4.2. Operações de domínio

```
Pace      Pace.fromTime(Distance d, RaceTime t)
Speed     Speed.fromTime(Distance d, RaceTime t)
RaceTime  RaceTime.fromPace(Distance d, Pace p)
RaceTime  RaceTime.fromSpeed(Distance d, Speed s)
Pace      Pace.fromSpeed(Speed s)
Speed     Speed.fromPace(Pace p)
List<Split> Splits.compute(Distance d, Pace p, int stepKm)
```

Todos os construtores validam invariantes; violações lançam
`IllegalArgumentException` com mensagem localizada.

### 4.3. Diagrama de classes (ASCII)

```
+------------+    fromTime     +-------+
| Distance   |◄───────────────►| Pace  |
+------------+                 +-------+
       ▲                           ▲
       │                           │
       │ fromPace                  │ fromSpeed
       ▼                           ▼
+------------+                 +-------+
| RaceTime   |◄────fromSpeed──►| Speed |
+------------+                 +-------+
```

---

## 5. Componentes

| Componente | Responsabilidade |
|------------|------------------|
| `PaceController` | Adapter REST (parse, validação superficial, mapeamento erros) |
| `PaceCalculatorService` | Orquestra value objects do domínio |
| `Distance` / `RaceTime` / `Pace` / `Speed` | Lógica de domínio + invariantes |
| `Splits` | Geração de tabela de splits |
| `GlobalExceptionHandler` | Conversão de exceções → JSON padronizado |
| `RateLimitFilter` | 60 req/min por IP via token bucket |
| `HistoryStore` (front) | `localStorage` com chave `pace_history_v1` |

---

## 6. Fluxos Principais

### 6.1. Cálculo a partir do tempo alvo (UC-01)

```
Usuário ─► UI ─► debounce 200ms ─► função cliente compute()
                                ─► (em paralelo) POST /api/v1/pace/from-time
UI exibe resultado client-side
quando server responder, reconcilia (se divergir, exibe valor server)
```

### 6.2. Persistência de histórico (futuro)

```
Usuário autentic. ─► POST /api/v1/history
                     ─► HistoryService.save()
                     ─► MongoDB pace_history
```

---

## 7. Contratos de API

Detalhamento completo em [`api.md`](./api.md). Endpoints:

- `POST /api/v1/pace/from-time`
- `POST /api/v1/pace/from-pace`
- `POST /api/v1/pace/from-speed`

Formato de erro padronizado, header `X-Request-Id`, rate limit nos headers.

---

## 8. Modelo de Dados (Histórico — pós-MVP)

Coleção `pace_history`:

```json
{
  "_id": "ObjectId",
  "userId": "string|null",
  "createdAt": "ISO-8601",
  "input": {
    "mode": "from-time|from-pace|from-speed",
    "distanceKm": 10.0,
    "timeSeconds": 3000,
    "paceSecondsPerKm": null,
    "speedKmh": null
  },
  "result": {
    "paceSecondsPerKm": 300,
    "speedKmh": 12.0,
    "timeSeconds": 3000
  }
}
```

Índices: `userId + createdAt desc`; TTL opcional 365 dias para anônimos.

---

## 9. Decisões Técnicas (ADRs)

Listadas em [`architecture.md`](./architecture.md). Resumo:

- **ADR-001** Lógica duplicada client/server
- **ADR-002** `BigDecimal` no servidor
- **ADR-003** Sem framework pesado no front (React puro + Zustand)
- **ADR-004** Histórico só no MVP em `localStorage`
- **ADR-005** Stack Java consistente com `TesteMongo`

---

## 10. Qualidade

| Aspecto | Alvo |
|---------|------|
| Cobertura de linha | ≥ 80% |
| Mutation score | ≥ 60% |
| OWASP Dependency Check | falha em CVSS ≥ 7.0 |
| SpotBugs + FindSecBugs | sem violações High |
| Acessibilidade (axe) | zero violações WCAG AA |
| P95 da API | < 200 ms |

Plano completo em [`test-plan.md`](./test-plan.md).

---

## 11. Segurança

- Inputs apenas numéricos; validação rigorosa nos value objects
- CSP estrita no front; sem `eval`
- Rate limit 60 req/min por IP; resposta 429 com `Retry-After`
- Logs sem dados pessoais; `X-Request-Id` para rastreabilidade
- Dependências auditadas mensalmente (`mvn versions:display-dependency-updates`)

---

## 12. Observabilidade

- **Logs:** JSON estruturado (Logback) com MDC `requestId`, `endpoint`, `durationMs`
- **Métricas:** Micrometer/Prometheus — `http_request_duration_seconds`,
  `calc_total{mode=...}`, `validation_errors_total{field=...}`
- **Tracing:** opcional, OpenTelemetry exportando para Jaeger

---

## 13. Plano de Implementação (Roadmap)

### Sprint 1 — Domínio + API
- Value Objects: `Distance`, `RaceTime`, `Pace`, `Speed`
- `PaceCalculatorService` com 3 modos
- `PaceController` + DTOs
- Tabela-oráculo como `@ParameterizedTest`
- Mutation testing CI

### Sprint 2 — Front MVP
- Setup Vite + React + Zustand
- Componentes: `DistanceSelector`, `TimeInput`, `ResultCard`, `SplitsTable`
- Lógica client-side (mesma tabela-oráculo)
- Cypress: cenários UC-01, UC-04

### Sprint 3 — Polimento
- Histórico local (`localStorage`)
- Modo escuro automático
- A11y audit (axe)
- k6 perf test
- Deploy staging

### Sprint 4 — Endurecimento
- Rate limit
- OpenTelemetry
- Documentação OpenAPI publicada
- Smoke tests pós-deploy

---

## 14. Riscos e Mitigações

| Risco | Probabilidade | Impacto | Mitigação |
|-------|---------------|---------|-----------|
| Divergência client/server em arredondamento | Média | Médio | Tabela-oráculo compartilhada; testes de paridade |
| Float imprecisos no JS | Alta | Médio | Operações inteiras em segundos no client |
| Bundle JS inflado | Média | Alto | Sem libs UI pesadas; budget 80 KB gzip |
| Rate limit afeta scraping legítimo | Baixa | Baixo | Documentar limites; chave de API se necessário |

---

## 15. Critérios de Aceite (DoD do MVP)

- [ ] Os 7 casos da tabela-oráculo passam no front e no back
- [ ] Lighthouse mobile ≥ 90 em Performance e Accessibility
- [ ] Mutation score ≥ 60%, cobertura ≥ 80%
- [ ] OWASP Dependency Check sem CVSS ≥ 7.0
- [ ] P95 < 200 ms a 100 RPS
- [ ] Documentação OpenAPI publicada
- [ ] README com instruções de execução local

---

## 16. Referências

- World Athletics — distâncias oficiais
- WCAG 2.1 — diretrizes de acessibilidade
- OWASP Top 10 (2021)
- Padrão Hexagonal — Alistair Cockburn
- `justoeu/TesteMongo` — referência de stack Java/Maven

---

## Apêndice A — Exemplos de execução manual

```bash
# Backend (após implementado)
mvn spring-boot:run

# Curl
curl -X POST http://localhost:8080/api/v1/pace/from-time \
  -H 'Content-Type: application/json' \
  -d '{"distanceKm": 10, "timeSeconds": 3000}'

# Resposta esperada
# {"distanceKm":10.0,"timeSeconds":3000,"paceSecondsPerKm":300,
#  "paceFormatted":"5:00/km","speedKmh":12.0,...}
```
