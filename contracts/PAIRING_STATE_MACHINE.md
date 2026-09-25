# Máquina de Estados do Pareamento

Estados oficiais para o Desktop / Mobile Center ao integrar com o Mobile Agent:

1. `AGENT_NOT_FOUND`
2. `AGENT_READY`
3. `WAITING_PAIR_CODE`
4. `PAIRING`
5. `PAIRED`
6. `SESSION_STARTING`
7. `CONNECTED`
8. `ERROR`
9. `DISCONNECTED`

## Transições críticas

- `AGENT_READY -> WAITING_PAIR_CODE`: Agent preparado e aguardando código.
- `WAITING_PAIR_CODE -> PAIRING`: técnico enviou código atual.
- `PAIRING -> PAIRED`: `/api/v1/pair` confirmou sucesso.
- `PAIRED -> SESSION_STARTING`: Desktop iniciou `/api/v1/session/start`.
- `SESSION_STARTING -> CONNECTED`: sessão confirmada.
- Qualquer falha de autenticação, expiração ou transporte deve ir para `ERROR` ou retornar a `WAITING_PAIR_CODE` conforme o caso.

## Regras de UI

- Botão deve mostrar `PAREAR` até a confirmação real de `/pair`.
- A interface não deve mostrar `PAREADO` ou `CONECTADO` antecipadamente.
- Ao gerar novo código no celular, o código anterior torna-se inválido para a UI e para novas tentativas.
- O estado visual deve ser derivado da resposta real do Agent, não apenas do clique do usuário.
