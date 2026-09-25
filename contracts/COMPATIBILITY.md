# Compatibilidade Desktop ↔ Agent

## Matriz atual

| Desktop / Mobile Center | Mobile Agent | Protocolo | Estado |
|---|---|---|---|
| 0.2.13 | 0.2.1 | 1.0 | Compatível em teste USB/ADB |

## Regras

- O Desktop deve tolerar campos opcionais ausentes.
- O Agent deve ignorar campos opcionais desconhecidos quando seguro.
- Mudanças incompatíveis exigem nova versão principal do protocolo.
- Falha do Agent deve permitir fallback ADB no Desktop quando possível.
