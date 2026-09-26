# SYNC PACKET — Desktop 0.2.15 Wi-Fi ADB Preview

Data: 2026-09-25

## Objetivo

Adicionar uso do Welltech Mobile Center sem cabo USB em Android 11+ sem expor diretamente a API local do Mobile Agent na LAN.

## Estratégia adotada

O Wi-Fi desta primeira etapa usa o mecanismo oficial de Depuração sem fio do ADB:

1. PC e Android na mesma rede Wi-Fi;
2. usuário ativa `Opções do desenvolvedor > Depuração sem fio`;
3. Desktop executa pareamento ADB usando IP:porta + código temporário do Android;
4. Desktop conecta ao IP:porta principal da Depuração sem fio;
5. o aparelho passa a aparecer no ADB com serial `IP:porta` e estado `device`;
6. todo o fluxo Welltech existente continua igual sobre esse transporte;
7. o Mobile Agent continua ouvindo somente em `127.0.0.1:37183` no aparelho;
8. o Desktop usa `adb forward` como já fazia no USB.

## Segurança

- nenhuma porta do Agent é aberta diretamente na LAN;
- token do Agent continua somente em memória;
- o código do Agent continua temporário e independente do código de pareamento ADB;
- autorização de Depuração sem fio continua sob controle do Android/usuário;
- USB permanece como fallback;
- `WIFI_LOCAL_SECURE` direto no servidor do Agent continua BLOQUEADO até existir camada criptográfica própria.

## Desktop 0.2.15 Preview

Base: Desktop 0.2.14 State Sync.

Adicionado:

- botão `Wi-Fi sem cabo`;
- diálogo de pareamento ADB Wi-Fi;
- entrada de IP:porta de pareamento;
- entrada de código de 6 dígitos do Android;
- entrada de IP:porta de conexão;
- execução de `adb pair`;
- execução de `adb connect`;
- confirmação de estado `device`;
- seleção do serial Wi-Fi (`IP:porta`);
- detecção automática preparada para USB ou Wi-Fi;
- botão `Desconectar Wi-Fi`.

## Compatibilidade esperada

- Android 11+;
- Agent 0.2.2-alpha ou linha 0.3 experimental;
- Protocol 1.0 / contract 1.0.1;
- Desktop 0.2.15 Preview usa o mesmo fluxo `/health`, `/pair`, `/session/start`, `/capabilities`, `/device`, `/battery` através do `adb forward`.

## Regra para a frente Agent

Não alterar o bind do `LocalAgentServer` para `0.0.0.0` por causa deste Wi-Fi.

O modo sem cabo desta etapa é ADB Wireless Debugging + tunnel local, não exposição direta da API Agent.

## Teste real obrigatório

No Moto G84 Android 15:

1. ativar Depuração sem fio;
2. parear pelo Desktop 0.2.15;
3. confirmar `device` sem USB;
4. abrir cartão;
5. preparar Agent;
6. abrir pareamento do Agent;
7. confirmar sessão `CONNECTED`;
8. validar telemetria;
9. validar scrcpy sem cabo;
10. desligar/religar Wi-Fi e testar reconexão;
11. manter 0.2.14 como referência até o teste terminar.
