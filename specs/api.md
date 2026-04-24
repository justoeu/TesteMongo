# API REST — Pace Calculator

Base path: `/api/v1`

## POST `/pace/from-time`

Calcula pace e velocidade a partir de distância e tempo.

### Request
```json
{
  "distanceKm": 10.0,
  "timeSeconds": 3000
}
```

### Response 200
```json
{
  "distanceKm": 10.0,
  "timeSeconds": 3000,
  "paceSecondsPerKm": 300,
  "paceFormatted": "5:00/km",
  "speedKmh": 12.00,
  "splits": [
    { "km": 1, "cumulativeSeconds": 300, "cumulativeFormatted": "5:00" },
    { "km": 2, "cumulativeSeconds": 600, "cumulativeFormatted": "10:00" }
  ]
}
```

### Erros
| Status | Code | Quando |
|--------|------|--------|
| 400 | `INVALID_DISTANCE` | distância ≤ 0 ou > 500 |
| 400 | `INVALID_TIME` | tempo ≤ 0 ou > 86400 |
| 422 | `OUT_OF_RANGE_PACE` | pace fora de 2:00–15:00/km |

---

## POST `/pace/from-pace`

Calcula tempo previsto a partir de distância e pace.

### Request
```json
{
  "distanceKm": 21.0975,
  "paceSecondsPerKm": 300
}
```

### Response 200
```json
{
  "distanceKm": 21.0975,
  "paceSecondsPerKm": 300,
  "paceFormatted": "5:00/km",
  "speedKmh": 12.00,
  "timeSeconds": 6329,
  "timeFormatted": "1:45:29",
  "splits": [ ... ]
}
```

---

## POST `/pace/from-speed`

Calcula tempo previsto a partir de distância e velocidade.

### Request
```json
{
  "distanceKm": 10.0,
  "speedKmh": 12.0
}
```

### Response 200
```json
{
  "distanceKm": 10.0,
  "speedKmh": 12.00,
  "paceSecondsPerKm": 300,
  "paceFormatted": "5:00/km",
  "timeSeconds": 3000,
  "timeFormatted": "50:00",
  "splits": [ ... ]
}
```

---

## Formato de erro padrão

```json
{
  "error": {
    "code": "INVALID_DISTANCE",
    "message": "A distância deve ser maior que zero e menor ou igual a 500 km.",
    "field": "distanceKm"
  }
}
```

## Headers

- `Content-Type: application/json; charset=utf-8`
- `X-RateLimit-Limit: 60`
- `X-RateLimit-Remaining: <n>`
- `X-Request-Id: <uuid>` (gerado pelo servidor; usado em logs)

## Versionamento

- Caminho do path (`/v1`); breaking changes → `/v2`
- Sem deprecation silenciosa: anunciar 90 dias antes via header `Sunset`
