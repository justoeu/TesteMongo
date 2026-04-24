# Modelo Matemático

## Variáveis e unidades

| Símbolo | Significado | Unidade |
|---------|-------------|---------|
| `d` | Distância | quilômetros (km) |
| `t` | Tempo total | segundos (s) |
| `v` | Velocidade média | km/h |
| `p` | Pace | segundos/km |

## Conversões básicas

```
1 hora     = 3600 s
1 minuto   = 60 s
HH:MM:SS → t = HH*3600 + MM*60 + SS
MM:SS    → t = MM*60 + SS
```

## Fórmulas centrais

### Pace a partir de distância e tempo
```
p = t / d                    (segundos por km)
pace_str = floor(p/60):pad2(round(p mod 60))
```

### Velocidade a partir de distância e tempo
```
v = (d / t) * 3600           (km/h)
```

### Tempo a partir de distância e pace
```
t = p * d                    (segundos)
```

### Tempo a partir de distância e velocidade
```
t = (d / v) * 3600           (segundos)
```

### Conversão pace ↔ velocidade
```
v  = 3600 / p
p  = 3600 / v
```

## Regras de arredondamento

- **Pace:** truncar segundos ao inteiro mais próximo (`HALF_UP`).
  Exibir como `M:SS/km`. Ex.: `300,4 s/km → 5:00/km`; `300,6 s/km → 5:01/km`
- **Velocidade:** 2 casas decimais (`HALF_UP`). Ex.: `12,005 → 12,01`
- **Tempo total:** mostrar segundos inteiros, formato `HH:MM:SS`

## Exemplos de validação (oráculos para testes)

| Distância | Tempo | Pace esperado | Velocidade esperada |
|-----------|-------|---------------|---------------------|
| 5 km | 25:00 | 5:00/km | 12,00 km/h |
| 10 km | 50:00 | 5:00/km | 12,00 km/h |
| 10 km | 45:00 | 4:30/km | 13,33 km/h |
| 21,0975 km | 1:45:29 | 5:00/km | 12,00 km/h |
| 21,0975 km | 1:30:00 | 4:16/km | 14,07 km/h |
| 42,195 km | 3:00:00 | 4:16/km | 14,07 km/h |
| 42,195 km | 4:00:00 | 5:41/km | 10,55 km/h |

## Casos-borda

- **Distância zero ou negativa** → erro de validação
- **Tempo zero** → erro de validação
- **Pace > 15:00/km ou < 2:00/km** → aviso (não bloqueia, mas sinaliza)
- **Distância muito curta (< 0,1 km)** → permitir, mas avisar baixa precisão
- **Tempos > 24h** → bloquear (ultramaratona fora do MVP)

## Cálculo de splits

```
Para i = 1..N (passo s):
  split_acumulado(i) = i * p
  split_parcial(i)   = p * s
```

Onde `s` é o passo (1 km por padrão; 5 km para distâncias > 21 km).
