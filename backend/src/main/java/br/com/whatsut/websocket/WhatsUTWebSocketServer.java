package br.com.whatsut.websocket;

import br.com.whatsut.dao.GroupDAO;
import br.com.whatsut.dao.GroupMemberDAO;
import br.com.whatsut.dao.MessageDAO;
import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.*;
import br.com.whatsut.service.AuthService;
import br.com.whatsut.service.GroupService;
import br.com.whatsut.service.MessageService;
import br.com.whatsut.service.UserService;
import br.com.whatsut.util.ConfigManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servidor WebSocket para comunicação em tempo real com o frontend.
 */
public class WhatsUTWebSocketServer extends WebSocketServer {
    private static final Logger logger = Logger.getLogger(WhatsUTWebSocketServer.class.getName());
    private final ObjectMapper objectMapper;
    private final Map<WebSocket, String> socketSessionMap;
    private final Map<String, WebSocket> sessionSocketMap;
    private final Map<String, String> sessionUserMap;
    private final WebSocketServerHandlers handlers;
    
    // Serviços RMI
    private AuthService authService;
    private UserService userService;
    private GroupService groupService;
    private MessageService messageService;
    
    // DAOs para acesso direto (otimização)
    private final UserDAO userDAO;
    private final GroupDAO groupDAO;
    private final GroupMemberDAO groupMemberDAO;
    private final MessageDAO messageDAO;
    
    /**
     * Construtor do servidor WebSocket.
     *
     * @param port Porta para o servidor WebSocket
     */
    public WhatsUTWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        this.objectMapper = new ObjectMapper();
        this.socketSessionMap = new ConcurrentHashMap<>();
        this.sessionSocketMap = new ConcurrentHashMap<>();
        this.sessionUserMap = new ConcurrentHashMap<>();
        this.handlers = new WebSocketServerHandlers();
        
        // Inicializar DAOs
        this.userDAO = new UserDAO();
        this.groupDAO = new GroupDAO();
        this.groupMemberDAO = new GroupMemberDAO();
        this.messageDAO = new MessageDAO();
        
        // Conectar aos serviços RMI
        connectToRmiServices();
    }
    
    /**
     * Conecta aos serviços RMI.
     */
    private void connectToRmiServices() {
        try {
            String host = ConfigManager.getProperty("server.host", "localhost");
            int rmiPort = ConfigManager.getIntProperty("server.rmi.port", 1099);
            
            Registry registry = LocateRegistry.getRegistry(host, rmiPort);
            
            authService = (AuthService) registry.lookup("AuthService");
            userService = (UserService) registry.lookup("UserService");
            groupService = (GroupService) registry.lookup("GroupService");
            messageService = (MessageService) registry.lookup("MessageService");
            
            logger.info("Conectado aos serviços RMI com sucesso");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao conectar aos serviços RMI", e);
        }
    }
    
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String sessionId = handshake.getFieldValue("X-Session-Id");
        String token = handshake.getFieldValue("X-Auth-Token");
        
        if (sessionId == null || token == null) {
            logger.warning("Tentativa de conexão sem credenciais");
            sendErrorMessage(conn, "authentication_required", "Autenticação necessária");
            conn.close();
            return;
        }
        
        try {
            // Validar token
            boolean valid = authService.validateToken(sessionId, token);
            if (!valid) {
                logger.warning("Token inválido para sessão: " + sessionId);
                sendErrorMessage(conn, "invalid_token", "Token inválido");
                conn.close();
                return;
            }
            
            // Obter ID do usuário da sessão
            String userId = sessionUserMap.get(sessionId);
            if (userId == null) {
                // Buscar usuário associado à sessão
                // Implementação simplificada, em produção seria necessário buscar a sessão
                User user = userService.getUserByUsername(sessionId);
                if (user != null) {
                    userId = user.getUserId();
                    sessionUserMap.put(sessionId, userId);
                } else {
                    logger.warning("Usuário não encontrado para sessão: " + sessionId);
                    sendErrorMessage(conn, "user_not_found", "Usuário não encontrado");
                    conn.close();
                    return;
                }
            }
            
            // Registrar conexão
            socketSessionMap.put(conn, sessionId);
            sessionSocketMap.put(sessionId, conn);
            
            // Atualizar status do usuário para online
            userService.updateUserStatus(userId, true);
            
            logger.info("Nova conexão WebSocket: " + conn.getRemoteSocketAddress() + " - Sessão: " + sessionId);
            
            // Enviar confirmação de conexão
            Map<String, Object> response = new HashMap<>();
            response.put("type", "connection_success");
            response.put("userId", userId);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar conexão WebSocket", e);
            sendErrorMessage(conn, "server_error", "Erro interno do servidor");
            conn.close();
        }
    }
    
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        String sessionId = socketSessionMap.get(conn);
        if (sessionId != null) {
            String userId = sessionUserMap.get(sessionId);
            if (userId != null) {
                try {
                    // Atualizar status do usuário para offline
                    userService.updateUserStatus(userId, false);
                } catch (RemoteException e) {
                    logger.log(Level.WARNING, "Erro ao atualizar status do usuário", e);
                }
            }
            
            // Remover mapeamentos
            socketSessionMap.remove(conn);
            sessionSocketMap.remove(sessionId);
        }
        
        logger.info("Conexão WebSocket fechada: " + conn.getRemoteSocketAddress() + " - Código: " + code + " - Razão: " + reason);
    }
    
    @Override
    public void onMessage(WebSocket conn, String message) {
        String sessionId = socketSessionMap.get(conn);
        if (sessionId == null) {
            sendErrorMessage(conn, "not_authenticated", "Não autenticado");
            return;
        }
        
        try {
            // Processar mensagem recebida
            Map<String, Object> request = objectMapper.readValue(message, new com.fasterxml.jackson.core.type.TypeReference<HashMap<String, Object>>() {});
            String type = (String) request.get("type");
            
            if (type == null) {
                sendErrorMessage(conn, "invalid_request", "Tipo de requisição não especificado");
                return;
            }
            
            // Processar diferentes tipos de requisição
            switch (type) {
                case "get_user":
                    handlers.handleGetUser(this, conn, request);
                    break;
                case "get_users":
                    handlers.handleGetUsers(this, conn, request);
                    break;
                case "get_groups":
                    handlers.handleGetGroups(this, conn, request);
                    break;
                case "get_group_members":
                    handlers.handleGetGroupMembers(this, conn, request);
                    break;
                case "get_conversations":
                    handlers.handleGetConversations(this, conn, request);
                    break;
                case "get_messages":
                    handlers.handleGetMessages(this, conn, request);
                    break;
                case "send_message":
                    handlers.handleSendMessage(this, conn, request);
                    break;
                case "create_group":
                    handlers.handleCreateGroup(this, conn, request);
                    break;
                case "add_user_to_group":
                    handlers.handleAddUserToGroup(this, conn, request);
                    break;
                case "remove_user_from_group":
                    handlers.handleRemoveUserFromGroup(this, conn, request);
                    break;
                default:
                    sendErrorMessage(conn, "unknown_request_type", "Tipo de requisição desconhecido: " + type);
                    break;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar mensagem WebSocket", e);
            sendErrorMessage(conn, "server_error", "Erro ao processar requisição: " + e.getMessage());
        }
    }
    
    @Override
    public void onError(WebSocket conn, Exception ex) {
        logger.log(Level.SEVERE, "Erro em conexão WebSocket: " + (conn != null ? conn.getRemoteSocketAddress() : "null"), ex);
    }
    
    @Override
    public void onStart() {
        logger.info("Servidor WebSocket iniciado na porta " + getPort());
    }
    
    /**
     * Envia uma mensagem de erro para o cliente.
     *
     * @param conn Conexão WebSocket
     * @param code Código de erro
     * @param message Mensagem de erro
     */
    private void sendErrorMessage(WebSocket conn, String code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("type", "error");
        error.put("code", code);
        error.put("message", message);
        sendJsonMessage(conn, error);
    }
    
    /**
     * Envia um objeto como JSON para o cliente.
     *
     * @param conn Conexão WebSocket
     * @param object Objeto a ser enviado
     */
    private void sendJsonMessage(WebSocket conn, Object object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            conn.send(json);
        } catch (JsonProcessingException e) {
            logger.log(Level.SEVERE, "Erro ao serializar objeto para JSON", e);
        }
    }
    
    /**
     * Obtém a conexão WebSocket de um usuário pelo ID.
     *
     * @param userId ID do usuário
     * @return Conexão WebSocket ou null se não encontrada
     */
    public WebSocket getConnectionByUserId(String userId) {
        String sessionId = null;
        for (Map.Entry<String, String> entry : sessionUserMap.entrySet()) {
            if (entry.getValue().equals(userId)) {
                sessionId = entry.getKey();
                break;
            }
        }
        
        if (sessionId != null) {
            return sessionSocketMap.get(sessionId);
        }
        
        return null;
    }
    
    /**
     * Obtém o serviço de autenticação.
     *
     * @return Serviço de autenticação
     */
    public AuthService getAuthService() {
        return authService;
    }
    
    /**
     * Obtém o serviço de usuários.
     *
     * @return Serviço de usuários
     */
    public UserService getUserService() {
        return userService;
    }
    
    /**
     * Obtém o serviço de grupos.
     *
     * @return Serviço de grupos
     */
    public GroupService getGroupService() {
        return groupService;
    }
    
    /**
     * Obtém o serviço de mensagens.
     *
     * @return Serviço de mensagens
     */
    public MessageService getMessageService() {
        return messageService;
    }
    
    /**
     * Obtém o DAO de usuários.
     *
     * @return DAO de usuários
     */
    public UserDAO getUserDAO() {
        return userDAO;
    }
    
    /**
     * Obtém o DAO de grupos.
     *
     * @return DAO de grupos
     */
    public GroupDAO getGroupDAO() {
        return groupDAO;
    }
    
    /**
     * Obtém o DAO de membros de grupo.
     *
     * @return DAO de membros de grupo
     */
    public GroupMemberDAO getGroupMemberDAO() {
        return groupMemberDAO;
    }
    
    /**
     * Obtém o DAO de mensagens.
     *
     * @return DAO de mensagens
     */
    public MessageDAO getMessageDAO() {
        return messageDAO;
    }
}
