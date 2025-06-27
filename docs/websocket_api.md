# Especificação da API WebSocket para WhatsUT

Este documento define a estrutura de mensagens WebSocket entre o front-end e o adaptador Java que se comunica com os serviços RMI.

## Formato Geral de Mensagens

### Requisições do Cliente (Front → Back)
```json
{
  "requestId": "uuid-v4-aqui",
  "action": "nome_da_acao",
  "data": {
    // dados específicos da ação
  }
}
```

### Respostas do Servidor (Back → Front)
```json
{
  "requestId": "uuid-v4-aqui", // mesmo ID da requisição
  "success": true,
  "data": {
    // dados da resposta
  },
  "error": null // ou mensagem de erro se success=false
}
```

### Eventos do Servidor (Back → Front)
```json
{
  "type": "nome_do_evento",
  "data": {
    // dados específicos do evento
  }
}
```

## Autenticação e Usuários

### 1. Registro de Usuário
**Requisição:**
```json
{
  "requestId": "123",
  "action": "register",
  "data": {
    "username": "pedro",
    "email": "pedro@example.com",
    "password": "senha123"
  }
}
```

**Resposta:**
```json
{
  "requestId": "123",
  "success": true,
  "data": {
    "userId": "user-uuid",
    "username": "pedro"
  },
  "error": null
}
```

### 2. Login
**Requisição:**
```json
{
  "requestId": "124",
  "action": "login",
  "data": {
    "email": "pedro@example.com",
    "password": "senha123"
  }
}
```

**Resposta:**
```json
{
  "requestId": "124",
  "success": true,
  "data": {
    "sessionId": "session-token-aqui",
    "userId": "user-uuid",
    "username": "pedro"
  },
  "error": null
}
```

**Evento para todos os usuários online:**
```json
{
  "type": "userStatusChanged",
  "data": {
    "userId": "user-uuid",
    "username": "pedro",
    "status": "online"
  }
}
```

### 3. Obter Lista de Usuários
**Requisição:**
```json
{
  "requestId": "125",
  "action": "getUsers",
  "data": {
    "sessionId": "session-token-aqui"
  }
}
```

**Resposta:**
```json
{
  "requestId": "125",
  "success": true,
  "data": {
    "users": [
      {
        "userId": "user1-uuid",
        "username": "pedro",
        "status": "online",
        "isAdmin": true
      },
      {
        "userId": "user2-uuid",
        "username": "maria",
        "status": "offline",
        "isAdmin": false
      }
    ]
  },
  "error": null
}
```

### 4. Atualizar Status
**Requisição:**
```json
{
  "requestId": "126",
  "action": "updateStatus",
  "data": {
    "sessionId": "session-token-aqui",
    "status": "busy" // online, offline, busy
  }
}
```

**Resposta:**
```json
{
  "requestId": "126",
  "success": true,
  "data": {},
  "error": null
}
```

**Evento para todos os usuários online:**
```json
{
  "type": "userStatusChanged",
  "data": {
    "userId": "user-uuid",
    "username": "pedro",
    "status": "busy"
  }
}
```

### 5. Logout
**Requisição:**
```json
{
  "requestId": "127",
  "action": "logout",
  "data": {
    "sessionId": "session-token-aqui"
  }
}
```

**Resposta:**
```json
{
  "requestId": "127",
  "success": true,
  "data": {},
  "error": null
}
```

**Evento para todos os usuários online:**
```json
{
  "type": "userStatusChanged",
  "data": {
    "userId": "user-uuid",
    "username": "pedro",
    "status": "offline"
  }
}
```

### 6. Banir Usuário (Admin)
**Requisição:**
```json
{
  "requestId": "128",
  "action": "banUser",
  "data": {
    "sessionId": "session-token-aqui",
    "targetUserId": "user-to-ban-uuid"
  }
}
```

**Resposta:**
```json
{
  "requestId": "128",
  "success": true,
  "data": {},
  "error": null
}
```

**Evento para o usuário banido:**
```json
{
  "type": "systemNotification",
  "data": {
    "message": "Você foi banido do sistema.",
    "action": "BANNED"
  }
}
```

## Mensagens Privadas

### 7. Obter Conversas Privadas
**Requisição:**
```json
{
  "requestId": "129",
  "action": "getPrivateConversations",
  "data": {
    "sessionId": "session-token-aqui"
  }
}
```

**Resposta:**
```json
{
  "requestId": "129",
  "success": true,
  "data": {
    "conversations": [
      {
        "userId": "user2-uuid",
        "username": "maria",
        "lastMessage": "Olá, como vai?",
        "timestamp": 1624553600000,
        "unreadCount": 2
      },
      {
        "userId": "user3-uuid",
        "username": "joao",
        "lastMessage": "Vamos marcar aquela reunião",
        "timestamp": 1624550000000,
        "unreadCount": 0
      }
    ]
  },
  "error": null
}
```

### 8. Obter Histórico de Mensagens Privadas
**Requisição:**
```json
{
  "requestId": "130",
  "action": "getPrivateMessages",
  "data": {
    "sessionId": "session-token-aqui",
    "otherUserId": "user2-uuid",
    "limit": 50,
    "before": 1624553600000 // timestamp opcional para paginação
  }
}
```

**Resposta:**
```json
{
  "requestId": "130",
  "success": true,
  "data": {
    "messages": [
      {
        "messageId": "msg1-uuid",
        "senderId": "user2-uuid",
        "senderName": "maria",
        "content": "Olá, como vai?",
        "timestamp": 1624553600000,
        "isFile": false
      },
      {
        "messageId": "msg2-uuid",
        "senderId": "user1-uuid",
        "senderName": "pedro",
        "content": "Tudo bem, e você?",
        "timestamp": 1624553700000,
        "isFile": false
      }
    ]
  },
  "error": null
}
```

### 9. Enviar Mensagem Privada
**Requisição:**
```json
{
  "requestId": "131",
  "action": "sendPrivateMessage",
  "data": {
    "sessionId": "session-token-aqui",
    "receiverId": "user2-uuid",
    "content": "Olá, tudo bem?"
  }
}
```

**Resposta:**
```json
{
  "requestId": "131",
  "success": true,
  "data": {
    "messageId": "msg3-uuid",
    "timestamp": 1624554000000
  },
  "error": null
}
```

**Evento para o destinatário:**
```json
{
  "type": "newPrivateMessage",
  "data": {
    "messageId": "msg3-uuid",
    "senderId": "user1-uuid",
    "senderName": "pedro",
    "content": "Olá, tudo bem?",
    "timestamp": 1624554000000,
    "isFile": false
  }
}
```

## Grupos

### 10. Obter Lista de Grupos
**Requisição:**
```json
{
  "requestId": "132",
  "action": "getGroups",
  "data": {
    "sessionId": "session-token-aqui"
  }
}
```

**Resposta:**
```json
{
  "requestId": "132",
  "success": true,
  "data": {
    "groups": [
      {
        "groupId": "group1-uuid",
        "name": "Projeto WhatsUT",
        "adminId": "user1-uuid",
        "adminName": "pedro",
        "lastMessage": "Reunião amanhã às 10h",
        "timestamp": 1624560000000,
        "memberCount": 5,
        "isMember": true
      },
      {
        "groupId": "group2-uuid",
        "name": "Grupo de Estudos",
        "adminId": "user3-uuid",
        "adminName": "joao",
        "lastMessage": "Alguém tem o material?",
        "timestamp": 1624550000000,
        "memberCount": 8,
        "isMember": false
      }
    ]
  },
  "error": null
}
```

### 11. Criar Grupo
**Requisição:**
```json
{
  "requestId": "133",
  "action": "createGroup",
  "data": {
    "sessionId": "session-token-aqui",
    "name": "Novo Grupo",
    "description": "Descrição do grupo",
    "deleteOnAdminExit": true,
    "initialMembers": ["user2-uuid", "user3-uuid"]
  }
}
```

**Resposta:**
```json
{
  "requestId": "133",
  "success": true,
  "data": {
    "groupId": "group3-uuid",
    "name": "Novo Grupo"
  },
  "error": null
}
```

**Evento para membros convidados:**
```json
{
  "type": "groupInvitation",
  "data": {
    "groupId": "group3-uuid",
    "groupName": "Novo Grupo",
    "adminId": "user1-uuid",
    "adminName": "pedro"
  }
}
```

### 12. Solicitar Entrada em Grupo
**Requisição:**
```json
{
  "requestId": "134",
  "action": "requestJoinGroup",
  "data": {
    "sessionId": "session-token-aqui",
    "groupId": "group2-uuid"
  }
}
```

**Resposta:**
```json
{
  "requestId": "134",
  "success": true,
  "data": {
    "requestId": "request1-uuid",
    "status": "pending"
  },
  "error": null
}
```

**Evento para o administrador do grupo:**
```json
{
  "type": "groupJoinRequest",
  "data": {
    "requestId": "request1-uuid",
    "groupId": "group2-uuid",
    "groupName": "Grupo de Estudos",
    "userId": "user1-uuid",
    "username": "pedro"
  }
}
```

### 13. Aprovar/Rejeitar Solicitação de Entrada
**Requisição:**
```json
{
  "requestId": "135",
  "action": "respondJoinRequest",
  "data": {
    "sessionId": "session-token-aqui",
    "requestId": "request1-uuid",
    "approved": true
  }
}
```

**Resposta:**
```json
{
  "requestId": "135",
  "success": true,
  "data": {},
  "error": null
}
```

**Evento para o solicitante:**
```json
{
  "type": "groupJoinResponse",
  "data": {
    "groupId": "group2-uuid",
    "groupName": "Grupo de Estudos",
    "approved": true
  }
}
```

**Evento para todos os membros do grupo (se aprovado):**
```json
{
  "type": "newGroupMember",
  "data": {
    "groupId": "group2-uuid",
    "userId": "user1-uuid",
    "username": "pedro"
  }
}
```

### 14. Obter Membros do Grupo
**Requisição:**
```json
{
  "requestId": "136",
  "action": "getGroupMembers",
  "data": {
    "sessionId": "session-token-aqui",
    "groupId": "group1-uuid"
  }
}
```

**Resposta:**
```json
{
  "requestId": "136",
  "success": true,
  "data": {
    "members": [
      {
        "userId": "user1-uuid",
        "username": "pedro",
        "isAdmin": true,
        "status": "online"
      },
      {
        "userId": "user2-uuid",
        "username": "maria",
        "isAdmin": false,
        "status": "offline"
      }
    ]
  },
  "error": null
}
```

### 15. Banir Membro do Grupo
**Requisição:**
```json
{
  "requestId": "137",
  "action": "banFromGroup",
  "data": {
    "sessionId": "session-token-aqui",
    "groupId": "group1-uuid",
    "userId": "user2-uuid"
  }
}
```

**Resposta:**
```json
{
  "requestId": "137",
  "success": true,
  "data": {},
  "error": null
}
```

**Evento para o usuário banido:**
```json
{
  "type": "groupBanned",
  "data": {
    "groupId": "group1-uuid",
    "groupName": "Projeto WhatsUT"
  }
}
```

**Evento para todos os membros do grupo:**
```json
{
  "type": "memberRemoved",
  "data": {
    "groupId": "group1-uuid",
    "userId": "user2-uuid",
    "username": "maria",
    "reason": "banned"
  }
}
```

### 16. Sair do Grupo
**Requisição:**
```json
{
  "requestId": "138",
  "action": "leaveGroup",
  "data": {
    "sessionId": "session-token-aqui",
    "groupId": "group1-uuid"
  }
}
```

**Resposta:**
```json
{
  "requestId": "138",
  "success": true,
  "data": {},
  "error": null
}
```

**Evento para todos os membros do grupo:**
```json
{
  "type": "memberRemoved",
  "data": {
    "groupId": "group1-uuid",
    "userId": "user1-uuid",
    "username": "pedro",
    "reason": "left"
  }
}
```

**Evento adicional se o administrador saiu e o grupo foi eliminado:**
```json
{
  "type": "groupDeleted",
  "data": {
    "groupId": "group1-uuid",
    "reason": "adminLeft"
  }
}
```

**Evento adicional se o administrador saiu e um novo administrador foi escolhido:**
```json
{
  "type": "newGroupAdmin",
  "data": {
    "groupId": "group1-uuid",
    "userId": "user3-uuid",
    "username": "joao"
  }
}
```

### 17. Obter Mensagens do Grupo
**Requisição:**
```json
{
  "requestId": "139",
  "action": "getGroupMessages",
  "data": {
    "sessionId": "session-token-aqui",
    "groupId": "group1-uuid",
    "limit": 50,
    "before": 1624560000000 // timestamp opcional para paginação
  }
}
```

**Resposta:**
```json
{
  "requestId": "139",
  "success": true,
  "data": {
    "messages": [
      {
        "messageId": "gmsg1-uuid",
        "senderId": "user3-uuid",
        "senderName": "joao",
        "content": "Reunião amanhã às 10h",
        "timestamp": 1624560000000,
        "isFile": false
      },
      {
        "messageId": "gmsg2-uuid",
        "senderId": "user1-uuid",
        "senderName": "pedro",
        "content": "Estarei presente",
        "timestamp": 1624561000000,
        "isFile": false
      }
    ]
  },
  "error": null
}
```

### 18. Enviar Mensagem para Grupo
**Requisição:**
```json
{
  "requestId": "140",
  "action": "sendGroupMessage",
  "data": {
    "sessionId": "session-token-aqui",
    "groupId": "group1-uuid",
    "content": "Olá pessoal!"
  }
}
```

**Resposta:**
```json
{
  "requestId": "140",
  "success": true,
  "data": {
    "messageId": "gmsg3-uuid",
    "timestamp": 1624562000000
  },
  "error": null
}
```

**Evento para todos os membros do grupo:**
```json
{
  "type": "newGroupMessage",
  "data": {
    "messageId": "gmsg3-uuid",
    "groupId": "group1-uuid",
    "senderId": "user1-uuid",
    "senderName": "pedro",
    "content": "Olá pessoal!",
    "timestamp": 1624562000000,
    "isFile": false
  }
}
```

## Transferência de Arquivos

### 19. Iniciar Transferência de Arquivo
**Requisição:**
```json
{
  "requestId": "141",
  "action": "initiateFileTransfer",
  "data": {
    "sessionId": "session-token-aqui",
    "receiverId": "user2-uuid",
    "fileName": "documento.pdf",
    "fileSize": 1024000,
    "fileType": "application/pdf"
  }
}
```

**Resposta:**
```json
{
  "requestId": "141",
  "success": true,
  "data": {
    "transferId": "transfer1-uuid",
    "chunkSize": 65536,
    "totalChunks": 16
  },
  "error": null
}
```

**Evento para o destinatário:**
```json
{
  "type": "fileTransferInitiated",
  "data": {
    "transferId": "transfer1-uuid",
    "senderId": "user1-uuid",
    "senderName": "pedro",
    "fileName": "documento.pdf",
    "fileSize": 1024000,
    "fileType": "application/pdf"
  }
}
```

### 20. Transferir Chunk de Arquivo
**Requisição:**
```json
{
  "requestId": "142",
  "action": "transferFileChunk",
  "data": {
    "sessionId": "session-token-aqui",
    "transferId": "transfer1-uuid",
    "chunkIndex": 0,
    "chunk": "base64-encoded-data"
  }
}
```

**Resposta:**
```json
{
  "requestId": "142",
  "success": true,
  "data": {
    "chunkIndex": 0,
    "received": true
  },
  "error": null
}
```

**Evento para o destinatário (a cada 25% do progresso):**
```json
{
  "type": "fileTransferProgress",
  "data": {
    "transferId": "transfer1-uuid",
    "progress": 25, // porcentagem
    "chunksReceived": 4,
    "totalChunks": 16
  }
}
```

### 21. Completar Transferência de Arquivo
**Requisição:**
```json
{
  "requestId": "143",
  "action": "completeFileTransfer",
  "data": {
    "sessionId": "session-token-aqui",
    "transferId": "transfer1-uuid"
  }
}
```

**Resposta:**
```json
{
  "requestId": "143",
  "success": true,
  "data": {
    "fileId": "file1-uuid",
    "messageId": "msg4-uuid"
  },
  "error": null
}
```

**Evento para o destinatário:**
```json
{
  "type": "newPrivateMessage",
  "data": {
    "messageId": "msg4-uuid",
    "senderId": "user1-uuid",
    "senderName": "pedro",
    "content": "documento.pdf",
    "timestamp": 1624563000000,
    "isFile": true,
    "fileId": "file1-uuid",
    "fileSize": 1024000,
    "fileType": "application/pdf"
  }
}
```

### 22. Download de Arquivo
**Requisição:**
```json
{
  "requestId": "144",
  "action": "downloadFileChunk",
  "data": {
    "sessionId": "session-token-aqui",
    "fileId": "file1-uuid",
    "chunkIndex": 0
  }
}
```

**Resposta:**
```json
{
  "requestId": "144",
  "success": true,
  "data": {
    "chunkIndex": 0,
    "chunk": "base64-encoded-data",
    "totalChunks": 16
  },
  "error": null
}
```
