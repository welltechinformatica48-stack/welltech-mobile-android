# Welltech Mobile Agent

Continuação do APK Android existente da Welltech.

## Versão em desenvolvimento
**0.2.0-alpha**

## Mantido da 0.1.0
- Diagnóstico local somente leitura
- Android, build e patch de segurança
- CPU, RAM e armazenamento
- Bateria, temperatura e tensão
- UsageStats mediante autorização do Android
- Relatório compartilhável
- Visual preto + verde Welltech

## Adicionado na 0.2.0-alpha
- Protocolo Welltech 1.0
- Foreground Service para sessão técnica
- Servidor local em `127.0.0.1:37183`
- Pareamento explícito de 60 segundos
- Token forte por sessão
- `/api/v1/health`
- `/api/v1/pair`
- `/api/v1/session/start` e `/session/stop`
- `/api/v1/capabilities`
- `/api/v1/device`
- `/api/v1/battery`
- WebSocket `/api/v1/stream` com heartbeat

O Agent é complemento do Welltech Desktop/Mobile Center. ADB/Fastboot/scrcpy e firmware continuam no Desktop.
