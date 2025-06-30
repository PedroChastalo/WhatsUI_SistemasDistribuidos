package br.com.whatsut.websocket;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.whatsut.service.AuthenticationService;
import br.com.whatsut.service.UserService;
import br.com.whatsut.service.MessageService;
import br.com.whatsut.service.GroupService;
import br.com.whatsut.model.User;

public class WhatsUTWebSocketServer extends WebSocketServer {
    private final Map<WebSocket, String> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Serviços RMI
    private AuthenticationService authService;
    private UserService userService;
    private MessageService messageService;
    private GroupService groupService;
    
    public WhatsUTWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        initRMIServices();
    }
    
    private void initRMIServices() {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            
            // Conectar aos serviços RMI
            authService = (AuthenticationService) registry.lookup("AuthenticationService");
            userService = (UserService) registry.lookup("UserService");
            messageService = (MessageService) registry.lookup("MessageService");
            groupService = (GroupService) registry.lookup("GroupService");
            
            System.out.println("Serviços RMI conectados com sucesso!");
        } catch (Exception e) {
            System.err.println("Erro ao conectar aos serviços RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("Nova conexão: " + conn.getRemoteSocketAddress());
        
        // Verificar se há um sessionId na URL
        String sessionId = getSessionIdFromQuery(handshake.getResourceDescriptor());
        if (sessionId != null) {
            try {
                if (authService.validateSession(sessionId)) {
                    sessions.put(conn, sessionId);
                    System.out.println("Sessão validada: " + sessionId);
                } else {
                    System.out.println("Sessão inválida: " + sessionId);
                }
            } catch (Exception e) {
                System.err.println("Erro ao validar sessão: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Conexão fechada: " + conn.getRemoteSocketAddress());
        sessions.remove(conn);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            Map<String, Object> request = objectMapper.readValue(message, Map.class);
            String type = (String) request.get("type");
            String requestId = (String) request.get("requestId");
            Map<String, Object> data = (Map<String, Object>) request.get("data");
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", type + "_response");
            response.put("requestId", requestId);
            
            // Tratar mensagens de ping/pong para manter a conexão
            if ("ping".equals(type)) {
                response.put("type", "pong");
                conn.send(objectMapper.writeValueAsString(response));
                return;
            }
            
            // Processar requisições com base no tipo
            processRequest(conn, type, data, response);
            
        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void processRequest(WebSocket conn, String type, Map<String, Object> data, Map<String, Object> response) {
        try {
            switch (type) {
                // Autenticação
                case "login":
                    handleLogin(conn, data, response);
                    break;
                case "register":
                    handleRegister(conn, data, response);
                    break;
                case "logout":
                    handleLogout(conn, response);
                    break;
                    
                // Usuários
                case "getUsers":
                    handleGetUsers(conn, response);
                    break;
                case "getUser":
                    handleGetUser(conn, data, response);
                    break;
                case "updateStatus":
                    handleUpdateStatus(conn, data, response);
                    break;
                    
                // Mensagens privadas
                case "getPrivateConversations":
                    handleGetPrivateConversations(conn, response);
                    break;
                case "getPrivateMessages":
                    handleGetPrivateMessages(conn, data, response);
                    break;
                case "sendPrivateMessage":
                    handleSendPrivateMessage(conn, data, response);
                    break;
                    
                // Grupos
                case "getGroups":
                    handleGetGroups(conn, response);
                    break;
                case "createGroup":
                    handleCreateGroup(conn, data, response);
                    break;
                case "getGroupMessages":
                    handleGetGroupMessages(conn, data, response);
                    break;
                case "sendGroupMessage":
                    handleSendGroupMessage(conn, data, response);
                    break;
                case "getGroupMembers":
                    handleGetGroupMembers(conn, data, response);
                    break;
                case "addUserToGroup":
                    handleAddUserToGroup(conn, data, response);
                    break;
                case "removeUserFromGroup":
                    handleRemoveUserFromGroup(conn, data, response);
                    break;
                case "setGroupAdmin":
                    handleSetGroupAdmin(conn, data, response);
                    break;
                case "leaveGroup":
                    handleLeaveGroup(conn, data, response);
                    break;
                case "deleteGroup":
                    handleDeleteGroup(conn, data, response);
                    break;
                    
                default:
                    response.put("success", false);
                    response.put("error", "Tipo de requisição desconhecido: " + type);
            }
            
            conn.send(objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            try {
                response.put("success", false);
                response.put("error", e.getMessage());
                conn.send(objectMapper.writeValueAsString(response));
            } catch (Exception ex) {
                System.err.println("Erro ao enviar resposta de erro: " + ex.getMessage());
            }
        }
    }
    
    private void handleLogin(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String email = (String) data.get("email");
        String password = (String) data.get("password");
        
        Map<String, Object> result = authService.login(email, password);
        
        if ((boolean) result.get("success")) {
            Map<String, Object> userData = (Map<String, Object>) result.get("data");
            String sessionId = (String) userData.get("sessionId");
            sessions.put(conn, sessionId);
        }
        
        response.putAll(result);
    }
    
    private void handleRegister(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String username = (String) data.get("username");
        String email = (String) data.get("email");
        String displayName = (String) data.get("displayName");
        String password = (String) data.get("password");
        
        Map<String, Object> result = authService.register(username, email, displayName, password);
        
        if ((boolean) result.get("success")) {
            Map<String, Object> userData = (Map<String, Object>) result.get("data");
            String sessionId = (String) userData.get("sessionId");
            sessions.put(conn, sessionId);
        }
        
        response.putAll(result);
    }
    
    private void handleLogout(WebSocket conn, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        
        if (sessionId != null) {
            boolean success = authService.logout(sessionId);
            sessions.remove(conn);
            
            response.put("success", success);
            if (!success) {
                response.put("error", "Erro ao fazer logout");
            }
        } else {
            response.put("success", false);
            response.put("error", "Sessão não encontrada");
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Erro na conexão: " + (conn != null ? conn.getRemoteSocketAddress() : "null"));
        ex.printStackTrace();
    }
    
    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket iniciado na porta " + getPort());
    }
    
    // ===== Handlers para requisições de usuários =====
    
    private void handleGetUsers(WebSocket conn, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        try {
            List<User> users = userService.getAllUsers(sessionId);
            response.put("success", true);
            response.put("data", users);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao obter usuários: " + e.getMessage());
            throw e;
        }
    }
    
    private void handleGetUser(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        String userId = (String) data.get("userId");
        if (userId == null) {
            response.put("success", false);
            response.put("error", "ID do usuário não fornecido");
            return;
        }
        
        try {
            User user = userService.getUserById(sessionId, userId);
            if (user == null) {
                response.put("success", false);
                response.put("error", "Usuário não encontrado");
                return;
            }
            
            response.put("success", true);
            response.put("data", user);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao obter usuário: " + e.getMessage());
            throw e;
        }
    }
    
    private void handleUpdateStatus(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        String status = (String) data.get("status");
        if (status == null) {
            response.put("success", false);
            response.put("error", "Status não fornecido");
            return;
        }
        
        try {
            boolean success = userService.updateStatus(sessionId, status);
            response.put("success", success);
            if (!success) {
                response.put("error", "Não foi possível atualizar o status");
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao atualizar status: " + e.getMessage());
            throw e;
        }
    }
    
    // ===== Handlers para mensagens privadas =====
    
    private void handleGetPrivateConversations(WebSocket conn, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        try {
            List<Map<String, Object>> conversations = messageService.getPrivateConversations(sessionId);
            response.put("success", true);
            response.put("data", conversations);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao obter conversas privadas: " + e.getMessage());
            throw e;
        }
    }
    
    private void handleGetPrivateMessages(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        String userId = (String) data.get("userId");
        if (userId == null) {
            // Verificar se foi enviado como receiverId (para compatibilidade)
            userId = (String) data.get("receiverId");
            if (userId == null) {
                response.put("success", false);
                response.put("error", "ID do usuário não fornecido");
                return;
            }
        }
        
        try {
            List<Map<String, Object>> messages = messageService.getPrivateMessages(sessionId, userId);
            response.put("success", true);
            response.put("data", messages);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao obter mensagens privadas: " + e.getMessage());
            throw e;
        }
    }
    
    private void handleSendPrivateMessage(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        // Aceitar tanto userId quanto receiverId para compatibilidade
        String receiverId = (String) data.get("userId");
        if (receiverId == null) {
            receiverId = (String) data.get("receiverId");
            if (receiverId == null) {
                response.put("success", false);
                response.put("error", "ID do destinatário não fornecido");
                return;
            }
        }
        
        String content = (String) data.get("content");
        if (content == null) {
            response.put("success", false);
            response.put("error", "Conteúdo da mensagem não fornecido");
            return;
        }
        
        try {
            Map<String, Object> message = messageService.sendPrivateMessage(sessionId, receiverId, content);
            response.put("success", true);
            response.put("data", message);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao enviar mensagem privada: " + e.getMessage());
            throw e;
        }
    }
    
    // ===== Handlers para grupos =====
    
    private void handleGetGroups(WebSocket conn, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        try {
            List<Map<String, Object>> groups = groupService.getGroups(sessionId);
            response.put("success", true);
            response.put("data", groups);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao obter grupos: " + e.getMessage());
            throw e;
        }
    }
    
    private void handleCreateGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        String name = (String) data.get("name");
        if (name == null) {
            response.put("success", false);
            response.put("error", "Nome do grupo não fornecido");
            return;
        }
        
        List<String> members = new ArrayList<>();
        Object membersObj = data.get("members");
        if (membersObj instanceof List) {
            for (Object member : (List<?>) membersObj) {
                if (member instanceof String) {
                    members.add((String) member);
                }
            }
        }
        
        try {
            Map<String, Object> group = groupService.createGroup(sessionId, name, members);
            response.put("success", true);
            response.put("data", group);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao criar grupo: " + e.getMessage());
            throw e;
        }
    }
    
    private void handleGetGroupMessages(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        String groupId = (String) data.get("groupId");
        if (groupId == null) {
            response.put("success", false);
            response.put("error", "ID do grupo não fornecido");
            return;
        }
        
        try {
            List<Map<String, Object>> messages = groupService.getGroupMessages(sessionId, groupId);
            response.put("success", true);
            response.put("data", messages);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao obter mensagens do grupo: " + e.getMessage());
            throw e;
        }
    }
    
    private void handleSendGroupMessage(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        String groupId = (String) data.get("groupId");
        if (groupId == null) {
            response.put("success", false);
            response.put("error", "ID do grupo não fornecido");
            return;
        }
        
        String content = (String) data.get("content");
        if (content == null) {
            response.put("success", false);
            response.put("error", "Conteúdo da mensagem não fornecido");
            return;
        }
        
        try {
            Map<String, Object> message = groupService.sendGroupMessage(sessionId, groupId, content);
            response.put("success", true);
            response.put("data", message);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao enviar mensagem para o grupo: " + e.getMessage());
            throw e;
        }
    }
    
    private void handleGetGroupMembers(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        String groupId = (String) data.get("groupId");
        if (groupId == null) {
            response.put("success", false);
            response.put("error", "ID do grupo não fornecido");
            return;
        }
        
        try {
            List<Map<String, Object>> members = groupService.getGroupMembers(sessionId, groupId);
            response.put("success", true);
            response.put("data", members);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao obter membros do grupo: " + e.getMessage());
            throw e;
        }
    }
    
    // Métodos para gerenciamento de membros do grupo
    
    private void handleAddUserToGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }

        String groupId = (String) data.get("groupId");
        String userIdToAdd = (String) data.get("userIdToAdd");
        if (groupId == null || userIdToAdd == null) {
            response.put("success", false);
            response.put("error", "Parâmetros obrigatórios não fornecidos");
            return;
        }

        try {
            groupService.addUserToGroup(sessionId, groupId, userIdToAdd);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
    }

    private void handleRemoveUserFromGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }

        String groupId = (String) data.get("groupId");
        String userIdToRemove = (String) data.get("userIdToRemove");
        if (groupId == null || userIdToRemove == null) {
            response.put("success", false);
            response.put("error", "Parâmetros obrigatórios não fornecidos");
            return;
        }

        try {
            groupService.removeUserFromGroup(sessionId, groupId, userIdToRemove);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
    }
    
    private void handleSetGroupAdmin(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }

        String groupId = (String) data.get("groupId");
        String userIdToSetAdmin = (String) data.get("userIdToSetAdmin");
        if (groupId == null || userIdToSetAdmin == null) {
            response.put("success", false);
            response.put("error", "Parâmetros obrigatórios não fornecidos");
            return;
        }

        try {
            groupService.setGroupAdmin(sessionId, groupId, userIdToSetAdmin);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
    }
    
    private void handleLeaveGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }

        String groupId = (String) data.get("groupId");
        if (groupId == null) {
            response.put("success", false);
            response.put("error", "ID do grupo não fornecido");
            return;
        }

        try {
            groupService.leaveGroup(sessionId, groupId);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
    }
    
    private void handleDeleteGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }

        String groupId = (String) data.get("groupId");
        if (groupId == null) {
            response.put("success", false);
            response.put("error", "ID do grupo não fornecido");
            return;
        }

        try {
            groupService.deleteGroup(sessionId, groupId);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
        }
    }
    
    private String getSessionIdFromQuery(String resourceDescriptor) {
        if (resourceDescriptor == null || !resourceDescriptor.contains("?")) {
            return null;
        }
        
        String query = resourceDescriptor.split("\\?")[1];
        String[] params = query.split("&");
        
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equals("sessionId")) {
                return keyValue[1];
            }
        }
        
        return null;
    }
}
