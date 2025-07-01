package br.com.whatsut.websocket;

import java.net.InetSocketAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.whatsut.dao.PendingJoinRequestDAO;
import br.com.whatsut.model.User;
import br.com.whatsut.security.SessionManager;
import br.com.whatsut.service.AuthenticationService;
import br.com.whatsut.service.GroupService;
import br.com.whatsut.service.MessageService;
import br.com.whatsut.service.UserService;
// --- novos imports para handlers ---
import br.com.whatsut.websocket.handler.RequestHandler;
import br.com.whatsut.websocket.handler.AuthenticationHandler;
import br.com.whatsut.websocket.handler.UserHandler;
import br.com.whatsut.websocket.handler.MessageHandler;
import br.com.whatsut.websocket.handler.GroupHandler;

public class WhatsUTWebSocketServer extends WebSocketServer {
    private final ConcurrentMap<WebSocket, String> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PendingJoinRequestDAO pendingJoinRequestDAO = new PendingJoinRequestDAO();
    
    // Serviços RMI
    private AuthenticationService authService;
    private UserService userService;
    private MessageService messageService;
    private GroupService groupService;

    // Mapeamento de tipo de requisição -> handler dedicado
    private final Map<String, RequestHandler> handlerMap = new HashMap<>();
    
    public WhatsUTWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        initRMIServices();
        registerHandlers();
    }

    public boolean isUserOnline(String userId) {
        return sessions.values().stream().anyMatch(id -> id != null && id.equals(userId));
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
                    // Obter o userId a partir da sessão
                    String userId = SessionManager.getUserIdFromSession(sessionId);
                    if (userId == null) {
                        System.err.println("Sessão inválida: " + sessionId);
                        return;
                    }
                    
                    // Enviar notificações pendentes se for admin
                    User user = userService.getUserById(sessionId, userId);
                    if (user != null) {
                        List<PendingJoinRequestDAO.JoinRequest> pendings = pendingJoinRequestDAO.getRequests(user.getUserId());
                        for (PendingJoinRequestDAO.JoinRequest req : pendings) {
                            Map<String, Object> notify = new HashMap<>();
                            notify.put("type", "joinGroupRequest");
                            notify.put("groupId", req.groupId);
                            notify.put("groupName", req.groupName);
                            notify.put("userId", req.userId);
                            notify.put("userName", req.userName);
                            conn.send(objectMapper.writeValueAsString(notify));
                        }
                    }
                }
                // ...existing code...
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
            // 1) Tenta delegar a um handler externo (AuthenticationHandler, UserHandler, etc.)
            RequestHandler externalHandler = handlerMap.get(type);
            if (externalHandler != null) {
                externalHandler.handle(type, conn, data, response);
            } else {
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

    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("Erro na conexão: " + (conn != null ? conn.getRemoteSocketAddress() : "null"));
        ex.printStackTrace();
    }
    
    @Override
    public void onStart() {
        System.out.println("Servidor WebSocket iniciado na porta " + getPort());
    }

    /**
     * Registra os handlers responsáveis por diferentes tipos de requisição,
     * mantendo esta classe mais enxuta.
     */
    private void registerHandlers() {
        AuthenticationHandler authHandler = new AuthenticationHandler(sessions, authService);
        UserHandler userHandler = new UserHandler(sessions, userService, this::isUserOnline);
        MessageHandler messageHandler = new MessageHandler(sessions, messageService);
        GroupHandler groupHandler = new GroupHandler(sessions, groupService, userService);

        // Autenticação
        handlerMap.put("login", authHandler);
        handlerMap.put("register", authHandler);
        handlerMap.put("logout", authHandler);

        // Usuários
        handlerMap.put("getUsers", userHandler);
        handlerMap.put("getUser", userHandler);
        handlerMap.put("updateStatus", userHandler);

        // Mensagens privadas
        handlerMap.put("getPrivateConversations", messageHandler);
        handlerMap.put("getPrivateMessages", messageHandler);
        handlerMap.put("sendPrivateMessage", messageHandler);
        handlerMap.put("sendPrivateFile", messageHandler);

        // Grupos
        handlerMap.put("getGroups", groupHandler);
        handlerMap.put("getAllAvailableGroups", groupHandler);
        handlerMap.put("createGroup", groupHandler);
        handlerMap.put("getGroupMessages", groupHandler);
        handlerMap.put("sendGroupMessage", groupHandler);
        handlerMap.put("sendGroupFile", groupHandler);
        handlerMap.put("getGroupMembers", groupHandler);
        handlerMap.put("addUserToGroup", groupHandler);
        handlerMap.put("removeUserFromGroup", groupHandler);
        handlerMap.put("setGroupAdmin", groupHandler);
        handlerMap.put("leaveGroup", groupHandler);
        handlerMap.put("deleteGroup", groupHandler);
        handlerMap.put("requestJoinGroup", groupHandler);
        handlerMap.put("respondJoinGroup", groupHandler);
        handlerMap.put("removeJoinRequestNotification", groupHandler);
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

    /**
     * Processa uma solicitação para entrar em um grupo
     * @param conn Conexão WebSocket
     * @param data Dados da requisição
     * @param response Resposta a ser enviada
     * @throws Exception Em caso de erro
     */
    /**
     * Encontra a conexão WebSocket de um usuário pelo seu ID
     * @param userId ID do usuário
     * @return Conexão WebSocket ou null se não encontrada
     */
}

