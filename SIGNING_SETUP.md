# Assinatura permanente — Welltech Mobile Agent

A partir da versão 0.2.0.2, o APK de instalação permanente deve usar uma identidade de assinatura fixa.

## Objetivo

Depois que a primeira versão permanente for instalada, as próximas versões podem ser instaladas por cima, preservando o aplicativo e seus dados, desde que:

- o applicationId continue `com.welltech.mobile`;
- a mesma chave de assinatura seja usada;
- o `versionCode` seja sempre crescente.

## Secrets exigidos no GitHub Actions

Configure no repositório:

- `WELLTECH_KEYSTORE_BASE64`
- `WELLTECH_KEYSTORE_PASSWORD`
- `WELLTECH_KEY_ALIAS`
- `WELLTECH_KEY_PASSWORD`

A chave privada nunca deve ser commitada no repositório.

## Migração da fase debug

As versões Alpha anteriores foram geradas como Debug e usam o pacote `com.welltech.mobile.debug`.

A primeira instalação permanente (`com.welltech.mobile`) é uma migração única. Remova a versão Debug antes de instalar a primeira versão permanente assinada.

Depois dessa migração, não é necessário desinstalar para atualizações normais.

## Build

O workflow `.github/workflows/build-signed-apk.yml` monta um Release assinado, verifica a assinatura com `apksigner` e publica o APK como artefato.

## Segurança

Perder a chave privada pode impedir novas atualizações de instalações existentes. Comprometer a chave permite que terceiros assinem APKs como se fossem da Welltech. Mantenha ao menos duas cópias offline protegidas.
