package br.com.whatsut.websocket;

import br.com.whatsut.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.java_websocket.WebSocket;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manipuladores de requisições para o servidor WebSocket.
 * Esta classe contém os métodos para processar diferentes tipos de requisições do cliente.
 */
public class WebSocketServerHandlers {
    private static final Logger logger = Logger.getLogger(WebSocketServerHandlers.class.getName());
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Processa requisição para obter informações de um usuário.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleGetUser(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            if (userId == null) {
                sendErrorMessage(conn, "missing_parameter", "Parâmetro userId não especificado");
                return;
            }
            
            User user = server.getUserService().getUser(userId);
            if (user == null) {
                sendErrorMessage(conn, "user_not_found", "Usuário não encontrado");
                return;
            }
            
            // Remover informações sensíveis
            user.setPasswordHash(null);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "user_info");
            response.put("user", user);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição getUser", e);
            sendErrorMessage(conn, "server_error", "Erro ao buscar informações do usuário");
        }
    }
    
    /**
     * Processa requisição para obter lista de usuários.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleGetUsers(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            List<User> users = server.getUserService().getAllUsers();
            
            // Remover informações sensíveis
            users.forEach(user -> user.setPasswordHash(null));
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "users_list");
            response.put("users", users);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição getUsers", e);
            sendErrorMessage(conn, "server_error", "Erro ao buscar lista de usuários");
        }
    }
    
    /**
     * Processa requisição para obter grupos de um usuário.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleGetGroups(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            if (userId == null) {
                sendErrorMessage(conn, "missing_parameter", "Parâmetro userId não especificado");
                return;
            }
            
            List<Group> groups = server.getGroupService().getUserGroups(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "groups_list");
            response.put("groups", groups);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição getGroups", e);
            sendErrorMessage(conn, "server_error", "Erro ao buscar grupos do usuário");
        }
    }
    
    /**
     * Processa requisição para obter membros de um grupo.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleGetGroupMembers(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            String groupId = (String) request.get("groupId");
            if (groupId == null) {
                sendErrorMessage(conn, "missing_parameter", "Parâmetro groupId não especificado");
                return;
            }
            
            List<User> users = server.getGroupService().getGroupUsers(groupId);
            
            // Remover informações sensíveis
            users.forEach(user -> user.setPasswordHash(null));
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "group_members");
            response.put("groupId", groupId);
            response.put("members", users);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição getGroupMembers", e);
            sendErrorMessage(conn, "server_error", "Erro ao buscar membros do grupo");
        }
    }
    
    /**
     * Processa requisição para obter conversas de um usuário.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleGetConversations(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            if (userId == null) {
                sendErrorMessage(conn, "missing_parameter", "Parâmetro userId não especificado");
                return;
            }
            
            // Obter grupos do usuário
            List<Group> groups = server.getGroupService().getUserGroups(userId);
            
            // Obter conversas privadas (implementação simplificada)
            List<Map<String, Object>> privateConversations = new ArrayList<>();
            List<User> allUsers = server.getUserService().getAllUsers();
            
            for (User user : allUsers) {
                if (!user.getUserId().equals(userId)) {
                    // Para cada usuário, verificar se há mensagens trocadas
                    List<PrivateMessage> messages = server.getMessageService().getPrivateMessages(userId, user.getUserId(), 1, 0);
                    
                    if (!messages.isEmpty() || true) { // Em produção, remover o "|| true"
                        Map<String, Object> conversation = new HashMap<>();
                        conversation.put("userId", user.getUserId());
                        conversation.put("displayName", user.getDisplayName());
                        conversation.put("username", user.getUsername());
                        conversation.put("online", user.isOnline());
                        conversation.put("lastSeen", user.getLastSeen());
                        
                        // Última mensagem, se houver
                        if (!messages.isEmpty()) {
                            PrivateMessage lastMessage = messages.get(0);
                            conversation.put("lastMessage", lastMessage.getContent());
                            conversation.put("timestamp", lastMessage.getTimestamp());
                            conversation.put("unread", !lastMessage.isRead() && lastMessage.getReceiverId().equals(userId));
                        } else {
                            conversation.put("lastMessage", "");
                            conversation.put("timestamp", 0);
                            conversation.put("unread", false);
                        }
                        
                        privateConversations.add(conversation);
                    }
                }
            }
            
            // Converter grupos para o formato de conversas
            List<Map<String, Object>> groupConversations = new ArrayList<>();
            for (Group group : groups) {
                Map<String, Object> conversation = new HashMap<>();
                conversation.put("groupId", group.getGroupId());
                conversation.put("name", group.getName());
                conversation.put("description", group.getDescription());
                conversation.put("isAdmin", group.getAdminId().equals(userId));
                
                // Última mensagem do grupo, se houver
                List<GroupMessage> messages = server.getMessageService().getGroupMessages(group.getGroupId(), 1, 0);
                if (!messages.isEmpty()) {
                    GroupMessage lastMessage = messages.get(0);
                    conversation.put("lastMessage", lastMessage.getContent());
                    conversation.put("timestamp", lastMessage.getTimestamp());
                    conversation.put("unread", !lastMessage.isRead());
                } else {
                    conversation.put("lastMessage", "");
                    conversation.put("timestamp", group.getCreatedAt());
                    conversation.put("unread", false);
                }
                
                groupConversations.add(conversation);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "conversations");
            response.put("privateConversations", privateConversations);
            response.put("groupConversations", groupConversations);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição getConversations", e);
            sendErrorMessage(conn, "server_error", "Erro ao buscar conversas do usuário");
        }
    }
    
    /**
     * Processa requisição para obter mensagens.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleGetMessages(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            String userId = (String) request.get("userId");
            String targetId = (String) request.get("targetId");
            Boolean isGroup = (Boolean) request.get("isGroup");
            Integer limit = (Integer) request.get("limit");
            Integer offset = (Integer) request.get("offset");
            
            if (userId == null || targetId == null || isGroup == null) {
                sendErrorMessage(conn, "missing_parameter", "Parâmetros obrigatórios não especificados");
                return;
            }
            
            if (limit == null) limit = 20;
            if (offset == null) offset = 0;
            
            List<? extends Message> messages;
            
            if (isGroup) {
                // Mensagens de grupo
                messages = server.getMessageService().getGroupMessages(targetId, limit, offset);
            } else {
                // Mensagens privadas
                messages = server.getMessageService().getPrivateMessages(userId, targetId, limit, offset);
                
                // Marcar mensagens como lidas
                for (Message message : messages) {
                    if (!message.isRead() && !message.getSenderId().equals(userId)) {
                        server.getMessageService().markMessageAsRead(message.getMessageId(), userId);
                    }
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "messages");
            response.put("isGroup", isGroup);
            response.put("targetId", targetId);
            response.put("messages", messages);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição getMessages", e);
            sendErrorMessage(conn, "server_error", "Erro ao buscar mensagens");
        }
    }
    
    /**
     * Processa requisição para enviar mensagem.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleSendMessage(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            String senderId = (String) request.get("senderId");
            String targetId = (String) request.get("targetId");
            String content = (String) request.get("content");
            Boolean isGroup = (Boolean) request.get("isGroup");
            
            if (senderId == null || targetId == null || content == null || isGroup == null) {
                sendErrorMessage(conn, "missing_parameter", "Parâmetros obrigatórios não especificados");
                return;
            }
            
            Message message;
            
            if (isGroup) {
                // Enviar mensagem para grupo
                message = server.getMessageService().sendGroupMessage(senderId, targetId, content);
                
                if (message != null) {
                    // Notificar todos os membros do grupo
                    List<GroupMember> members = server.getGroupMemberDAO().findByGroupId(targetId);
                    for (GroupMember member : members) {
                        if (!member.getUserId().equals(senderId)) {
                            WebSocket memberConn = server.getConnectionByUserId(member.getUserId());
                            if (memberConn != null && memberConn.isOpen()) {
                                Map<String, Object> notification = new HashMap<>();
                                notification.put("type", "new_group_message");
                                notification.put("message", message);
                                sendJsonMessage(memberConn, notification);
                            }
                        }
                    }
                }
            } else {
                // Enviar mensagem privada
                message = server.getMessageService().sendPrivateMessage(senderId, targetId, content);
                
                if (message != null) {
                    // Notificar destinatário
                    WebSocket receiverConn = server.getConnectionByUserId(targetId);
                    if (receiverConn != null && receiverConn.isOpen()) {
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("type", "new_private_message");
                        notification.put("message", message);
                        sendJsonMessage(receiverConn, notification);
                    }
                }
            }
            
            if (message == null) {
                sendErrorMessage(conn, "message_send_failed", "Falha ao enviar mensagem");
                return;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "message_sent");
            response.put("message", message);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição sendMessage", e);
            sendErrorMessage(conn, "server_error", "Erro ao enviar mensagem");
        }
    }
    
    /**
     * Processa requisição para criar grupo.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleCreateGroup(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String adminId = (String) request.get("adminId");
            Boolean deleteOnAdminExit = (Boolean) request.get("deleteOnAdminExit");
            
            if (name == null || adminId == null) {
                sendErrorMessage(conn, "missing_parameter", "Parâmetros obrigatórios não especificados");
                return;
            }
            
            if (description == null) description = "";
            if (deleteOnAdminExit == null) deleteOnAdminExit = false;
            
            Group group = server.getGroupService().createGroup(name, description, adminId, deleteOnAdminExit);
            
            if (group == null) {
                sendErrorMessage(conn, "group_creation_failed", "Falha ao criar grupo");
                return;
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "group_created");
            response.put("group", group);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição createGroup", e);
            sendErrorMessage(conn, "server_error", "Erro ao criar grupo");
        }
    }
    
    /**
     * Processa requisição para adicionar usuário a um grupo.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleAddUserToGroup(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            String groupId = (String) request.get("groupId");
            String userId = (String) request.get("userId");
            String adminId = (String) request.get("adminId");
            
            if (groupId == null || userId == null || adminId == null) {
                sendErrorMessage(conn, "missing_parameter", "Parâmetros obrigatórios não especificados");
                return;
            }
            
            boolean success = server.getGroupService().addUserToGroup(groupId, userId, adminId);
            
            if (!success) {
                sendErrorMessage(conn, "add_user_failed", "Falha ao adicionar usuário ao grupo");
                return;
            }
            
            // Notificar o usuário adicionado
            WebSocket userConn = server.getConnectionByUserId(userId);
            if (userConn != null && userConn.isOpen()) {
                Group group = server.getGroupService().getGroup(groupId);
                if (group != null) {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "added_to_group");
                    notification.put("group", group);
                    sendJsonMessage(userConn, notification);
                }
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "user_added_to_group");
            response.put("groupId", groupId);
            response.put("userId", userId);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição addUserToGroup", e);
            sendErrorMessage(conn, "server_error", "Erro ao adicionar usuário ao grupo");
        }
    }
    
    /**
     * Processa requisição para remover usuário de um grupo.
     *
     * @param server Servidor WebSocket
     * @param conn Conexão WebSocket
     * @param request Requisição
     */
    public void handleRemoveUserFromGroup(WhatsUTWebSocketServer server, WebSocket conn, Map<String, Object> request) {
        try {
            String groupId = (String) request.get("groupId");
            String userId = (String) request.get("userId");
            String adminId = (String) request.get("adminId");
            
            if (groupId == null || userId == null || adminId == null) {
                sendErrorMessage(conn, "missing_parameter", "Parâmetros obrigatórios não especificados");
                return;
            }
            
            boolean success = server.getGroupService().removeUserFromGroup(groupId, userId, adminId);
            
            if (!success) {
                sendErrorMessage(conn, "remove_user_failed", "Falha ao remover usuário do grupo");
                return;
            }
            
            // Notificar o usuário removido
            WebSocket userConn = server.getConnectionByUserId(userId);
            if (userConn != null && userConn.isOpen()) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "removed_from_group");
                notification.put("groupId", groupId);
                sendJsonMessage(userConn, notification);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("type", "user_removed_from_group");
            response.put("groupId", groupId);
            response.put("userId", userId);
            sendJsonMessage(conn, response);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar requisição removeUserFromGroup", e);
            sendErrorMessage(conn, "server_error", "Erro ao remover usuário do grupo");
        }
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
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao serializar objeto para JSON", e);
        }
    }
}
