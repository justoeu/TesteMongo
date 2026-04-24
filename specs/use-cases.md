# Casos de Uso

## UC-01 — Calcular pace para terminar 10 km em 50 min

**Ator:** Corredor amador
**Pré-condições:** Acesso à aplicação
**Fluxo principal:**
1. Usuário acessa a tela inicial
2. Seleciona o atalho **10 km**
3. Digita `50:00` no campo "tempo alvo"
4. Clica em **Calcular** (ou o resultado atualiza ao digitar)
5. Sistema exibe:
   - Pace: **5:00/km**
   - Velocidade: **12,00 km/h**
   - Tabela de splits (1 km → 5:00, 2 km → 10:00, …)

**Pós-condições:** Cálculo é exibido; opcionalmente salvo no histórico local

**Fluxo alternativo A1 — entrada inválida:**
- 4a. Usuário digita `0:00` → sistema exibe erro:
  "O tempo alvo deve ser maior que zero."

---

## UC-02 — Calcular tempo previsto para meia maratona a 5:30/km

**Ator:** Treinador
**Fluxo principal:**
1. Usuário seleciona **21 km (Meia Maratona)**
2. Alterna para a aba **"A partir do pace"**
3. Digita `5:30` em pace
4. Sistema exibe:
   - Tempo previsto: **1:56:02**
   - Velocidade: **10,91 km/h**
   - Tabela de splits a cada 1 km

---

## UC-03 — Calcular para distância customizada

**Ator:** Corredor amador
**Fluxo principal:**
1. Usuário escolhe **Distância customizada**
2. Digita `7,5` km
3. Digita tempo `42:30`
4. Sistema exibe pace `5:40/km` e velocidade `10,59 km/h`

**Validação:**
- Aceitar tanto vírgula quanto ponto como separador decimal
- Rejeitar valores ≤ 0 ou > 500 km

---

## UC-04 — Copiar resultado para compartilhar

**Ator:** Corredor amador
**Fluxo principal:**
1. Após qualquer cálculo (UC-01 a UC-03)
2. Usuário clica no botão **Copiar**
3. Sistema copia para clipboard:
   `10 km em 50:00 — pace 5:00/km, velocidade 12,00 km/h`
4. Sistema exibe toast: "Copiado!"

---

## UC-05 — Recuperar histórico local

**Ator:** Corredor amador
**Pré-condições:** Existem cálculos salvos no `localStorage`
**Fluxo principal:**
1. Usuário acessa aba **Histórico**
2. Sistema lista até 20 cálculos mais recentes (ordem decrescente)
3. Usuário clica em um item → tela carrega valores e recalcula

**Fluxo alternativo:**
- Usuário clica em **Limpar histórico** → confirmação → `localStorage` zerado
