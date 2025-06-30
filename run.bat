@echo off
setlocal enabledelayedexpansion

echo === WhatsUT - Script de Inicializacao ===

:: Verificar se a porta 8080 esta em uso
echo Verificando se a porta 8080 esta em uso...
netstat -ano | findstr :8080 > temp.txt
set /p PORT_INFO=<temp.txt
del temp.txt

if not "!PORT_INFO!"=="" (
    for /f "tokens=5" %%a in ("!PORT_INFO!") do set PORT_PID=%%a
    echo Porta 8080 esta em uso pelo processo !PORT_PID!
    set /p KILL_PROCESS=Deseja encerrar este processo? (s/n): 
    if /i "!KILL_PROCESS!"=="s" (
        echo Encerrando processo...
        taskkill /F /PID !PORT_PID!
        timeout /t 2 > nul
        echo Processo encerrado com sucesso!
    ) else (
        echo Operacao cancelada. Por favor, libere a porta 8080 manualmente ou altere a porta no codigo.
        exit /b 1
    )
) else (
    echo Porta 8080 esta livre.
)

:: Navegar para o diretorio do backend
echo Navegando para o diretorio do backend...
cd backend || (
    echo Diretorio backend nao encontrado!
    exit /b 1
)

:: Verificar se o diretorio target\classes existe, se nao, criar
if not exist "target\classes" (
    echo Criando diretorio target\classes...
    mkdir target\classes
)

:: Compilar o projeto com Maven
echo Compilando o projeto com Maven...
call mvn clean package -DskipTests
if %ERRORLEVEL% neq 0 (
    echo Erro na compilacao!
    exit /b 1
)
echo Compilacao concluida com sucesso!

:: Verificar se os servicos RMI estao registrados corretamente
echo Verificando implementacao dos servicos RMI...
findstr "registry.rebind(\"UserService\"" src\main\java\br\com\whatsut\server\WhatsUTServer.java > nul
if %ERRORLEVEL% equ 0 (
    echo Servico UserService esta configurado corretamente.
) else (
    echo AVISO: Servico UserService pode nao estar registrado corretamente no WhatsUTServer.java
    echo Verifique se existe uma linha como: registry.rebind("UserService", new UserServiceImpl^(^)^);
)

:: Executar o servidor
echo Iniciando o servidor WhatsUT...
java -jar target\whatsut-backend-1.0-SNAPSHOT-jar-with-dependencies.jar

:: Este codigo nunca sera executado se o servidor estiver rodando corretamente
echo O servidor foi encerrado!
