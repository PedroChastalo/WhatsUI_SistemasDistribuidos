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
 * Handles user-related requests (getUsers, getUser, updateStatus).
 */
public class UserHandler implements RequestHandler {

    private final ConcurrentMap<WebSocket, String> sessions;
    private final UserService userService;
    private final Function<String, Boolean> isUserOnline;

    public UserHandler(ConcurrentMap<WebSocket, String> sessions, UserService userService,
                       Function<String, Boolean> isUserOnline) {
        this.sessions = sessions;
        this.userService = userService;
        this.isUserOnline = isUserOnline;
    }

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
