# Welltech Sync Status

Atualizado em: 2026-09-25

## Estado atual

- Desktop / Mobile Center — baseline oficial anterior: 0.2.12 Processos + scrcpy corrigidos.
- Desktop / Mobile Center — trilha de integração Agent: 0.2.14 State Sync, validada em teste real com Agent 0.2.2 para o fluxo de pareamento/sessão.
- Desktop / Mobile Center — trilha paralela de estabilidade de espelhamento: 0.2.13.1 Espelhamento Estável, criada após identificar tela preta/intermitência no scrcpy.
- Mobile Agent Android: 0.2.2-alpha validado em `dev/agent`.
- Mobile Agent Android 0.3: linha experimental em `feature/agent-0.3-ui-transport`, não mesclada e não substitui a 0.2.2 validada.
- Protocolo de integração: 1.0.
- Versão do contrato: 1.0.1.
- Transporte validado: USB/ADB + ADB forward para 127.0.0.1:37183.
- Pareamento: código temporário de 6 dígitos.
- Token de sessão: somente em memória.

## Compatibilidade validada

Cadeia comprovada em teste real:

Windows -> USB/ADB -> Mobile Agent -> pareamento -> sessão -> capabilities -> device -> battery -> encerramento

O Desktop 0.2.14 State Sync foi validado com o Agent 0.2.2 mostrando o fluxo visual correto e sem antecipar `Pareado/Conectado`.

A mudança do Agent 0.2.2 é aditiva: o protocolo permanece 1.0. Clientes antigos podem ignorar os novos campos do `/health`.

## Trilhas Desktop que precisam ser conciliadas

### 0.2.14 State Sync

Inclui a correção do fluxo de pareamento e consumo do estado público do Agent:

- PRONTO
- AGUARDANDO CÓDIGO
- PAREANDO
- PAREADO
- INICIANDO SESSÃO
- CONECTADO

Foi validado em teste real com o Agent 0.2.2.

### 0.2.13.1 Espelhamento Estável

Resultado da frente paralela de validação do Desktop após análise de gravação em que o scrcpy ficou preto/intermitente.

Correções registradas nessa trilha:

- acordar a tela do aparelho ao iniciar espelhamento;
- manter o aparelho desperto durante Tela/Somente Leitura;
- não desbloquear PIN/senha automaticamente;
- preservar o restante do Desktop e telemetria.

Próxima evolução dessa trilha: monitorar a sessão de espelhamento e registrar causas verificáveis quando a imagem falhar.

### Regra de reconciliação

Nenhuma das duas trilhas Desktop deve simplesmente sobrescrever a outra.

Antes da próxima promoção oficial do Desktop, a linha única deve conter ao mesmo tempo:

1. State Sync do Agent validado na 0.2.14;
2. estabilidade de espelhamento da 0.2.13.1;
3. fallback ADB;
4. detecção rápida sem diagnóstico pesado automático;
5. processos/scrcpy já corrigidos da baseline 0.2.12.

## Regra de separação de frentes

1. O repositório `welltech-mobile-android` pertence ao Mobile Agent.
2. Alterações de Desktop não devem ser implementadas neste repositório.
3. Mudanças que afetem os dois lados devem primeiro ser registradas como contrato ou sync packet.
4. A `main` deve permanecer estável.
5. Desenvolvimento estável do Agent ocorre em `dev/agent`.
6. Experimentos 0.3 permanecem em branch própria até validação.
7. Desktop e Agent podem evoluir em ritmos diferentes desde que mantenham compatibilidade com o protocolo acordado.

## Estado público fornecido pelo Agent 0.2.2

`GET /api/v1/health` fornece, sem expor token ou código:

- `agentVersion`
- `connectionState`: `idle`, `waiting_pair_code`, `paired`, `connected`
- `readyForPairing`
- `pairingRemainingSeconds`
- `paired`
- `active`

Mapeamento recomendado no Desktop:

- `idle` -> AGENT_READY
- `waiting_pair_code` -> WAITING_PAIR_CODE
- `paired` -> PAIRED
- `connected` -> CONNECTED
- `PAIRING` e `SESSION_STARTING` continuam estados transitórios locais do Desktop durante as requisições.

## Pendências compartilhadas atuais

- conciliar Desktop 0.2.14 State Sync com 0.2.13.1 Espelhamento Estável;
- eliminar consoles auxiliares visíveis;
- unificar identificação visual de versão do Agent;
- manter fallback ADB;
- combinar telemetria Agent + ADB com fonte identificável;
- avançar UI responsiva da linha Agent 0.3 sem alterar a 0.2.2 validada;
- Wi-Fi/relay somente após transporte criptografado e autorização explícita.
