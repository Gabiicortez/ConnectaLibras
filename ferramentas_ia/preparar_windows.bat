@echo off
cd /d "%~dp0"
if "%~1"=="" (
  echo Arraste a pasta da sua IA para este arquivo .bat.
  pause
  exit /b 1
)
py -3.11 preparar_modelos.py --origem "%~1"
if errorlevel 1 (
  echo Falha. Veja as instrucoes de dependencias no LEIA-ME_IA.md.
) else (
  echo Modelos preparados. Agora gere outro APK no Android Studio.
)
pause
