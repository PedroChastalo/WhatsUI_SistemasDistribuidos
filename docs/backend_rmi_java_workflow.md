# Workflow para Desenvolvimento do Backend RMI com Sockets em Java para o WhatsUT

Contexto:

Somos alunos do curso de ciencia da computacão e o professor da materia de sistemas distribuidos pedir a seguinte tarefa

## Tarefa a ser feita:

Você foi contratado para o desenvolvimento de um sistema para comunicação interpessoal: o WhatsUT. Tal sistema precisa atender os seguintes requisitos:

1. Autenticação criptografada: o usuário precisa estar cadastrado para utilizar, e seu acesso deve ser feito via senha. É importante usar um processo de criptografia de dados.

2. Lista de usuários: ao realizar o login, uma lista de usuários deve ser apresentada, caracterizando o usuário que estiver atualmente logado e disponível para chat.

3. Lista de grupos: uma lista de grupos para chat deve ser apresentada. O cliente poderá pedir para entrar no grupo de conversa, sendo aprovado ou não pelo criador do grupo de conversação.

4. Dois modos de chat devem ser providos: chat privado, permitindo a conversação entre duas pessoas apenas; e chat em grupo, permitindo com que várias pessoas possam se juntar a uma conversa. No caso da conversa em grupo, o usuário que criou pode dar permissão a outros usuários para entrada.

5. **Envio de arquivos**: em chats privados, um usuário poderá enviar arquivos ao outro usuário.

6. **Exclusão**: um usuário poderá requisitar ao servidor que um usuário seja banido da aplicação. Banir um usuário do grupo é tarefa do administrador do grupo. Caso o administrador do grupo saia, o aplicativo deve decidir quem será o novo administrador, ou se o grupo seja eliminado. Tal opção pode ser ajustada no momento da criação do chat em grupo.

É importante que se tenha telas intuitivas, modernas e "caprichadas" tanto para o cliente quanto para o servidor. Ainda, deve-se apresentar os diagramas UML (atividades, colaboração, sequência...). Pontos serão dados para chamadas de CallBack, interfaces de servidor para configuração.

---

## Implementação do Backend

Baseado na análise do frontend React do WhatsUT e nos requisitos acima, este documento serve como guia completo para a implementação do servidor backend em Java utilizando RMI (Remote Method Invocation) com sockets.

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

### 3.4 UserService.java

```java
public interface UserService extends Remote {
    List<UserDTO> getUsers(String sessionId) throws RemoteException;
    UserDTO getUserById(String sessionId, String userId) throws RemoteException;
    boolean updateStatus(String sessionId, String status) throws RemoteException;
    boolean updateProfile(String sessionId, UserProfileDTO profileData) throws RemoteException;
}
```

### 3.5 NotificationService.java

```java
public interface NotificationService extends Remote {
    void registerClient(String sessionId, NotificationListener listener) throws RemoteException;
    void unregisterClient(String sessionId) throws RemoteException;
}
```

## 4. Modelos de Dados (DTOs)

### 4.1 User.java

```java
public class User implements Serializable {
    private String userId;
    private String username;
    private String displayName;
    private String email;
    private String passwordHash;
    private String status; // "online", "offline", "away"
    private Date lastSeen;
    private String profilePicture;
    // Getters, setters, construtores
}
```

### 4.2 Message.java

```java
public class Message implements Serializable {
    private String messageId;
    private String senderId;
    private String senderName;
    private String content;
    private long timestamp;
    private boolean isFile;
    private String recipientId; // userId ou groupId
    private String type; // "user" ou "group"
    private String status; // "sending", "sent", "delivered", "read", "failed"
    // Getters, setters, construtores
}
```

### 4.3 Group.java

```java
public class Group implements Serializable {
    private String groupId;
    private String name;
    private String description;
    private String creatorId;
    private long createdAt;
    private String lastMessage;
    private long timestamp;
    private List<GroupMember> members;
    // Getters, setters, construtores
}
```

### 4.4 GroupMember.java

```java
public class GroupMember implements Serializable {
    private String userId;
    private String groupId;
    private String role; // "admin" ou "member"
    private long joinedAt;
    // Getters, setters, construtores
}
```

## 5. Implementação do WebSocket

### 5.1 WebSocketHandler.java

Responsável por:

- Aceitar conexões WebSocket do frontend
- Traduzir mensagens JSON para chamadas RMI
- Enviar respostas de volta para o cliente
- Gerenciar conexões persistentes

```java
public class WebSocketHandler {
    private Map<String, Session> sessions = new ConcurrentHashMap<>();

    public void onOpen(Session session) {
        // Inicializar sessão WebSocket
    }

    public void onMessage(String message, Session session) {
        // Processar mensagem recebida
        JsonObject request = JsonParser.parseString(message).getAsJsonObject();
        String type = request.get("type").getAsString();
        String sessionId = request.has("sessionId") ? request.get("sessionId").getAsString() : null;

        // Encaminhar para o processador de requisições
        RequestProcessor processor = new RequestProcessor();
        JsonObject response = processor.processRequest(type, request, sessionId);

        // Enviar resposta
        session.getBasicRemote().sendText(response.toString());
    }

    public void onClose(Session session) {
        // Limpar recursos quando a conexão for fechada
    }

    public void onError(Session session, Throwable throwable) {
        // Tratar erros de conexão
    }
}
```

## 6. Formato das Requisições e Respostas

### 6.1 Formato de Requisição

```json
{
  "type": "login",
  "email": "usuario@exemplo.com",
  "password": "senha123"
}
```

### 6.2 Formato de Resposta

```json
{
  "success": true,
  "data": {
    "sessionId": "abc123",
    "user": {
      "userId": "u1",
      "username": "usuario",
      "displayName": "Nome do Usuário"
    }
  }
}
```

## 7. Mapeamento de Requisições WebSocket para RMI

| Tipo de Requisição | Serviço RMI | Método |
|-------------------|------------|--------|
| login | AuthService | login(email, password) |
| logout | AuthService | logout(sessionId) |
| register | AuthService | register(userData) |
| getUsers | UserService | getUsers(sessionId) |
| getPrivateConversations | MessageService | getPrivateConversations(sessionId) |
| getPrivateMessages | MessageService | getPrivateMessages(sessionId, otherUserId, limit) |
| sendPrivateMessage | MessageService | sendPrivateMessage(sessionId, receiverId, content) |
| getGroups | GroupService | getGroups(sessionId) |
| getGroupMembers | GroupService | getGroupMembers(sessionId, groupId) |
| getGroupMessages | MessageService | getGroupMessages(sessionId, groupId, limit) |
| sendGroupMessage | MessageService | sendGroupMessage(sessionId, groupId, content) |
| createGroup | GroupService | createGroup(sessionId, groupData) |
| addUserToGroup | GroupService | addUserToGroup(sessionId, groupId, userId) |
| removeUserFromGroup | GroupService | removeUserFromGroup(sessionId, groupId, userId) |
| setGroupAdmin | GroupService | setGroupAdmin(sessionId, groupId, userId) |
| leaveGroup | GroupService | leaveGroup(sessionId, groupId, deleteIfAdmin) |
| deleteGroup | GroupService | deleteGroup(sessionId, groupId) |
| updateStatus | UserService | updateStatus(sessionId, status) |

## 8. Sistema de Notificações em Tempo Real

### 8.1 NotificationListener.java
```java
public interface NotificationListener extends Remote {
    void notify(NotificationDTO notification) throws RemoteException;
}
```

### 8.2 Tipos de Notificações
- `userStatusChanged`: Quando um usuário muda de status
- `newPrivateMessage`: Quando uma nova mensagem privada é recebida
- `newGroupMessage`: Quando uma nova mensagem de grupo é recebida
- `groupMemberAdded`: Quando um usuário é adicionado a um grupo
- `groupMemberRemoved`: Quando um usuário é removido de um grupo
- `groupAdminChanged`: Quando um administrador de grupo é alterado

## 9. Persistência de Dados

### 9.1 Estrutura de Arquivos
- Arquivo `users.json`: Armazena informações dos usuários
- Pasta `messages/`: Contém arquivos JSON separados por conversa/grupo
  - `messages_private_{userId1}_{userId2}.json`: Mensagens privadas entre dois usuários
  - `messages_group_{groupId}.json`: Mensagens de um grupo específico
- Arquivo `groups.json`: Armazena informações dos grupos
- Arquivo `group_members.json`: Armazena membros dos grupos
- Arquivo `sessions.json`: Armazena sessões ativas
- Pasta `files/`: Armazena arquivos enviados pelos usuários

### 9.2 Exemplo de Implementação DAO
```java
public class UserDAO {
    private final String filePath = "data/users.json";
    private final ObjectMapper mapper = new ObjectMapper();
    
    public UserDAO() {
        // Garante que o diretório de dados existe
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    public User findById(String userId) {
        try {
            List<User> users = readAllUsers();
            return users.stream()
                .filter(u -> u.getUserId().equals(userId))
                .findFirst()
                .orElse(null);
        } catch (IOException e) {
            logger.error("Erro ao buscar usuário", e);
            return null;
        }
    }
    
    public boolean save(User user) {
        try {
            List<User> users = readAllUsers();
            // Remove o usuário se já existir (para atualização)
            users.removeIf(u -> u.getUserId().equals(user.getUserId()));
            users.add(user);
            writeAllUsers(users);
            return true;
        } catch (IOException e) {
            logger.error("Erro ao salvar usuário", e);
            return false;
        }
    }
    
    private synchronized List<User> readAllUsers() throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        return mapper.readValue(file, 
            mapper.getTypeFactory().constructCollectionType(List.class, User.class));
    }
    
    private synchronized void writeAllUsers(List<User> users) throws IOException {
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        mapper.writeValue(file, users);
    }
    
    // Outros métodos: delete, findAll, findByUsername, etc.
}
```

## 10. Segurança

### 10.1 Autenticação
- Implementar hash de senhas com bcrypt ou algoritmo similar
- Gerar tokens de sessão seguros e com expiração
- Validar sessão em todas as chamadas de método

### 10.2 Autorização
- Verificar permissões para operações em grupos
- Garantir que apenas administradores possam realizar certas ações
- Validar que usuários só possam acessar seus próprios dados

## 11. Inicialização do Servidor

### 11.1 WhatsUTServer.java
```java
public class WhatsUTServer {
    public static void main(String[] args) {
        try {
            // Configurar segurança RMI
            System.setProperty("java.security.policy", "server.policy");
            
            // Iniciar registro RMI
            Registry registry = LocateRegistry.createRegistry(1099);
            
            // Criar e registrar serviços
            AuthService authService = new AuthServiceImpl();
            MessageService messageService = new MessageServiceImpl();
            GroupService groupService = new GroupServiceImpl();
            UserService userService = new UserServiceImpl();
            NotificationService notificationService = new NotificationServiceImpl();
            
            registry.rebind("AuthService", authService);
            registry.rebind("MessageService", messageService);
            registry.rebind("GroupService", groupService);
            registry.rebind("UserService", userService);
            registry.rebind("NotificationService", notificationService);
            
            // Iniciar servidor WebSocket
            WebSocketServer webSocketServer = new WebSocketServer(8080);
            webSocketServer.start();
            
            System.out.println("Servidor WhatsUT iniciado com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro ao iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## 13. Documentação

### 13.1 Javadoc
- Documentar todas as interfaces e classes públicas
- Explicar parâmetros e retornos de métodos
- Documentar exceções que podem ser lançadas

### 13.2 Manual de Implantação
- Instruções para configurar o ambiente
- Passos para iniciar o servidor
- Configuração do sistema de armazenamento de arquivos

## 14. Considerações para Implantação

### 14.1 Requisitos de Sistema
- Java 11 ou superior
- Mínimo de 4GB de RAM
- Porta 1099 aberta para RMI
- Porta 8080 aberta para WebSocket
- Espaço em disco para armazenamento de arquivos JSON e arquivos enviados pelos usuários

### 14.2 Configuração
- Arquivo `config.properties` para configurações do servidor
- Arquivo `storage.properties` para configurações do armazenamento de dados
- Arquivo `server.policy` para configurações de segurança RMI

## 15. Cronograma de Desenvolvimento

1. **Semana 1**: Configuração do ambiente e estrutura básica
   - Configurar projeto Java
   - Implementar interfaces RMI
   - Configurar servidor WebSocket básico

2. **Semana 2**: Implementação dos serviços principais
   - Implementar AuthService
   - Implementar UserService
   - Configurar sistema de armazenamento de arquivos

3. **Semana 3**: Implementação de mensagens e grupos
   - Implementar MessageService
   - Implementar GroupService
   - Testar comunicação básica

4. **Semana 4**: Sistema de notificações e testes
   - Implementar NotificationService
   - Escrever testes unitários e de integração
   - Corrigir bugs e otimizar desempenho

5. **Semana 5**: Documentação e entrega
   - Escrever documentação completa
   - Preparar manual de implantação
   - Realizar testes finais

## 16. Entregáveis

1. Código-fonte completo do servidor Java
2. Estrutura inicial de arquivos JSON para armazenamento de dados
3. Documentação técnica (Javadoc)
4. Manual de implantação
5. Relatório de testes
6. Arquivo JAR executável do servidor

## 17. Diagramas UML

Para atender aos requisitos do professor, é necessário desenvolver os seguintes diagramas UML:

### 17.1 Diagrama de Classes
Representando a estrutura estática do sistema, incluindo:
- Classes de serviço (AuthService, MessageService, GroupService, etc.)
- Classes de modelo (User, Message, Group, etc.)
- Relações entre as classes

### 17.2 Diagrama de Sequência
Ilustrando as interações entre componentes para os principais fluxos:
- Login e autenticação
- Envio de mensagens privadas
- Envio de mensagens em grupo
- Adição/remoção de usuários em grupos
- Envio de arquivos

### 17.3 Diagrama de Atividades
Mostrando o fluxo de trabalho para processos como:
- Processo de login
- Criação de grupo
- Gerenciamento de permissões de grupo
- Envio e recebimento de mensagens

### 17.4 Diagrama de Colaboração
Detalhando como os objetos interagem para realizar tarefas específicas:
- Colaboração entre serviços para autenticação
- Colaboração para notificações em tempo real

## 18. Verificação de Requisitos

A seguir, verificamos como a implementação proposta atende a cada um dos requisitos solicitados pelo professor:

### 18.1 Autenticação Criptografada
- **Implementado por**: `AuthService.java` e `SecurityUtils.java`
- **Detalhes**: Senhas armazenadas com hash usando bcrypt, comunicação segura via SSL/TLS, tokens de sessão com expiração

### 18.2 Lista de Usuários
- **Implementado por**: `UserService.java` e `getUsers()` no frontend
- **Detalhes**: Lista de usuários com status online/offline, cache implementado para evitar requisições repetidas

### 18.3 Lista de Grupos
- **Implementado por**: `GroupService.java` e `getGroups()` no frontend
- **Detalhes**: Listagem de grupos, mecanismo de solicitação para entrar em grupos, aprovação pelo administrador

### 18.4 Dois Modos de Chat
- **Implementado por**: `MessageService.java` com métodos distintos para mensagens privadas e de grupo
- **Detalhes**: Chat privado entre dois usuários e chat em grupo com múltiplos participantes, permissões de entrada controladas pelo administrador

### 18.5 Envio de Arquivos
- **Implementado por**: Métodos de envio de mensagens com suporte a arquivos
- **Detalhes**: Envio de arquivos via mensagens privadas, com tratamento especial para diferentes tipos de arquivos

### 18.6 Exclusão
- **Implementado por**: `GroupService.java` com métodos de gerenciamento de usuários
- **Detalhes**: Remoção de usuários de grupos pelo administrador, transferência de administração quando o administrador sai, opção de eliminar grupo

### 18.7 Interface Intuitiva
- **Implementado por**: Frontend React já desenvolvido e interface de administração do servidor
- **Detalhes**: UI moderna com componentes reutilizáveis, feedback visual para ações, estados de carregamento

### 18.8 Callbacks e Interfaces de Configuração
- **Implementado por**: `NotificationService.java` e sistema de eventos
- **Detalhes**: Callbacks RMI para notificações em tempo real, interface de configuração do servidor com parâmetros ajustáveis
