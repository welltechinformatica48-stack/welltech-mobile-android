# Teste USB/ADB — Welltech Mobile Agent

Este pacote valida a comunicação real entre um computador Windows e o Welltech Mobile Agent pelo Protocolo 1.0.

## O que ele testa

1. Detecta ADB.
2. Detecta exatamente um Android autorizado.
3. Reconhece o Agent oficial `com.welltech.mobile` ou o Alpha `com.welltech.mobile.debug`.
4. Abre o aplicativo no celular.
5. Cria `adb forward tcp:37183 tcp:37183`.
6. Valida `GET /api/v1/health`.
7. Usa o código temporário de 6 dígitos para `POST /api/v1/pair`.
8. Inicia a sessão autenticada.
9. Consulta capabilities, device e battery.
10. Encerra a sessão, revoga o token e remove o forward.
11. Salva um relatório JSON sem persistir o bearer token.

## Uso

No Windows, execute:

`1-TESTAR_AGENT_WELLTECH.cmd`

O script tentará localizar `adb.exe` no PATH, na pasta local `platform-tools`, no Android SDK do usuário ou em `C:\platform-tools`.

No celular, mantenha a depuração USB autorizada. Quando o aplicativo abrir, toque em **ABRIR PAREAMENTO** e informe ao script o código de 6 dígitos.

Se o Agent não responder, o teste tenta salvar um logcat filtrado na pasta `resultado`, facilitando o diagnóstico de crash.

## Segurança

O teste usa somente o túnel ADB local para `127.0.0.1:37183`. O token é mantido apenas em memória e não é gravado no relatório.
