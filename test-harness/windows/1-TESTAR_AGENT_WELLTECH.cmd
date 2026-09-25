@echo off
title Welltech Mobile Agent - Teste USB/ADB
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0TESTAR_AGENT_WELLTECH.ps1"
