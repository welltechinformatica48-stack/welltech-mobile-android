# Welltech Mobile Protocol 1.0

## Objetivo

Este contrato define a integração mínima compatível entre o Welltech Desktop / Mobile Center e o Welltech Mobile Agent Android.

## Transporte

- Conexão física: USB.
- Descoberta e preparação: ADB.
- Encaminhamento: `adb forward tcp:<porta-local> tcp:37183`.
- O Desktop pode escolher uma porta local dinâmica.
- O Agent atende localmente na porta `37183`.

## Princípios

- Detectar o aparelho não inicia diagnóstico completo.
- O Desktop pode preparar/abrir o Agent automaticamente.
- O pareamento exige ação explícita do técnico no celular.
- O código temporário possui 6 dígitos e expira.
- Um novo código substitui o código anterior.
- Token de sessão não deve ser persistido em relatório ou arquivo de configuração.
- Ausência/falha do Agent não pode derrubar o fallback ADB do Desktop.

## Fluxo mínimo

1. Desktop detecta o dispositivo por ADB.
2. Desktop identifica se o pacote do Agent está instalado.
3. Desktop prepara o Agent via ADB.
4. Desktop cria o forward para a porta 37183.
5. Desktop consulta `/api/v1/health`.
6. Técnico abre o pareamento no celular.
7. Desktop envia o código para `/api/v1/pair`.
8. Após sucesso em `/pair`, Desktop inicia sessão em `/api/v1/session/start`.
9. Com sessão ativa, Desktop pode consultar:
   - `/api/v1/capabilities`
   - `/api/v1/device`
   - `/api/v1/battery`
10. Ao desconectar/fechar, sessão e forward devem ser limpos.

## Máquina de estados do Desktop

- `AGENT_NOT_FOUND`
- `AGENT_READY`
- `WAITING_PAIR_CODE`
- `PAIRING`
- `PAIRED`
- `SESSION_STARTING`
- `CONNECTED`
- `ERROR`
- `DISCONNECTED`

### Regra crítica

`CONNECTED` só pode ser exibido após confirmação de sucesso de `/pair` e `/session/start`.

## Compatibilidade

- Campo desconhecido recebido: ignorar quando não for obrigatório.
- Campo opcional ausente: usar fallback sem encerrar a sessão.
- Endpoint obrigatório indisponível: degradar para ADB quando houver equivalente.
- Mudanças incompatíveis exigem nova versão principal do protocolo.
- Mudanças aditivas e opcionais podem permanecer na versão 1.x, desde que não quebrem clientes 1.0.
