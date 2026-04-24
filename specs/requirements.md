# Requisitos — Calculadora Interativa de Ritmo de Corrida

## 1. Visão de Produto

Aplicação web/CLI que permite a corredores calcularem **pace (min/km)** e
**velocidade média (km/h)** para distâncias canônicas (5 km, 10 km, 21,0975 km,
42,195 km) e personalizadas, a partir de um **tempo alvo** informado.
Adicionalmente, permite o cálculo inverso: dado um pace ou velocidade, prever o
tempo total para uma distância.

## 2. Personas

| Persona | Necessidade |
|---------|-------------|
| Corredor iniciante | Saber em que ritmo precisa correr para terminar 5 km em 30 min |
| Corredor amador | Planejar treinos com paces específicos (Z2, Z3, limiar) |
| Treinador | Validar metas de prova rapidamente para múltiplos atletas |

## 3. Requisitos Funcionais

### RF-01 — Cálculo de pace e velocidade a partir do tempo alvo
- **Entrada:** distância (km, decimal positivo) e tempo alvo (HH:MM:SS ou MM:SS)
- **Saída:** pace em `min:ss/km` e velocidade em `km/h` (2 casas decimais)
- **Critério de aceite:** para 10 km em 50:00 → pace 5:00/km e velocidade 12,00 km/h

### RF-02 — Distâncias pré-configuradas
- A interface deve oferecer botões/atalhos para: **5 km, 10 km, 21,0975 km (Meia
  Maratona), 42,195 km (Maratona)** e **distância customizada**

### RF-03 — Cálculo inverso (tempo previsto)
- **Entrada:** distância + (pace `min:ss/km` **OU** velocidade `km/h`)
- **Saída:** tempo total estimado (`HH:MM:SS`)
- **Critério de aceite:** 21,0975 km a 5:00/km → 1:45:29

### RF-04 — Tabela de splits
- Para cada cálculo, exibir tabela com tempo acumulado por km (1, 2, 3, …, N)
- Suportar passos personalizados (1 km, 5 km) para distâncias longas

### RF-05 — Validação de entrada
- Distância: > 0 e ≤ 500 km
- Tempo: > 0 e ≤ 24 horas
- Pace: entre `2:00/km` e `15:00/km` (faixa fisiologicamente razoável)
- Velocidade: entre `4 km/h` e `30 km/h`
- Mensagens de erro claras e localizadas (PT-BR)

### RF-06 — Persistência opcional do histórico
- Permitir ao usuário salvar cálculos em histórico local (não autenticado)
- Backend: persistir em MongoDB quando autenticado (futuro — fora do MVP)

### RF-07 — Cópia rápida
- Botão "copiar resultado" formata texto pronto para colar em redes sociais:
  `10 km em 50:00 — pace 5:00/km, velocidade 12,00 km/h`

## 4. Requisitos Não Funcionais

### RNF-01 — Performance
- Cálculo client-side em < 50 ms (operação puramente aritmética)
- API REST: P95 < 200 ms

### RNF-02 — Precisão
- Cálculo internamente em `BigDecimal` (Java) ou `Number` com cuidado de
  truncamento (JS)
- Arredondamento padrão **HALF_UP** para apresentação
- Pace exibido com truncamento ao segundo inteiro

### RNF-03 — Acessibilidade
- WCAG 2.1 AA: contraste, navegação por teclado, ARIA-labels
- Suporte a leitor de tela para inputs de tempo

### RNF-04 — Internacionalização
- Pt-BR como padrão; en-US como fallback
- Suporte a separador decimal vírgula/ponto na entrada

### RNF-05 — Compatibilidade
- Browsers: 2 últimas versões de Chrome, Firefox, Safari, Edge
- Mobile-first (320 px de largura mínima)

### RNF-06 — Segurança
- Sanitização de input (defesa em profundidade mesmo sendo apenas numérico)
- CSP estrita; sem `eval`
- Rate limiting na API: 60 req/min por IP

### RNF-07 — Qualidade de código
- Cobertura de testes: ≥ 80% linhas
- Mutation score (PITest): ≥ 60%
- Build falha com CVE CVSS ≥ 7.0 (OWASP Dependency Check)

## 5. Fora de Escopo (não-MVP)

- Login/autenticação
- Sincronização com Strava/Garmin
- Predição de tempos por VO2max ou fórmulas de Riegel/Cameron
- Planos de treino personalizados
- Gráficos históricos

## 6. Métricas de Sucesso

| Métrica | Alvo |
|---------|------|
| Tempo médio para obter resultado | < 5 segundos |
| Taxa de erro de validação | < 2% das submissões |
| Disponibilidade | ≥ 99,5% |
