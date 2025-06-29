# Documentação das APIs do WhatsUT

Esta documentação descreve as APIs disponíveis no sistema WhatsUT, tanto para o backend (WebSocket e REST) quanto para o frontend (React).

## Backend APIs

### WebSocket API

O servidor WebSocket opera na porta 8080 e aceita conexões com os seguintes headers de autenticação:
- `X-Session-Id`: ID da sessão do usuário
- `X-Auth-Token`: Token de autenticação

#### Mensagens WebSocket

| Tipo | Descrição | Payload | Resposta |
|------|-----------|---------|----------|
| `login` | Autenticação do usuário | `{ username: string, password: string }` | `{ sessionId: string, userId: string, displayName: string }` |
| `register` | Registro de novo usuário | `{ username: string, password: string, displayName: string }` | `{ success: boolean, message: string }` |
| `getUsers` | Obter lista de usuários | `{ sessionId: string }` | `{ users: User[] }` |
| `getGroups` | Obter grupos do usuário | `{ sessionId: string }` | `{ groups: Group[] }` |
| `getGroupMembers` | Obter membros de um grupo | `{ sessionId: string, groupId: string }` | `{ members: User[] }` |
| `createGroup` | Criar novo grupo | `{ sessionId: string, groupName: string, members: string[] }` | `{ groupId: string, groupName: string }` |
| `addUserToGroup` | Adicionar usuário ao grupo | `{ sessionId: string, groupId: string, userId: string }` | `{ success: boolean }` |
| `removeUserFromGroup` | Remover usuário do grupo | `{ sessionId: string, groupId: string, userId: string }` | `{ success: boolean }` |
| `sendMessage` | Enviar mensagem privada | `{ sessionId: string, receiverId: string, content: string }` | `{ messageId: string, timestamp: number }` |
| `sendGroupMessage` | Enviar mensagem para grupo | `{ sessionId: string, groupId: string, content: string }` | `{ messageId: string, timestamp: number }` |
| `getMessages` | Obter mensagens privadas | `{ sessionId: string, userId: string, limit: number }` | `{ messages: Message[] }` |
| `getGroupMessages` | Obter mensagens de grupo | `{ sessionId: string, groupId: string, limit: number }` | `{ messages: Message[] }` |

### REST API

#### Endpoints para Upload de Arquivos

| Método | Endpoint | Descrição | Parâmetros | Resposta |
|--------|----------|-----------|------------|----------|
| `POST` | `/api/messages/private/file` | Upload de arquivo para chat privado | `file: File, sessionId: string, receiverId: string, fileType?: string` | `{ messageId: string, fileUrl: string, fileType: string, timestamp: number }` |
| `POST` | `/api/messages/group/file` | Upload de arquivo para grupo | `file: File, sessionId: string, groupId: string, fileType?: string` | `{ messageId: string, fileUrl: string, fileType: string, timestamp: number }` |
| `GET` | `/api/files/:fileId` | Download de arquivo | `fileId: string` | Arquivo binário |

## Frontend API (React Hooks e Stores)

### WebSocketContext

O `WebSocketContext` fornece acesso às funcionalidades de WebSocket em toda a aplicação React.

```jsx
const { 
  connect,
  disconnect,
  isConnected,
  login,
  register,
  sendMessage,
  sendGroupMessage,
  sendFile,
  sendPrivateFile,
  sendGroupFile,
  getUsers,
  getGroups,
  getGroupMembers,
  createGroup,
  addUserToGroup,
  removeUserFromGroup,
  getMessages,
  getGroupMessages
} = useWebSocket();
```

### WebSocketStore (Zustand)

O `WebSocketStore` gerencia o estado global relacionado às comunicações WebSocket.

```jsx
const { 
  isConnected,
  isConnecting,
  sessionId,
  currentUser,
  users,
  groups,
  conversations,
  groupMessages,
  privateMessages,
  groupMembers,
  connect,
  disconnect,
  login,
  register,
  // ... outros métodos
} = useWebSocketStore();
```

## Modelos de Dados

### User

```typescript
interface User {
  userId: string;
  username: string;
  displayName: string;
  status?: 'online' | 'offline';
  lastSeen?: number;
}
```

### Message

```typescript
interface Message {
  messageId: string;
  senderId: string;
  senderName: string;
  content: string;
  timestamp: number;
  status: 'sending' | 'sent' | 'delivered' | 'read' | 'failed';
  isOwn: boolean;
  isFile?: boolean;
  fileName?: string;
  fileUrl?: string;
  fileType?: string;
}
```

### Group

```typescript
interface Group {
  groupId: string;
  groupName: string;
  creatorId: string;
  members: string[];
  createdAt: number;
  lastMessage?: string;
  lastMessageTime?: number;
}
```

### Conversation

```typescript
interface Conversation {
  id: string;
  type: 'private' | 'group';
  name: string;
  lastMessage?: string;
  lastMessageTime?: number;
  unreadCount?: number;
}
```

## Fluxos de Comunicação

### Autenticação

1. Cliente envia `login` com credenciais
2. Servidor valida e retorna `sessionId`
3. Cliente armazena `sessionId` para futuras requisições

### Envio de Mensagem Privada

1. Cliente envia `sendMessage` com `receiverId` e `content`
2. Servidor processa e entrega a mensagem
3. Receptor recebe evento `newMessage`

### Envio de Arquivo

1. Cliente faz upload via REST API
2. Servidor armazena o arquivo e gera URL
3. Cliente recebe URL e adiciona à mensagem
4. Mensagem é entregue como mensagem normal com metadados de arquivo

### Comunicação em Grupo

1. Cliente envia `sendGroupMessage` com `groupId` e `content`
2. Servidor distribui para todos os membros do grupo
3. Membros recebem evento `newGroupMessage`
