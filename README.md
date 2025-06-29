# WhatsUT - Sistema de Chat em Tempo Real

WhatsUT é um sistema de chat em tempo real inspirado no WhatsApp, desenvolvido como um projeto para a disciplina de Sistemas Distribuídos. O sistema permite comunicação em tempo real entre usuários, suporte a grupos, e compartilhamento de arquivos.

## Arquitetura

O projeto é dividido em duas partes principais:

### Backend (Java)
- Servidor WebSocket para comunicação em tempo real
- Serviços RMI para autenticação e gerenciamento de dados
- Armazenamento de mensagens, usuários e arquivos

### Frontend (React)
- Interface de usuário moderna e responsiva
- Comunicação em tempo real via WebSocket
- Suporte para envio de mensagens e arquivos

## Requisitos

- Java 11 ou superior
- Node.js 16 ou superior
- pnpm (gerenciador de pacotes para o frontend)
- Maven (para o backend)

## Como Executar

### Backend

1. Navegue até a pasta do backend:
   ```
   cd backend
   ```

2. Compile o projeto com Maven:
   ```
   mvn clean package
   ```

3. Execute o servidor:
   ```
   java -cp target/whatsut-backend-1.0-SNAPSHOT-jar-with-dependencies.jar br.com.whatsut.server.WhatsUTServer
   ```

### Frontend

1. Navegue até a pasta do frontend:
   ```
   cd front
   ```

2. Instale as dependências:
   ```
   pnpm install
   ```

3. Execute o servidor de desenvolvimento:
   ```
   pnpm dev
   ```

4. Acesse a aplicação em seu navegador:
   ```
   http://localhost:5173
   ```

## Funcionalidades

- **Autenticação**: Registro e login de usuários
- **Chat Privado**: Comunicação direta entre dois usuários
- **Grupos**: Criação e gerenciamento de grupos de chat
- **Compartilhamento de Arquivos**: Envio e recebimento de arquivos
- **Status Online/Offline**: Visualização do status dos usuários
- **Notificações**: Alertas para novas mensagens

## Estrutura do Projeto

### Backend
- `src/main/java/br/com/whatsut/server`: Classes principais do servidor
- `src/main/java/br/com/whatsut/websocket`: Implementação do servidor WebSocket
- `src/main/java/br/com/whatsut/rmi`: Serviços RMI
- `src/main/java/br/com/whatsut/dao`: Camada de acesso a dados
- `src/main/java/br/com/whatsut/model`: Modelos de dados

### Frontend
- `src/components`: Componentes React reutilizáveis
- `src/contexts`: Contextos React, incluindo WebSocketContext
- `src/stores`: Gerenciamento de estado com Zustand
- `src/pages`: Páginas da aplicação
- `src/utils`: Funções utilitárias

## Contribuição

Para contribuir com o projeto:

1. Faça um fork do repositório
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## Licença

Este projeto está licenciado sob a licença MIT - veja o arquivo LICENSE para mais detalhes.
