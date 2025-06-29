# Workflow para Desenvolvimento do Backend RMI com Sockets em Java para o WhatsUT

Baseado na análise do frontend React do WhatsUT, este documento serve como guia completo para a implementação do servidor backend em Java utilizando RMI (Remote Method Invocation) com sockets.

## 1. Arquitetura Geral do Sistema

### 1.1 Componentes Principais
- **Servidor RMI**: Núcleo do backend que gerencia conexões e processa requisições
- **Interface Remota**: Define os métodos que podem ser chamados remotamente pelo cliente
- **Implementação da Interface**: Implementa a lógica de negócio
- **Registro RMI**: Permite que os clientes encontrem os objetos remotos
- **Gerenciador de Sessões**: Controla autenticação e sessões de usuários
- **Gerenciador de Mensagens**: Processa envio/recebimento de mensagens
- **Gerenciador de Grupos**: Administra criação e gerenciamento de grupos
- **Sistema de Notificações**: Envia notificações em tempo real

### 1.2 Fluxo de Comunicação
1. Cliente se conecta ao servidor via WebSocket (frontend)
2. Servidor traduz requisições WebSocket para chamadas RMI
3. Objetos RMI processam as requisições
4. Respostas são enviadas de volta via WebSocket

## 2. Estrutura de Pacotes e Classes

```
com.whatsut
├── server
│   ├── WhatsUTServer.java (Classe principal do servidor)
│   ├── RMIRegistry.java (Configuração do registro RMI)
│   └── WebSocketServer.java (Ponte entre WebSocket e RMI)
├── interfaces
│   ├── AuthService.java
│   ├── MessageService.java
│   ├── GroupService.java
│   ├── UserService.java
│   └── NotificationService.java
├── implementation
│   ├── AuthServiceImpl.java
│   ├── MessageServiceImpl.java
│   ├── GroupServiceImpl.java
│   ├── UserServiceImpl.java
│   └── NotificationServiceImpl.java
├── models
│   ├── User.java
│   ├── Message.java
│   ├── Group.java
│   ├── Session.java
│   ├── Conversation.java
│   └── Notification.java
├── dao
│   ├── UserDAO.java
│   ├── MessageDAO.java
│   ├── GroupDAO.java
│   └── ConversationDAO.java
├── utils
│   ├── DatabaseConnection.java
│   ├── SecurityUtils.java
│   └── JsonConverter.java
└── websocket
    ├── WebSocketHandler.java
    ├── RequestProcessor.java
    └── ResponseSender.java
```

## 3. Interfaces Remotas

### 3.1 AuthService.java
```java
public interface AuthService extends Remote {
    SessionDTO login(String email, String password) throws RemoteException;
    boolean logout(String sessionId) throws RemoteException;
    UserDTO register(UserRegistrationDTO userData) throws RemoteException;
    boolean validateSession(String sessionId) throws RemoteException;
}
```

### 3.2 MessageService.java
```java
public interface MessageService extends Remote {
    List<MessageDTO> getPrivateMessages(String sessionId, String otherUserId, int limit) throws RemoteException;
    List<MessageDTO> getGroupMessages(String sessionId, String groupId, int limit) throws RemoteException;
    MessageDTO sendPrivateMessage(String sessionId, String receiverId, String content) throws RemoteException;
    MessageDTO sendGroupMessage(String sessionId, String groupId, String content) throws RemoteException;
    List<ConversationDTO> getPrivateConversations(String sessionId) throws RemoteException;
}
```

### 3.3 GroupService.java
```java
public interface GroupService extends Remote {
    List<GroupDTO> getGroups(String sessionId) throws RemoteException;
    List<GroupMemberDTO> getGroupMembers(String sessionId, String groupId) throws RemoteException;
    GroupDTO createGroup(String sessionId, GroupCreationDTO groupData) throws RemoteException;
    boolean addUserToGroup(String sessionId, String groupId, String userId) throws RemoteException;
    boolean removeUserFromGroup(String sessionId, String groupId, String userId) throws RemoteException;
    boolean setGroupAdmin(String sessionId, String groupId, String userId) throws RemoteException;
    boolean leaveGroup(String sessionId, String groupId, boolean deleteIfAdmin) throws RemoteException;
    boolean deleteGroup(String sessionId, String groupId) throws RemoteException;
}
```
