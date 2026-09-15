@echo off
rem Быстрый запуск собранного Desktop-дистрибутива.
rem После сборки (.\gradlew lwjgl3:installDist) запускать игру можно этим файлом без пересборки.
call "%~dp0lwjgl3\build\install\lwjgl3\bin\lwjgl3.bat" %*