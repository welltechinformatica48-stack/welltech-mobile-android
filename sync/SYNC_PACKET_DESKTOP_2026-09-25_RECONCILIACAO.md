# SYNC PACKET — Desktop ↔ Agent — 2026-09-25

## Origem das informações

Este pacote consolida duas frentes Desktop que evoluíram em paralelo e a frente Mobile Agent.

## Desktop — baseline e trilhas

### Baseline oficial anterior

- 0.2.12 — Processos + scrcpy corrigidos.

### Trilha A — integração Agent

- Desktop 0.2.14 State Sync.
- Validado com Mobile Agent 0.2.2.
- Pareamento/sessão visualmente sincronizados.
- `Conectado` só aparece depois de `/pair` + `/session/start` confirmados.
- Fallback ADB preservado.

### Trilha B — estabilidade de espelhamento

- Desktop 0.2.13.1 Espelhamento Estável.
- Criado depois de análise de gravação com tela preta/intermitência no scrcpy.
- Acorda a tela ao iniciar espelhamento.
- Mantém o aparelho desperto durante Tela/Somente Leitura.
- Não tenta desbloquear PIN/senha automaticamente.
- Próximo passo dessa trilha: monitor de sessão/categorias de falha verificáveis.

## Mobile Agent

### Linha validada

- Agent 0.2.2-alpha.
- Protocol 1.0 / contract 1.0.1.
- USB/ADB + forward local 127.0.0.1:37183.
- Estados públicos em `/api/v1/health`: `idle`, `waiting_pair_code`, `paired`, `connected`.
- Código temporário de 6 dígitos.
- Token somente em memória.

### Linha experimental

- Agent 0.3 em `feature/agent-0.3-ui-transport`.
- Não substitui 0.2.2.
- Objetivos: UI responsiva, identificação de transporte, preparação para Wi-Fi local seguro e futuro Mobile Console.
- Wi-Fi e relay permanecem bloqueados até criptografia/autorização adequadas.

## Regra obrigatória de reconciliação

A próxima linha única do Desktop deve incorporar, sem regressão:

1. baseline 0.2.12 de processos/scrcpy;
2. State Sync da 0.2.14;
3. estabilidade de espelhamento da 0.2.13.1;
4. detecção rápida do Android sem inspeção pesada automática;
5. fallback ADB quando Agent estiver ausente/antigo/sem permissão;
6. nenhuma tentativa automática de desbloquear PIN/senha.

## Pendências cruzadas

- eliminar console/janelas pretas auxiliares do Desktop;
- unificar strings visuais de versão do Agent;
- combinar telemetria Agent + ADB com fonte por métrica;
- adicionar monitor de sessão de espelhamento;
- manter versões Desktop/Agent independentes;
- qualquer mudança incompatível exige nova revisão de protocolo.

## Instrução para os dois chats

Antes de iniciar mudança que afete a outra frente:

1. ler `SYNC_STATUS.md`;
2. ler este sync packet;
3. implementar apenas na branch da própria frente;
4. registrar novo sync packet se a mudança cruzar Desktop ↔ Agent;
5. não promover versão oficial antes de teste de integração real.
