# Roadmap — Agent 0.3 + Welltech Mobile Console

## Fase A — Agent 0.3: interface e base de transporte

1. Corrigir proporções, paddings, tipografia e navegação para diferentes tamanhos de tela.
2. Separar visualmente conexão, pareamento, diagnóstico e permissões.
3. Introduzir abstração de transporte com modos USB_LOOPBACK, WIFI_LOCAL_SECURE e REMOTE_RELAY.
4. Manter USB_LOOPBACK como único transporte habilitado até o canal Wi-Fi estar criptografado.
5. Preparar permissões Android necessárias para LAN.
6. Exibir fonte da conexão e estado real da sessão.

## Fase B — Wi-Fi local seguro

1. Pareamento explícito por código/QR.
2. Canal criptografado e autenticação mútua/pinning.
3. Descoberta opcional do Agent na LAN.
4. Rejeitar cliente não autorizado antes de qualquer telemetria.
5. Timeout, revogação e reconexão segura.
6. Teste Desktop <-> Agent na mesma rede sem cabo.

## Fase C — Mobile Console do técnico

MVP:
- login/perfil técnico local;
- clientes;
- atendimentos;
- vários equipamentos por atendimento;
- fotos/anexos;
- defeito relatado;
- checklist e diagnóstico;
- solução proposta;
- orçamento;
- aprovação/recusa;
- relatório e compartilhamento;
- sincronização com Desktop quando disponível.

## Fase D — integração de diagnóstico

- Mobile Console descobre/conecta a Agent autorizado;
- lê capabilities;
- coleta device/battery e demais módulos disponíveis;
- identifica a fonte de cada dado;
- não inventa dado indisponível;
- mantém diagnóstico manual quando Agent não estiver disponível.

## Fase E — acesso remoto Internet/4G/5G

- Welltech Relay;
- conexão de saída do Agent;
- TLS forte;
- autorização explícita;
- sessão temporária;
- trilha de auditoria;
- revogação instantânea;
- sem porta pública no aparelho.

## Critério para avançar de fase

Nenhuma fase substitui a anterior até passar em teste real. USB/ADB continua sendo fallback estável durante toda a evolução.
