# Engenharia de Requisitos - WhatsUT

## 1. Visão Geral do Sistema

O WhatsUT é uma aplicação de chat em tempo real que permite comunicação entre usuários através de mensagens privadas e grupos. O sistema deve suportar autenticação, gerenciamento de usuários, mensagens em tempo real, grupos e notificações.

## 2. Requisitos Funcionais

### 2.1 Autenticação e Autorização
- RF001: O sistema deve permitir login com email/usuário e senha
- RF002: O sistema deve manter sessões de usuário
- RF003: O sistema deve permitir logout
- RF004: O sistema deve validar credenciais de acesso

### 2.2 Gerenciamento de Usuários
- RF005: O sistema deve exibir lista de usuários online
- RF006: O sistema deve atualizar status online/offline em tempo real
- RF007: O sistema deve permitir busca de usuários
- RF008: O sistema deve exibir perfil básico do usuário

### 2.3 Mensagens Privadas
- RF009: O sistema deve permitir envio de mensagens entre usuários
- RF010: O sistema deve exibir histórico de conversas
- RF011: O sistema deve mostrar status de entrega das mensagens (enviado/entregue/lido)
- RF012: O sistema deve permitir envio de arquivos
- RF013: O sistema deve mostrar indicador de digitação
- RF014: O sistema deve entregar mensagens em tempo real

### 2.4 Grupos
- RF015: O sistema deve permitir criação de grupos
- RF016: O sistema deve permitir adicionar/remover participantes
- RF017: O sistema deve gerenciar permissões de administrador
- RF018: O sistema deve permitir configuração de privacidade do grupo
- RF019: O sistema deve exibir lista de participantes
- RF020: O sistema deve permitir transferência de liderança

### 2.5 Notificações
- RF021: O sistema deve enviar notificações de novas mensagens
- RF022: O sistema deve mostrar contador de mensagens não lidas
- RF023: O sistema deve notificar mudanças de status dos usuários

## 3. Requisitos Não Funcionais

### 3.1 Performance
- RNF001: O sistema deve suportar até 10.000 usuários simultâneos
- RNF002: Mensagens devem ser entregues em menos de 100ms
- RNF003: O sistema deve ter disponibilidade de 99.9%

### 3.2 Segurança
- RNF004: Todas as comunicações devem usar HTTPS/WSS
- RNF005: Senhas devem ser criptografadas
- RNF006: Sessões devem expirar após inatividade

### 3.3 Escalabilidade
- RNF007: O sistema deve ser horizontalmente escalável
- RNF008: Banco de dados deve suportar sharding

## 4. Arquitetura do Sistema

### 4.1 Componentes Principais
- **API Gateway**: Roteamento e autenticação
- **Auth Service**: Gerenciamento de autenticação
- **User Service**: Gerenciamento de usuários
- **Chat Service**: Mensagens e conversas
- **Group Service**: Gerenciamento de grupos
- **Notification Service**: Notificações em tempo real
- **File Service**: Upload e gerenciamento de arquivos
- **WebSocket Server**: Comunicação em tempo real

### 4.2 Banco de Dados
- **PostgreSQL**: Dados relacionais (usuários, grupos, mensagens)
- **Redis**: Cache e sessões
- **MongoDB**: Arquivos e metadados

## 5. Endpoints da API

### 5.1 Autenticação

#### POST /api/auth/login
**Descrição**: Realizar login do usuário
**Request Body**:
```json
{
  "email": "string",
  "password": "string",
  "rememberMe": "boolean"
}
```
**Response**:
```json
{
  "success": true,
  "data": {
    "user": {
      "id": "uuid",
      "email": "string",
      "name": "string",
      "avatar": "string",
      "lastSeen": "datetime"
    },
    "token": "string",
    "refreshToken": "string"
  }
}
```

#### POST /api/auth/logout
**Descrição**: Realizar logout do usuário
**Headers**: Authorization: Bearer {token}
**Response**:
```json
{
  "success": true,
  "message": "Logout realizado com sucesso"
}
```

#### POST /api/auth/refresh
**Descrição**: Renovar token de acesso
**Request Body**:
```json
{
  "refreshToken": "string"
}
```

### 5.2 Usuários

#### GET /api/users/me
**Descrição**: Obter dados do usuário logado
**Headers**: Authorization: Bearer {token}
**Response**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "string",
    "name": "string",
    "avatar": "string",
    "status": "online|offline|away",
    "lastSeen": "datetime"
  }
}
```

#### GET /api/users/online
**Descrição**: Listar usuários online
**Headers**: Authorization: Bearer {token}
**Query Parameters**:
- search: string (opcional)
- limit: integer (default: 50)
- offset: integer (default: 0)

**Response**:
```json
{
  "success": true,
  "data": {
    "users": [
      {
        "id": "uuid",
        "name": "string",
        "avatar": "string",
        "status": "online",
        "lastSeen": "datetime"
      }
    ],
    "total": "integer"
  }
}
```

#### PUT /api/users/status
**Descrição**: Atualizar status do usuário
**Headers**: Authorization: Bearer {token}
**Request Body**:
```json
{
  "status": "online|offline|away"
}
```

### 5.3 Conversas

#### GET /api/conversations
**Descrição**: Listar conversas do usuário
**Headers**: Authorization: Bearer {token}
**Response**:
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "type": "private|group",
      "name": "string",
      "avatar": "string",
      "lastMessage": {
        "content": "string",
        "timestamp": "datetime",
        "sender": "string"
      },
      "unreadCount": "integer",
      "participants": ["uuid"]
    }
  ]
}
```

#### GET /api/conversations/{conversationId}/messages
**Descrição**: Obter mensagens de uma conversa
**Headers**: Authorization: Bearer {token}
**Query Parameters**:
- limit: integer (default: 50)
- before: datetime (para paginação)

**Response**:
```json
{
  "success": true,
  "data": {
    "messages": [
      {
        "id": "uuid",
        "content": "string",
        "type": "text|file|image",
        "sender": {
          "id": "uuid",
          "name": "string",
          "avatar": "string"
        },
        "timestamp": "datetime",
        "status": "sent|delivered|read",
        "fileUrl": "string",
        "fileName": "string",
        "fileSize": "integer"
      }
    ],
    "hasMore": "boolean"
  }
}
```

#### POST /api/conversations/{conversationId}/messages
**Descrição**: Enviar mensagem
**Headers**: Authorization: Bearer {token}
**Request Body**:
```json
{
  "content": "string",
  "type": "text|file",
  "fileId": "uuid"
}
```

#### PUT /api/conversations/{conversationId}/read
**Descrição**: Marcar conversa como lida
**Headers**: Authorization: Bearer {token}

### 5.4 Grupos

#### POST /api/groups
**Descrição**: Criar novo grupo
**Headers**: Authorization: Bearer {token}
**Request Body**:
```json
{
  "name": "string",
  "description": "string",
  "avatar": "string",
  "participants": ["uuid"],
  "privacy": "public|private",
  "permissions": {
    "addMembers": "admins|all",
    "editInfo": "admins|all"
  }
}
```

#### GET /api/groups/{groupId}
**Descrição**: Obter informações do grupo
**Headers**: Authorization: Bearer {token}
**Response**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "name": "string",
    "description": "string",
    "avatar": "string",
    "privacy": "public|private",
    "createdBy": "uuid",
    "createdAt": "datetime",
    "permissions": {
      "addMembers": "admins|all",
      "editInfo": "admins|all"
    },
    "participants": [
      {
        "id": "uuid",
        "name": "string",
        "avatar": "string",
        "role": "admin|member",
        "joinedAt": "datetime"
      }
    ]
  }
}
```

#### POST /api/groups/{groupId}/participants
**Descrição**: Adicionar participante ao grupo
**Headers**: Authorization: Bearer {token}
**Request Body**:
```json
{
  "userId": "uuid"
}
```

#### DELETE /api/groups/{groupId}/participants/{userId}
**Descrição**: Remover participante do grupo
**Headers**: Authorization: Bearer {token}

#### PUT /api/groups/{groupId}/participants/{userId}/role
**Descrição**: Alterar role do participante
**Headers**: Authorization: Bearer {token}
**Request Body**:
```json
{
  "role": "admin|member"
}
```

### 5.5 Arquivos

#### POST /api/files/upload
**Descrição**: Upload de arquivo
**Headers**: Authorization: Bearer {token}
**Content-Type**: multipart/form-data
**Request Body**: FormData com arquivo

**Response**:
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "filename": "string",
    "originalName": "string",
    "size": "integer",
    "mimeType": "string",
    "url": "string"
  }
}
```

#### GET /api/files/{fileId}
**Descrição**: Download de arquivo
**Headers**: Authorization: Bearer {token}

## 6. WebSocket Events

### 6.1 Conexão
- **connect**: Estabelecer conexão WebSocket
- **disconnect**: Encerrar conexão
- **authenticate**: Autenticar conexão WebSocket

### 6.2 Mensagens
- **message:send**: Enviar mensagem
- **message:receive**: Receber mensagem
- **message:status**: Atualização de status da mensagem
- **typing:start**: Iniciar indicador de digitação
- **typing:stop**: Parar indicador de digitação

### 6.3 Usuários
- **user:status**: Mudança de status do usuário
- **user:online**: Usuário ficou online
- **user:offline**: Usuário ficou offline

### 6.4 Grupos
- **group:created**: Grupo criado
- **group:updated**: Grupo atualizado
- **group:member:added**: Membro adicionado
- **group:member:removed**: Membro removido

## 7. Schemas de Banco de Dados

### 7.1 Tabela: users
```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(100) NOT NULL,
  avatar TEXT,
  status VARCHAR(20) DEFAULT 'offline',
  last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 7.2 Tabela: conversations
```sql
CREATE TABLE conversations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  type VARCHAR(20) NOT NULL, -- 'private' ou 'group'
  name VARCHAR(100),
  avatar TEXT,
  description TEXT,
  privacy VARCHAR(20) DEFAULT 'public',
  created_by UUID REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 7.3 Tabela: conversation_participants
```sql
CREATE TABLE conversation_participants (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  role VARCHAR(20) DEFAULT 'member', -- 'admin' ou 'member'
  joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_read_at TIMESTAMP,
  UNIQUE(conversation_id, user_id)
);
```

### 7.4 Tabela: messages
```sql
CREATE TABLE messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
  sender_id UUID REFERENCES users(id),
  content TEXT,
  type VARCHAR(20) DEFAULT 'text', -- 'text', 'file', 'image'
  file_id UUID,
  file_name VARCHAR(255),
  file_size INTEGER,
  file_url TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 7.5 Tabela: message_status
```sql
CREATE TABLE message_status (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
  user_id UUID REFERENCES users(id),
  status VARCHAR(20) NOT NULL, -- 'sent', 'delivered', 'read'
  timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(message_id, user_id)
);
```

### 7.6 Tabela: files
```sql
CREATE TABLE files (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  filename VARCHAR(255) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  size INTEGER NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  path TEXT NOT NULL,
  uploaded_by UUID REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### 7.7 Tabela: user_sessions
```sql
CREATE TABLE user_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) NOT NULL,
  refresh_token_hash VARCHAR(255),
  expires_at TIMESTAMP NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  last_used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## 8. Índices Recomendados

```sql
-- Índices para performance
CREATE INDEX idx_messages_conversation_created ON messages(conversation_id, created_at DESC);
CREATE INDEX idx_conversation_participants_user ON conversation_participants(user_id);
CREATE INDEX idx_conversation_participants_conversation ON conversation_participants(conversation_id);
CREATE INDEX idx_message_status_message ON message_status(message_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_user_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_token ON user_sessions(token_hash);
```

## 9. Considerações de Implementação

### 9.1 Segurança
- Implementar rate limiting nos endpoints
- Validar todas as entradas
- Usar JWT para autenticação
- Implementar CORS adequadamente
- Criptografar dados sensíveis

### 9.2 Performance
- Implementar cache Redis para sessões e dados frequentes
- Usar connection pooling para banco de dados
- Implementar paginação em todas as listagens
- Otimizar queries com índices apropriados

### 9.3 Escalabilidade
- Usar load balancer para distribuir requisições
- Implementar horizontal scaling para WebSocket servers
- Considerar sharding do banco de dados por usuário
- Usar CDN para arquivos estáticos

### 9.4 Monitoramento
- Implementar logs estruturados
- Monitorar métricas de performance
- Configurar alertas para erros críticos
- Implementar health checks

## 10. Tecnologias Recomendadas

### 10.1 Backend
- **Framework**: Node.js com Express ou Fastify
- **WebSocket**: Socket.io ou ws
- **Banco de Dados**: PostgreSQL + Redis
- **ORM**: Prisma ou TypeORM
- **Autenticação**: JWT + bcrypt

### 10.2 Infraestrutura
- **Container**: Docker
- **Orquestração**: Kubernetes
- **Load Balancer**: Nginx
- **CDN**: CloudFlare
- **Storage**: AWS S3 ou similar

