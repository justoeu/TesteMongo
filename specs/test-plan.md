# Plano de Testes

## Pirâmide

```
        /\
       /E2\           ~ 5%   Cypress: fluxo principal UC-01
      /----\
     / Int. \         ~ 25%  Spring + RestAssured: API
    /--------\
   /  Unit    \       ~ 70%  JUnit 5 + AssertJ: domínio
  /------------\
```

## Testes unitários — Backend (`PaceCalculatorService`)

### Casos obrigatórios
- `paceFromTime_5km_25min_returnsFiveMinPerKm`
- `paceFromTime_10km_50min_returnsFiveMinPerKm`
- `speedFromTime_10km_45min_returns13_33`
- `timeFromPace_halfMarathon_5min_returns_1h45m29s`
- `timeFromSpeed_marathon_10kmh_returns_4h13m`
- Conversão pace ↔ velocidade ida e volta (property-based)

### Casos-borda (devem lançar `IllegalArgumentException`)
- `distance == 0`
- `distance < 0`
- `distance > 500`
- `time == 0`
- `time > 86400`
- `pace < 120` (2:00/km)
- `pace > 900` (15:00/km)

### Property-based (jqwik)
- Para qualquer `d > 0` e `t > 0`: `paceFromTime(d, t) ≈ 3600 / speedFromTime(d, t)` (tolerância 0.01)

## Testes de integração — API

- `POST /api/v1/pace/from-time` 200 com payload válido
- `POST /api/v1/pace/from-time` 400 com `distanceKm = -1`
- Rate limit: 61ª requisição em 60s retorna 429
- Header `X-Request-Id` propagado em logs

## Testes de mutação (PITest)

- Threshold mínimo: 60% mutation score, 80% line coverage
- Exclusões: `equals`, `hashCode`, `toString`

## Testes E2E (Cypress)

- Cenário 1 (UC-01): preencher 10 km + 50:00 → ver "5:00/km"
- Cenário 2: digitar `0` no tempo → ver mensagem de erro
- Cenário 3: clicar "Copiar" → asserir `cy.window().its('navigator.clipboard')`

## Testes de acessibilidade

- `axe-core` integrado no Cypress: zero violações em nível AA

## Testes de performance

- k6: 100 RPS por 1 min em `/api/v1/pace/from-time` → P95 < 200 ms

## Tabela-oráculo (compartilhada front/back)

| ID | Distância | Entrada | Modo | Esperado |
|----|-----------|---------|------|----------|
| T1 | 5 km | 25:00 | tempo | pace 5:00, vel 12,00 |
| T2 | 10 km | 50:00 | tempo | pace 5:00, vel 12,00 |
| T3 | 10 km | 45:00 | tempo | pace 4:30, vel 13,33 |
| T4 | 21,0975 km | 1:45:29 | tempo | pace 5:00, vel 12,00 |
| T5 | 21,0975 km | 5:00 | pace | tempo 1:45:29, vel 12,00 |
| T6 | 42,195 km | 12,00 | velocidade | tempo 3:30:59, pace 5:00 |
| T7 | 7,5 km | 42:30 | tempo | pace 5:40, vel 10,59 |

> Esses casos devem ser implementados como `@ParameterizedTest` no backend e
> `test.each` no frontend, garantindo que ambas as implementações produzam o
> mesmo resultado bit a bit (após formatação).
