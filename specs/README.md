# Specs — Calculadora Interativa de Ritmo de Corrida (Start Runners)

Esta pasta contém o **Software Design Document (SDD)** completo e os documentos
auxiliares para a funcionalidade **Calculadora Interativa de Ritmo de Corrida**.

> Observação: estes specs foram redigidos para ser portáveis para um repositório
> dedicado (`justoeu/start-runners`). Eles seguem o estilo já adotado no projeto
> `TesteMongo` (Java 17, Maven, JUnit 5, threshold de qualidade rígido).

## Índice

| Documento | Descrição |
|-----------|-----------|
| [`SDD.md`](./SDD.md) | Software Design Document completo (visão geral, arquitetura, modelagem, decisões) |
| [`requirements.md`](./requirements.md) | Requisitos funcionais e não funcionais |
| [`use-cases.md`](./use-cases.md) | Casos de uso detalhados (UC-01 a UC-05) |
| [`formulas.md`](./formulas.md) | Modelo matemático: pace, velocidade, conversões e arredondamento |
| [`api.md`](./api.md) | Contrato de API REST (request/response, status codes, erros) |
| [`ui-mockups.md`](./ui-mockups.md) | Especificação de interface (wireframes ASCII, fluxos, acessibilidade) |
| [`architecture.md`](./architecture.md) | Decisões arquiteturais (camadas, padrões, tecnologias) |
| [`test-plan.md`](./test-plan.md) | Plano de testes (unitários, integração, mutação, casos-borda) |
| [`glossary.md`](./glossary.md) | Glossário de termos do domínio de corrida |

## Como ler

1. Comece por [`requirements.md`](./requirements.md) — entenda **o quê** será construído
2. Avance para [`use-cases.md`](./use-cases.md) — entenda **como o usuário interage**
3. Leia [`formulas.md`](./formulas.md) — base matemática que sustenta tudo
4. Passe ao [`SDD.md`](./SDD.md) — visão de engenharia consolidada
5. Use [`api.md`](./api.md), [`ui-mockups.md`](./ui-mockups.md) e [`architecture.md`](./architecture.md) como referências técnicas
6. Verifique [`test-plan.md`](./test-plan.md) antes de implementar

## Status

| Fase | Status |
|------|--------|
| Especificação | Concluída |
| Implementação | Pendente |
| Testes | Pendente |
| Deploy | Pendente |
