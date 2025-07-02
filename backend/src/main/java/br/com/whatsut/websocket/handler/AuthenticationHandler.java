package br.com.whatsut.websocket.handler;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import org.java_websocket.WebSocket;
import br.com.whatsut.service.AuthenticationService;


/**
 * Processa solicitações de autenticação: login, cadastro e logout.
 */
public class AuthenticationHandler implements RequestHandler {

    // Mapa de sessões WebSocket -> sessionId
    private final ConcurrentMap<WebSocket, String> sessions;
    // Serviço de autenticação
    private final AuthenticationService authService;
    
    /**
     * Construtor do handler de autenticação
     * @param sessions Mapa de sessões WebSocket
     * @param authService Serviço de autenticação
     */
    public AuthenticationHandler(ConcurrentMap<WebSocket, String> sessions, AuthenticationService authService) {
        this.sessions = sessions;
        this.authService = authService;
    }

    /**
     * Processa o tipo de requisição de autenticação recebido
     */
    @Override
    public void handle(String type, WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        switch (type) {
            case "login":
                handleLogin(conn, data, response);
                break;
            case "register":
                handleRegister(conn, data, response);
                break;
            case "logout":
                handleLogout(conn, response);
                break;
            default:
                response.put("success", false);
                response.put("error", "Tipo de requisição de autenticação desconhecido: " + type);
        }
    }

    /**
     * Processa o login do usuário
     */
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

    /**
     * Processa o registro de um novo usuário
     */
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

    /**
     * Processa o logout do usuário
     */
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
}
