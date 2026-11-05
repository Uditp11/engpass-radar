@echo off
rem Baut das Projekt mit dem Maven Wrapper und startet es auf Payara Micro.
setlocal
cd /d "%~dp0"
call "%~dp0mvnw.cmd" -B -q package || exit /b 1
echo.
echo Engpass-Radar startet auf http://localhost:8080/
echo.
java -Dhazelcast.phone.home.enabled=false -jar target\payara-micro.jar --deploy target\engpass-radar.war --contextroot / --port 8080 --nocluster
