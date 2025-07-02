package br.com.whatsut.websocket.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

import org.java_websocket.WebSocket;

import br.com.whatsut.model.User;
import br.com.whatsut.service.UserService;

/**
 * Processa requisições relacionadas a usuários (getUsers, getUser, updateStatus).
 */
public class UserHandler implements RequestHandler {

    // Mapa de sessões WebSocket -> sessionId
    private final ConcurrentMap<WebSocket, String> sessions;
    // Serviço de usuários
    private final UserService userService;
    // Função para verificar se um usuário está online
    private final Function<String, Boolean> isUserOnline;

    /**
     * Construtor do handler de usuários
     * @param sessions Mapa de sessões WebSocket
     * @param userService Serviço de usuários
     * @param isUserOnline Função para verificar se usuário está online
     */
    public UserHandler(ConcurrentMap<WebSocket, String> sessions, UserService userService,
                       Function<String, Boolean> isUserOnline) {
        this.sessions = sessions;
        this.userService = userService;
        this.isUserOnline = isUserOnline;
    }

    /**
     * Processa o tipo de requisição de usuário recebido
     */
    @Override
    public void handle(String type, WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        switch (type) {
            case "getUsers":
                handleGetUsers(conn, response);
                break;
            case "getUser":
                handleGetUser(conn, data, response);
                break;
            case "updateStatus":
                handleUpdateStatus(conn, data, response);
                break;
            default:
                response.put("success", false);
                response.put("error", "Tipo de requisição de usuário desconhecido: " + type);
        }
    }

    /**
     * Obtém todos os usuários (exceto o próprio) e status online
     */
    private void handleGetUsers(WebSocket conn, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        List<User> users = userService.getAllUsers(sessionId);
        List<Map<String, Object>> usersWithStatus = new ArrayList<>();
        for (User user : users) {
            usersWithStatus.add(Map.of(
                    "userId", user.getUserId(),
                    "displayName", user.getDisplayName(),
                    "online", isUserOnline.apply(user.getUserId())
            ));
        }
        response.put("success", true);
        response.put("data", usersWithStatus);
    }

    /**
     * Obtém um usuário pelo ID
     */
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
        User user = userService.getUserById(sessionId, userId);
        if (user == null) {
            response.put("success", false);
            response.put("error", "Usuário não encontrado");
            return;
        }
        response.put("success", true);
        response.put("data", user);
    }

    /**
     * Atualiza o status do usuário logado
     */
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
        boolean success = userService.updateStatus(sessionId, status);
        response.put("success", success);
        if (!success) {
            response.put("error", "Não foi possível atualizar o status");
        }
    }
}
