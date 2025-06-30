#!/bin/bash

# Cores para saída
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # Sem cor

echo -e "${YELLOW}=== WhatsUT - Script de Inicialização ===${NC}"

# Verificar se a porta 8080 está em uso
echo -e "${YELLOW}Verificando se a porta 8080 está em uso...${NC}"
PORT_PID=$(lsof -ti:8080)
if [ ! -z "$PORT_PID" ]; then
    echo -e "${RED}Porta 8080 está em uso pelo processo $PORT_PID${NC}"
    read -p "Deseja encerrar este processo? (s/n): " KILL_PROCESS
    if [ "$KILL_PROCESS" = "s" ] || [ "$KILL_PROCESS" = "S" ]; then
        echo -e "${YELLOW}Encerrando processo...${NC}"
        kill -9 $PORT_PID
        sleep 2
        echo -e "${GREEN}Processo encerrado com sucesso!${NC}"
    else
        echo -e "${RED}Operação cancelada. Por favor, libere a porta 8080 manualmente ou altere a porta no código.${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}Porta 8080 está livre.${NC}"
fi

# Navegar para o diretório do backend
echo -e "${YELLOW}Navegando para o diretório do backend...${NC}"
cd backend || { echo -e "${RED}Diretório backend não encontrado!${NC}"; exit 1; }

# Verificar se o diretório target/classes existe, se não, criar
if [ ! -d "target/classes" ]; then
    echo -e "${YELLOW}Criando diretório target/classes...${NC}"
    mkdir -p target/classes
fi

# Compilar o projeto com Maven
echo -e "${YELLOW}Compilando o projeto com Maven...${NC}"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}Erro na compilação!${NC}"
    exit 1
fi
echo -e "${GREEN}Compilação concluída com sucesso!${NC}"

# Verificar se os serviços RMI estão registrados corretamente
echo -e "${YELLOW}Verificando implementação dos serviços RMI...${NC}"
if grep -q "registry.rebind(\"UserService\"" src/main/java/br/com/whatsut/server/WhatsUTServer.java; then
    echo -e "${GREEN}Serviço UserService está configurado corretamente.${NC}"
else
    echo -e "${RED}AVISO: Serviço UserService pode não estar registrado corretamente no WhatsUTServer.java${NC}"
    echo -e "${YELLOW}Verifique se existe uma linha como: registry.rebind(\"UserService\", new UserServiceImpl());${NC}"
fi

# Executar o servidor
echo -e "${YELLOW}Iniciando o servidor WhatsUT...${NC}"
java -jar target/whatsut-backend-1.0-SNAPSHOT-jar-with-dependencies.jar

# Este código nunca será executado se o servidor estiver rodando corretamente
echo -e "${RED}O servidor foi encerrado!${NC}"
