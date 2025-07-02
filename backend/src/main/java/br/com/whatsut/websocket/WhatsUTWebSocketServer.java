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

import br.com.whatsut.websocket.handler.RequestHandler;
import br.com.whatsut.websocket.handler.AuthenticationHandler;
import br.com.whatsut.websocket.handler.UserHandler;
import br.com.whatsut.websocket.handler.MessageHandler;
import br.com.whatsut.websocket.handler.GroupHandler;

/**
 * Ponto central de entrada WebSocket da aplicação. Ele delega cada requisição JSON recebida
 * JSON request (identified by its <code>type</code> field) to a specialised
 * {@link br.com.whatsut.websocket.handler.RequestHandler} implementation.
 * <p>
 * Além de intermediar mensagens, também é responsável por:
 * <ul>
 *   <li>Gerenciar o mapa em memória que conecta cada {@link WebSocket} à
 *       to a user <code>sessionId</code>;</li>
 *   <li>Inicializar os stubs RMI utilizados pelos handlers específicos;</li>
 *   <li>Realizar validações simples de conexão/sessão.</li>
 * </ul>
 */
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

    /**
 * Verifica se um usuário está conectado em pelo menos uma sessão
 * WebSocket session.
 *
 * @param userId the user identifier to verify
 * @return <code>true</code> if the user has an open connection, otherwise
 *         <code>false</code>
 */
public boolean isUserOnline(String userId) {
        return sessions.values().stream().anyMatch(id -> id != null && id.equals(userId));
    }
    
    /**
 * Localiza os stubs RMI remotos necessários para a aplicação. Todos os serviços estão
 * published on <code>localhost:1099</code> and share the convention that the
 * RMI binding name equals the simple service interface name.
 */
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
    /**
 * Disparado quando um novo handshake WebSocket é concluído.
 * <p>
 * If the client provides a valid <code>sessionId</code> query parameter the
 * method binds that session to the newly created connection and pushes any
 * pending notifications to the user.
 */
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
    /**
 * Realiza limpeza quando um socket é desconectado.
 */
public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Conexão fechada: " + conn.getRemoteSocketAddress());
        sessions.remove(conn);
    }
    
    @Override
    /**
 * Parses incoming JSON payloads, recognises the <code>type</code> field and
 * forwards the request to {@link #processRequest(WebSocket,String,Map,Map)}.
 */
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
    
    /**
 * Delegar a requisição ao handler especializado registrado em
 * {@link #handlerMap}. If no handler matches, an error response is returned.
 */
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
    /**
 * Tratamento global de exceções do servidor WebSocket.
 */
public void onError(WebSocket conn, Exception ex) {
        System.err.println("Erro na conexão: " + (conn != null ? conn.getRemoteSocketAddress() : "null"));
        ex.printStackTrace();
    }
    
    @Override
    /**
 * Callback executado quando o listener TCP começa a aceitar
 * connections.
 */
public void onStart() {
        System.out.println("Servidor WebSocket iniciado na porta " + getPort());
    }

    /**
     * Registra os handlers responsáveis por diferentes tipos de requisição,
     * mantendo esta classe mais enxuta.
     */
    /**
 * Preenche {@link #handlerMap} com todos os mapeamentos tipo de requisição → handler. Isso
 * indirection keeps this class agnostic of the business logic details.
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
    
    
    /**
 * Extrai o parâmetro <code>sessionId</code> da query string presente no
 * descriptor sent during the WebSocket handshake.
 *
 * @param resourceDescriptor Caminho bruto + query string, por exemplo "/?sessionId=abc"
 * @return o identificador de sessão extraído ou <code>null</code> caso não exista
 */
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

