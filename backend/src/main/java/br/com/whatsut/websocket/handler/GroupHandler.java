package br.com.whatsut.websocket.handler;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.java_websocket.WebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.whatsut.dao.PendingJoinRequestDAO;
import br.com.whatsut.model.User;
import br.com.whatsut.security.SessionManager;
import br.com.whatsut.service.GroupService;
import br.com.whatsut.service.UserService;

/**
 * Processa requisições WebSocket relacionadas a grupos.
 */
public class GroupHandler implements RequestHandler {

    // Mapa de sessões WebSocket -> sessionId
    private final ConcurrentMap<WebSocket, String> sessions;
    // Serviço de grupos
    private final GroupService groupService;
    // DAO para requisições pendentes de entrada em grupo
    private final PendingJoinRequestDAO pendingJoinRequestDAO = new PendingJoinRequestDAO();
    // Serviço de usuários
    private UserService userService;
    // ObjectMapper para serialização de notificações
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Construtor do handler de grupos
     * @param sessions Mapa de sessões WebSocket
     * @param groupService Serviço de grupos
     * @param userService Serviço de usuários
     */
    public GroupHandler(ConcurrentMap<WebSocket, String> sessions, GroupService groupService, UserService userService) {
        this.sessions = sessions;
        this.groupService = groupService;
        this.userService = userService;
    }

    /**
     * Processa o tipo de requisição de grupo recebido
     */
    @Override
    public void handle(String type, WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        switch (type) {
            case "getGroups":
                handleGetGroups(conn, response);
                break;
            case "getAllAvailableGroups":
                handleGetAllAvailableGroups(conn, response);
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
            case "sendGroupFile":
                handleSendGroupFile(conn, data, response);
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
            case "requestJoinGroup":
                handleRequestJoinGroup(conn, data, response);
                break;
            case "respondJoinGroup":
                handleRespondJoinGroup(conn, data, response);
                break;
            case "removeJoinRequestNotification":
                handleRemoveJoinRequestNotification(conn, data, response);
                break;
            default:
                response.put("success", false);
                response.put("error", "Tipo de requisição de grupos desconhecido: " + type);
        }
    }

    /**
     * Valida se a sessão está autenticada
     */
    private boolean validateSession(WebSocket conn, Map<String, Object> response) {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return false;
        }
        return true;
    }

    /**
     * Obtém o sessionId da conexão
     */
    private String getSessionId(WebSocket conn) {
        return sessions.get(conn);
    }

    /**
     * Obtém todos os grupos do usuário logado
     */
    private void handleGetGroups(WebSocket conn, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        List<Map<String, Object>> groups = groupService.getGroups(getSessionId(conn));
        response.put("success", true);
        response.put("data", groups);
    }

    /**
     * Obtém todos os grupos disponíveis para o usuário
     */
    private void handleGetAllAvailableGroups(WebSocket conn, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        List<Map<String, Object>> groups = groupService.getAllAvailableGroups(getSessionId(conn));
        response.put("success", true);
        response.put("data", groups);
    }

    /**
     * Cria um novo grupo
     */
    private void handleCreateGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        // Aceitar tanto "groupName" (padrão antigo) quanto "name" (usado pelo frontend)
        String groupName = (String) data.get("groupName");
        if (groupName == null || groupName.isBlank()) {
            groupName = (String) data.get("name");
        }
        if (groupName == null || groupName.isBlank()) {
            response.put("success", false);
            response.put("error", "Nome do grupo não fornecido");
            return;
        }
        // Obter lista de membros opcional enviada pelo frontend
        List<String> members = null;
        Object rawMembers = data.get("members");
        if (rawMembers instanceof List) {
            // suprimir warning com cast seguro generics
            members = ((List<?>) rawMembers).stream()
                    .filter(o -> o instanceof String)
                    .map(o -> (String) o)
                    .collect(Collectors.toList());
        }
        if (members == null) members = List.of();
        // Chamar serviço incluindo membros
        Map<String, Object> group = groupService.createGroup(getSessionId(conn), groupName, members);
        response.put("success", true);
        response.put("data", group);
    }

    /**
     * Obtém as mensagens de um grupo
     */
    private void handleGetGroupMessages(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        String groupId = (String) data.get("groupId");
        if (groupId == null) {
            response.put("success", false);
            response.put("error", "ID do grupo não fornecido");
            return;
        }
        List<Map<String, Object>> msgs = groupService.getGroupMessages(getSessionId(conn), groupId);
        response.put("success", true);
        response.put("data", msgs);
    }

    /**
     * Envia uma mensagem para o grupo
     */
    private void handleSendGroupMessage(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        String groupId = (String) data.get("groupId");
        String content = (String) data.get("content");
        if (groupId == null || content == null) {
            response.put("success", false);
            response.put("error", "Dados da mensagem não fornecidos");
            return;
        }
        Map<String, Object> result = groupService.sendGroupMessage(getSessionId(conn), groupId, content);
        response.put("success", true);
        response.put("data", result);
    }

    /**
     * Envia um arquivo para o grupo
     */
    private void handleSendGroupFile(WebSocket conn, Map<String,Object> data, Map<String,Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        String groupId = (String) data.get("groupId");
        String fileName = (String) data.get("fileName");
        String fileType = (String) data.get("fileType");
        String fileDataB64 = (String) data.get("fileData");
        if (groupId == null || fileName == null || fileDataB64 == null) {
            response.put("success", false);
            response.put("error", "Parâmetros faltando");
            return;
        }
        byte[] bytes = java.util.Base64.getDecoder().decode(fileDataB64);
        try {
            Map<String,Object> msg = groupService.sendGroupFile(sessionId, groupId, fileName, fileType, bytes);
            response.put("success", true);
            response.put("data", msg);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao enviar arquivo: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Obtém os membros de um grupo
     */
    private void handleGetGroupMembers(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        String groupId = (String) data.get("groupId");
        if (groupId == null) {
            response.put("success", false);
            response.put("error", "ID do grupo não fornecido");
            return;
        }
        List<Map<String, Object>> members = groupService.getGroupMembers(getSessionId(conn), groupId);
        response.put("success", true);
        response.put("data", members);
    }

    /**
     * Adiciona um usuário ao grupo
     */
    private void handleAddUserToGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        String groupId = (String) data.get("groupId");
        String userId = (String) data.get("userId");
        if (groupId == null || userId == null) {
            response.put("success", false);
            response.put("error", "Dados incompletos");
            return;
        }
        groupService.addUserToGroup(getSessionId(conn), groupId, userId);
        response.put("success", true);
    }

    /**
     * Remove um usuário do grupo
     */
    private void handleRemoveUserFromGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        String groupId = (String) data.get("groupId");
        String userId = (String) data.get("userId");
        if (userId == null) {
            userId = (String) data.get("userIdToRemove");
        }
        if (groupId == null || userId == null) {
            response.put("success", false);
            response.put("error", "Dados incompletos");
            return;
        }
        groupService.removeUserFromGroup(getSessionId(conn), groupId, userId);
        response.put("success", true);
    }

    /**
     * Define um novo admin para o grupo
     */
    private void handleSetGroupAdmin(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        String groupId = (String) data.get("groupId");
        String userId = (String) data.get("userId");
        if (groupId == null || userId == null) {
            response.put("success", false);
            response.put("error", "Dados incompletos");
            return;
        }
        // API atual não possui suporte a alterar admin com flag. Delegando sem booleano.
        groupService.setGroupAdmin(getSessionId(conn), groupId, userId);
        response.put("success", true);
    }

    /**
     * Permite que o usuário saia do grupo
     */
    private void handleLeaveGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        if (!validateSession(conn, response)) return;
        String groupId = (String) data.get("groupId");
        if (groupId == null) {
            response.put("success", false);
            response.put("error", "ID do grupo não fornecido");
            return;
        }
        groupService.leaveGroup(getSessionId(conn), groupId);
        response.put("success", true);
    }

    /**
     * Exclui um grupo
     */
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

    /**
     * Processa uma solicitação para entrar em um grupo
     * @param conn Conexão WebSocket
     * @param data Dados da requisição
     * @param response Resposta a ser enviada
     * @throws Exception Em caso de erro
     */
    private void handleRequestJoinGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
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

        // Obtenha o userId a partir da sessão
        String userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            response.put("success", false);
            response.put("error", "Sessão inválida");
            return;
        }
        
        // Obtenha o usuário usando o userId correto
        User user = userService.getUserById(sessionId, userId);
        if (user == null) {
            response.put("success", false);
            response.put("error", "Usuário não encontrado");
            return;
        }
        
        // Buscar todos os grupos disponíveis para o usuário
        List<Map<String, Object>> availableGroups = groupService.getAllAvailableGroups(sessionId);
        
        // Encontrar o grupo pelo ID entre os disponíveis
        Map<String, Object> groupMap = availableGroups.stream()
            .filter(g -> groupId.equals(g.get("groupId")))
            .findFirst().orElse(null);
            
        if (groupMap == null) {
            response.put("success", false);
            response.put("error", "Grupo não encontrado ou você já é membro");
            return;
        }
        
        String adminId = (String) groupMap.getOrDefault("adminId", groupMap.get("createdBy"));

        // Verifique se o admin está online usando utilitário
        WebSocket adminConn = findConnectionByUserId(adminId);

        PendingJoinRequestDAO.JoinRequest req = new PendingJoinRequestDAO.JoinRequest(
            (String) groupMap.get("groupId"), (String) groupMap.get("name"), user.getUserId(), user.getDisplayName()
        );

        if (adminConn != null) {
        // Admin online: envie notificação
        Map<String, Object> notify = new HashMap<>();
        notify.put("type", "joinGroupRequest");
        notify.put("groupId", (String) groupMap.get("groupId"));
        notify.put("groupName", (String) groupMap.get("name"));
        notify.put("userId", user.getUserId());
        notify.put("userName", user.getDisplayName());
        
        // Log detalhado antes de enviar a notificação
        System.out.println("[WhatsUTWebSocketServer] Enviando notificação de solicitação de grupo para admin " + adminId);
        System.out.println("[WhatsUTWebSocketServer] Detalhes da notificação: " + notify);
        
        try {
            String notifyJson = objectMapper.writeValueAsString(notify);
            System.out.println("[WhatsUTWebSocketServer] JSON da notificação: " + notifyJson);
            adminConn.send(notifyJson);
            System.out.println("[WhatsUTWebSocketServer] Notificação enviada com sucesso!");
        } catch (Exception e) {
            System.err.println("[WhatsUTWebSocketServer] Erro ao enviar notificação: " + e.getMessage());
            e.printStackTrace();
        }
    } else {
        // Admin offline: salve o pedido
        System.out.println("[WhatsUTWebSocketServer] Admin " + adminId + " está offline. Salvando solicitação para entrega posterior.");
        pendingJoinRequestDAO.addRequest(adminId, req);
    }

        response.put("success", true);
        response.put("message", "Pedido enviado ao admin do grupo.");
    }

    /**
     * Processa a resposta a uma solicitação de entrada em grupo
     * @param conn Conexão WebSocket
     * @param data Dados da requisição
     * @param response Resposta a ser enviada
     * @throws Exception Em caso de erro
     */
    private void handleRespondJoinGroup(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        
        String groupId = (String) data.get("groupId");
        String userId = (String) data.get("userId");
        boolean accept = (boolean) data.get("accept");
        
        if (groupId == null || userId == null) {
            response.put("success", false);
            response.put("error", "Parâmetros incompletos");
            return;
        }
        
        // Obtenha o adminId a partir da sessão
        String adminId = SessionManager.getUserIdFromSession(sessionId);
        if (adminId == null) {
            response.put("success", false);
            response.put("error", "Sessão inválida");
            return;
        }
        
        // Obter todos os grupos do usuário
        List<Map<String, Object>> userGroups = groupService.getGroups(sessionId);
        
        // Encontrar o grupo pelo ID
        Map<String, Object> groupMap = userGroups.stream()
            .filter(g -> groupId.equals(g.get("groupId")))
            .findFirst().orElse(null);
            
        if (groupMap == null) {
            response.put("success", false);
            response.put("error", "Grupo não encontrado ou você não é membro");
            return;
        }
        
        // Verificar se o usuário logado é o admin do grupo
        String groupAdminId = (String) groupMap.get("adminId");
        if (!groupAdminId.equals(adminId)) {
            response.put("success", false);
            response.put("error", "Apenas o admin pode aceitar solicitações");
            return;
        }
        
        // Verificar se o usuário solicitante existe
        User requestUser = userService.getUserById(sessionId, userId);
        if (requestUser == null) {
            response.put("success", false);
            response.put("error", "Usuário solicitante não encontrado");
            return;
        }
        
        if (accept) {
            try {
                // Adicionar o usuário ao grupo
                groupService.addUserToGroup(sessionId, groupId, userId);
                
                // Notificar o usuário que foi aceito no grupo, se estiver online
                WebSocket userConn = findConnectionByUserId(userId);
                System.out.println("[handleRespondJoinGroup] Tentando notificar usuário " + userId + " sobre aceitação. Conexão encontrada: " + (userConn != null));
                
                if (userConn != null) {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type", "joinGroupAccepted");
                    notification.put("groupId", groupId);
                    notification.put("groupName", (String) groupMap.get("name"));
                    
                    String notificationJson = objectMapper.writeValueAsString(notification);
                    System.out.println("[handleRespondJoinGroup] Enviando notificação de aceitação: " + notificationJson);
                    
                    userConn.send(notificationJson);
                    System.out.println("[handleRespondJoinGroup] Notificação de aceitação enviada com sucesso");
                }
                
                response.put("success", true);
                response.put("message", "Usuário adicionado ao grupo com sucesso");
                // Remove pending join request since it's handled
                pendingJoinRequestDAO.removeRequest(adminId, groupId, userId);
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().toLowerCase().contains("já é membro")) {
                    // Treat as success if the user is already in the group
                    pendingJoinRequestDAO.removeRequest(adminId, groupId, userId);
                    response.put("success", true);
                    response.put("message", "Usuário já era membro do grupo");
                } else {
                    response.put("success", false);
                    response.put("error", "Erro ao adicionar usuário ao grupo: " + e.getMessage());
                }
            }
        } else {
            // Notificar o usuário que foi rejeitado, se estiver online
            WebSocket userConn = findConnectionByUserId(userId);
            System.out.println("[handleRespondJoinGroup] Tentando notificar usuário " + userId + " sobre rejeição. Conexão encontrada: " + (userConn != null));
            
            if (userConn != null) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("type", "joinGroupRejected");
                notification.put("groupId", groupId);
                notification.put("groupName", (String) groupMap.get("name"));
                
                String notificationJson = objectMapper.writeValueAsString(notification);
                System.out.println("[handleRespondJoinGroup] Enviando notificação de rejeição: " + notificationJson);
                
                userConn.send(notificationJson);
                System.out.println("[handleRespondJoinGroup] Notificação de rejeição enviada com sucesso");
            }
            
            response.put("success", true);
            response.put("message", "Solicitação rejeitada com sucesso");
            // Remove pending join request after rejection
            pendingJoinRequestDAO.removeRequest(adminId, groupId, userId);
        }
    }

    /**
     * Remove notificação de solicitação de entrada em grupo
     */
    private void handleRemoveJoinRequestNotification(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        String adminId = SessionManager.getUserIdFromSession(sessionId);
        String groupId = (String) data.get("groupId");
        String userId = (String) data.get("userId");
        if (adminId == null || groupId == null || userId == null) {
            response.put("success", false);
            response.put("error", "Parâmetros obrigatórios não fornecidos");
            return;
        }
        boolean removed = pendingJoinRequestDAO.removeRequest(adminId, groupId, userId);
        response.put("success", removed);
        if (!removed) {
            response.put("error", "Notificação não encontrada ou já removida");
        }
    }

    /**
     * Encontra a conexão WebSocket de um usuário pelo seu ID
     * @param userId ID do usuário
     * @return Conexão WebSocket ou null se não encontrada
     */
    private WebSocket findConnectionByUserId(String userId) {
        for (Map.Entry<WebSocket, String> entry : sessions.entrySet()) {
            String sessionId = entry.getValue();
            if (sessionId != null) {
                String sessionUserId = SessionManager.getUserIdFromSession(sessionId);
                if (userId.equals(sessionUserId)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
