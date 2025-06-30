# WhatsUT - Aplicativo de Mensagens com Java RMI e WebSocket

Este é um aplicativo de mensagens que utiliza Java RMI para comunicação entre serviços backend e WebSocket para comunicação em tempo real com o frontend React.

## Requisitos

- Java 11 ou superior
- Node.js 14 ou superior
- npm 6 ou superior

## Estrutura do Projeto

- `backend/`: Servidor Java RMI e WebSocket
- `front/`: Cliente React

## Como Executar

### Backend

1. Navegue até a pasta do backend:
```bash
cd backend
```

2. Compile o projeto:
```bash
javac -d target/classes src/main/java/br/com/whatsut/**/*.java
```

3. Execute o servidor:
```bash
java -cp target/classes br.com.whatsut.server.WhatsUTServer
```

### Frontend

1. Navegue até a pasta do frontend:
```bash
cd front
```

2. Instale as dependências:
```bash
npm install
```

3. Execute o servidor de desenvolvimento:
```bash
npm start
```

## Solução de Problemas

### Erro: "NotBoundException: UserService"

Este erro ocorre quando o serviço RMI UserService não está registrado corretamente. Verifique se:

1. O servidor RMI está em execução
2. O serviço UserService está sendo registrado corretamente no WhatsUTServer.java

Solução:
```java
// Certifique-se de que o serviço está sendo registrado assim:
registry.rebind("UserService", new UserServiceImpl());
```

### Erro: "BindException: Address already in use"

Este erro ocorre quando a porta 8080 (ou outra porta configurada) já está sendo utilizada por outro processo.

Para resolver:

1. Encontre o processo que está usando a porta:
```bash
# No macOS/Linux
lsof -i :8080

# No Windows
netstat -ano | findstr :8080
```

2. Encerre o processo:
```bash
# No macOS/Linux
kill -9 [PID]

# No Windows
taskkill /F /PID [PID]
```

3. Ou altere a porta do servidor no arquivo WhatsUTWebSocketServer.java:
```java
// Altere a porta para outra disponível, por exemplo:
new WhatsUTWebSocketServer(8081);
```

## Script de Execução Automática

Para facilitar a execução, você pode usar o script `run.sh` (macOS/Linux) ou `run.bat` (Windows) na raiz do projeto. Este script:

1. Compila o projeto
2. Verifica se a porta está em uso e encerra o processo, se necessário
3. Inicia o servidor

Para executar:
```bash
# No macOS/Linux
./run.sh

# No Windows
run.bat
```

## Funcionalidades

- Autenticação de usuários
- Conversas privadas
- Grupos de chat
- Envio de mensagens em tempo real
- Lista de usuários online
- Status de usuário
