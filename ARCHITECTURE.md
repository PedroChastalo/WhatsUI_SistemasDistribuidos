# Arquitetura do WhatsUT

Este documento descreve a arquitetura técnica do sistema WhatsUT, incluindo diagramas e fluxos de dados.

## Visão Geral da Arquitetura

O WhatsUT é construído com uma arquitetura cliente-servidor distribuída:

```
+-------------------+        WebSocket        +-------------------+
|                   |<----------------------->|                   |
|  Cliente (React)  |                         |  Servidor (Java)  |
|                   |<----------------------->|                   |
+-------------------+        REST API         +-------------------+
                                                      |
                                                      | RMI
                                                      v
                                             +-------------------+
                                             |                   |
                                             | Serviços de Dados |
                                             |                   |
                                             +-------------------+
```

## Componentes Principais

### Frontend (React)

- **Interface do Usuário**: Componentes React para visualização e interação
- **Gerenciamento de Estado**: Zustand para gerenciar o estado global da aplicação
- **Comunicação em Tempo Real**: WebSocket para comunicação bidirecional com o servidor
- **Comunicação REST**: Chamadas REST para operações como upload de arquivos

### Backend (Java)

- **Servidor WebSocket**: Gerencia conexões em tempo real e distribui mensagens
- **Serviços RMI**: Implementa a lógica de negócios e acesso a dados
- **Armazenamento de Dados**: Gerencia usuários, mensagens, grupos e arquivos

## Fluxo de Dados

### Autenticação

```
+--------+                                +--------+                      +--------+
| Cliente |                               | WebSocket|                     | Serviço |
|        |                               | Server  |                      | Autent. |
+--------+                               +--------+                      +--------+
    |                                        |                               |
    | 1. Envia credenciais (login/register)  |                               |
    |--------------------------------------->|                               |
    |                                        | 2. Valida credenciais         |
    |                                        |------------------------------>|
    |                                        |                               |
    |                                        | 3. Retorna resultado          |
    |                                        |<------------------------------|
    | 4. Recebe sessionId e token            |                               |
    |<---------------------------------------|                               |
    |                                        |                               |
```

### Envio de Mensagem

```
+--------+                                +--------+                      +--------+
| Cliente |                               | WebSocket|                     | Serviço |
| Emissor |                               | Server  |                      | Mensagem|
+--------+                               +--------+                      +--------+
    |                                        |                               |
    | 1. Envia mensagem                      |                               |
    |--------------------------------------->|                               |
    |                                        | 2. Processa mensagem          |
    |                                        |------------------------------>|
    |                                        |                               |
    |                                        | 3. Armazena mensagem          |
    |                                        |<------------------------------|
    | 4. Confirmação de envio                |                               |
    |<---------------------------------------|                               |
    |                                        |                               |
    |                                        | 5. Entrega mensagem           |
    |                                        |-----+                         |
    |                                        |     |                         |
    |                                        |     v                         |
+--------+                                   |                               |
| Cliente |                                  |                               |
| Receptor|<----------------------------------                               |
+--------+                                                                  |
```

### Upload de Arquivo

```
+--------+                                +--------+                      +--------+
| Cliente |                               | REST API|                      | Sistema |
|        |                               | Server  |                      | Arquivos|
+--------+                               +--------+                      +--------+
    |                                        |                               |
    | 1. Envia arquivo (FormData)            |                               |
    |--------------------------------------->|                               |
    |                                        | 2. Processa arquivo           |
    |                                        |------------------------------>|
    |                                        |                               |
    |                                        | 3. Armazena arquivo           |
    |                                        |<------------------------------|
    | 4. Recebe URL e metadados              |                               |
    |<---------------------------------------|                               |
    |                                        |                               |
    | 5. Envia mensagem com link do arquivo  |                               |
    |------------------+                     |                               |
    |                  |                     |                               |
    |                  v                     |                               |
    |            [Fluxo de Mensagem]         |                               |
    |                                        |                               |
```

## Estrutura de Cache

Para evitar requisições desnecessárias e loops infinitos, o sistema implementa uma estrutura de cache:

```
+-------------------+
|  Cache Frontend   |
+-------------------+
| - Mensagens       |
| - Usuários        |
| - Grupos          |
| - Membros         |
+-------------------+
```

O cache é limpo em situações específicas:
- Quando um novo membro é adicionado a um grupo
- Quando uma nova mensagem é enviada
- Quando o usuário muda de conversa

## Segurança

- **Autenticação**: Baseada em tokens de sessão
- **Criptografia**: Senhas armazenadas com BCrypt
- **Validação**: Validação de entrada em todas as requisições
- **Headers de Segurança**: Headers de autenticação em requisições WebSocket
