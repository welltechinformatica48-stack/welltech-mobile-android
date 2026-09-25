# Welltech Sync Status

Atualizado em: 2026-09-25

## Estado atual

- Desktop / Mobile Center: 0.2.13 (frente separada, fora deste repositório)
- Mobile Agent Android: 0.2.1 funcional sobre a linha de desenvolvimento atual
- Protocolo de integração: 1.0
- Transporte validado: USB/ADB + ADB forward para 127.0.0.1:37183
- Pareamento: código temporário de 6 dígitos
- Token de sessão: somente em memória

## Compatibilidade validada

Cadeia comprovada em teste real:

Windows -> USB/ADB -> Mobile Agent -> pareamento -> sessão -> capabilities -> device -> battery -> encerramento

## Regra de separação de frentes

1. O repositório `welltech-mobile-android` pertence ao Mobile Agent.
2. Alterações de Desktop não devem ser implementadas neste repositório.
3. Mudanças que afetem os dois lados devem primeiro ser registradas como contrato/pêndencia de sincronização.
4. A `main` deve permanecer estável.
5. Desenvolvimento do Agent ocorre em branch própria.
6. Desktop e Agent podem evoluir em ritmos diferentes desde que mantenham compatibilidade com o protocolo acordado.

## Estados esperados do pareamento no Desktop

O Desktop deve refletir a máquina de estados abaixo, sem antecipar sucesso visual:

- AGENT_NOT_FOUND
- AGENT_READY
- WAITING_PAIR_CODE
- PAIRING
- PAIRED
- SESSION_STARTING
- CONNECTED
- ERROR
- DISCONNECTED

O botão `PAREAR` só pode mudar para estado conectado depois de sucesso confirmado em `/pair` e `/session/start`.

## Pendências do Desktop 0.2.13

- Corrigir sincronização visual do pareamento.
- Não exibir `Pareado` antes da confirmação real da sessão.
- Tratar código de pareamento novo como substituição do anterior.
- Ocultar consoles auxiliares quando possível.
- Preservar fallback ADB quando o Agent não estiver disponível.

## Próxima integração

Depois da correção de pareamento no Desktop:

- combinar telemetria Agent + ADB;
- definir fonte preferencial por dado;
- manter fallback automático;
- versionar qualquer mudança de contrato antes de implementar em ambos os lados.
