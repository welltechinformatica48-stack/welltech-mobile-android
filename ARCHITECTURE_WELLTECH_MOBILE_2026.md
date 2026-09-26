# Arquitetura Welltech Mobile — 2026

## Objetivo

Separar claramente três papéis do ecossistema Welltech sem duplicar regras de negócio:

1. **Welltech Desktop / Mobile Center** — bancada completa Windows, ADB/Fastboot, firmware, scrcpy, relatórios e orquestração.
2. **Welltech Mobile Agent** — agente Android no aparelho diagnosticado; expõe telemetria autorizada e nunca vira um painel administrativo do cliente.
3. **Welltech Mobile Console** — aplicativo do técnico para atendimento em campo, clientes, equipamentos, fotos, diagnóstico, orçamento, aprovação e relatório.

## Princípio de dados

Desktop e Mobile Console devem operar sobre o mesmo modelo de atendimento: cliente -> atendimento -> equipamentos -> diagnóstico -> orçamento -> aprovação -> execução -> validação -> entrega.

Cada equipamento possui sua própria subordem, diagnóstico, orçamento e status, mesmo quando vários aparelhos pertencem ao mesmo atendimento.

## Transportes do Agent

### USB/ADB
- transporte atual e validado;
- servidor local em 127.0.0.1:37183;
- ADB forward cria a ponte até o Desktop;
- fallback obrigatório.

### Wi-Fi local
- somente por ativação explícita do técnico/usuário;
- nunca abrir porta LAN em texto puro por padrão;
- exige autenticação forte e canal criptografado antes de telemetria;
- descoberta deve ser opcional;
- código/QR serve como autorização, não como substituto de criptografia;
- deve respeitar permissões de rede local do Android.

### Internet / 4G / 5G
- não expor porta do aparelho diretamente na Internet;
- usar conexão de saída do Agent para um Welltech Relay autenticado;
- sessão remota temporária, revogável e auditável;
- sem acesso remoto silencioso permanente por padrão.

## Mobile Console

Primeira navegação planejada:

- Início
- Atendimentos
- Clientes
- Equipamentos
- Diagnóstico
- Fotos
- Orçamento
- Aprovação
- Relatório
- Sincronização

## Segurança

- token de sessão efêmero e nunca persistido em relatório;
- autorização explícita no aparelho para diagnóstico remoto;
- TLS/criptografia obrigatória fora do loopback USB;
- logs não podem registrar token, código completo ou dados desnecessários;
- ações destrutivas exigem confirmação;
- Agent ausente ou indisponível não pode quebrar o fluxo ADB do Desktop.

## Compatibilidade

- versões do Desktop, Mobile Console e Agent são independentes;
- o Welltech Protocol tem versão própria;
- capabilities determinam o que cada lado consegue fazer;
- mudanças aditivas permanecem dentro do mesmo major;
- mudanças incompatíveis exigem nova versão major do protocolo.
