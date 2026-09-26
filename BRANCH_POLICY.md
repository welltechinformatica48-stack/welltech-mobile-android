# Política de Branches — Welltech

## Objetivo

Permitir desenvolvimento paralelo do Welltech Desktop e do Welltech Mobile Agent sem colisão entre chats, builds ou versões.

## Mobile Agent

Repositório: `welltechinformatica48-stack/welltech-mobile-android`

- `main`: estável; não recebe desenvolvimento experimental direto.
- `dev/agent`: desenvolvimento contínuo do Mobile Agent.
- branches de feature podem nascer de `dev/agent`, por exemplo `feature/agent-battery-health`.

## Desktop / Mobile Center

O Desktop deve viver em repositório ou linha de desenvolvimento própria. Enquanto não estiver conectado a este GitHub, este repositório não deve receber código do Desktop.

Quando houver repositório Desktop, usar:

- `main`: estável.
- `dev/desktop`: desenvolvimento contínuo do Desktop.
- branches de feature a partir de `dev/desktop`.

## Sincronização

Mudanças que afetem ambos os lados seguem esta ordem:

1. Atualizar contrato em `contracts/`.
2. Atualizar `SYNC_STATUS.md`.
3. Implementar no Agent em `dev/agent`.
4. Implementar no Desktop em `dev/desktop`.
5. Validar compatibilidade cruzada.
6. Só então promover cada frente para sua `main`.

## Regra anti-colisão

Um chat pode registrar pendência da outra frente, mas não deve implementar código pertencente à outra frente no repositório errado.
