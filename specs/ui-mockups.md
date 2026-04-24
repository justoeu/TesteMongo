# UI / UX

## Princípios

1. **Resultado em tempo real:** ao digitar, o resultado se atualiza (debounce 200 ms)
2. **Mobile-first:** thumb-friendly, inputs grandes
3. **Sem cadastro:** valor entregue na primeira interação
4. **Compartilhamento fácil:** botão "Copiar" sempre visível

## Wireframe — Tela principal (mobile)

```
+------------------------------------+
|  Start Runners — Calculadora       |
+------------------------------------+
|  Distância:                        |
|  [ 5km ] [10km] [21km] [42km] [+]  |
|  [____________________]  km        |
|                                    |
|  Modo:                             |
|  ( ) Tempo alvo                    |
|  ( ) Pace                          |
|  ( ) Velocidade                    |
|                                    |
|  Tempo: [HH] : [MM] : [SS]         |
|                                    |
|  ──────────────────────────────    |
|  Resultado                         |
|  ┌─────────────────────────────┐   |
|  │ Pace        5:00 /km        │   |
|  │ Velocidade  12,00 km/h      │   |
|  │ Tempo       50:00           │   |
|  └─────────────────────────────┘   |
|  [ Copiar ]   [ Salvar histórico ] |
|                                    |
|  Splits                            |
|  ┌─────┬──────────┬────────────┐   |
|  │ km  │ parcial  │ acumulado  │   |
|  │  1  │  5:00    │   5:00     │   |
|  │  2  │  5:00    │  10:00     │   |
|  │ ... │  ...     │   ...      │   |
|  └─────┴──────────┴────────────┘   |
+------------------------------------+
```

## Estados visuais

- **Vazio:** mostrar exemplo placeholder (ex.: "10 km em 50:00")
- **Carregando:** spinner inline (apenas se cálculo for via API)
- **Erro:** banner vermelho acima dos inputs; campo afetado com borda vermelha
- **Sucesso:** card de resultado com leve animação fade-in

## Acessibilidade (WCAG 2.1 AA)

- Inputs com `<label>` explícito; sem placeholder como label
- Foco visível (outline 2 px)
- `aria-live="polite"` na área de resultado
- Contraste mínimo 4.5:1
- Operável 100% via teclado: Tab, Enter, setas

## Responsividade

| Viewport | Layout |
|----------|--------|
| ≤ 640 px | 1 coluna, atalhos em chips horizontais com scroll |
| 641–1024 px | 2 colunas: inputs à esquerda, resultado à direita |
| ≥ 1025 px | 3 colunas: inputs / resultado / splits |

## Tema

- Modo claro padrão; modo escuro automático via `prefers-color-scheme`
- Paleta: primária `#0F62FE` (azul), erro `#DA1E28`, sucesso `#198038`
