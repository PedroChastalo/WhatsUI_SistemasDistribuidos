package br.com.whatsut.websocket.handler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.java_websocket.WebSocket;

import br.com.whatsut.service.MessageService;

/**
 * Processa solicitações relacionadas a mensagens privadas.
 */
public class MessageHandler implements RequestHandler {

    // Mapa de sessões WebSocket -> sessionId
    private final ConcurrentMap<WebSocket, String> sessions;
    // Serviço de mensagens privadas
    private final MessageService messageService;

    /**
     * Construtor do handler de mensagens privadas
     * @param sessions Mapa de sessões WebSocket
     * @param messageService Serviço de mensagens privadas
     */
    public MessageHandler(ConcurrentMap<WebSocket, String> sessions, MessageService messageService) {
        this.sessions = sessions;
        this.messageService = messageService;
    }

    /**
     * Processa o tipo de requisição de mensagem recebido
     */
    @Override
    public void handle(String type, WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        switch (type) {
            case "getPrivateConversations":
                handleGetPrivateConversations(conn, response);
                break;
            case "getPrivateMessages":
                handleGetPrivateMessages(conn, data, response);
                break;
            case "sendPrivateMessage":
                handleSendPrivateMessage(conn, data, response);
                break;
            case "sendPrivateFile":
                handleSendPrivateFile(conn, data, response);
                break;

            default:
                response.put("success", false);
                response.put("error", "Tipo de requisição de mensagens desconhecido: " + type);
        }
    }

    /**
     * Obtém as conversas privadas do usuário logado
     */
    private void handleGetPrivateConversations(WebSocket conn, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        List<Map<String, Object>> conversations = messageService.getPrivateConversations(sessionId);
        response.put("success", true);
        response.put("data", conversations);
    }

    /**
     * Obtém as mensagens privadas entre o usuário logado e outro usuário
     */
    private void handleGetPrivateMessages(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        String userId = (String) data.getOrDefault("userId", data.get("receiverId"));
        if (userId == null) {
            response.put("success", false);
            response.put("error", "ID do usuário não fornecido");
            return;
        }
        List<Map<String, Object>> messages = messageService.getPrivateMessages(sessionId, userId);
        response.put("success", true);
        response.put("data", messages);
    }

    /**
     * Envia uma mensagem privada
     */
    private void handleSendPrivateMessage(WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        String receiverId = (String) data.getOrDefault("userId", data.get("receiverId"));
        if (receiverId == null) {
            response.put("success", false);
            response.put("error", "ID do destinatário não fornecido");
            return;
        }
        String content = (String) data.get("content");
        if (content == null) {
            response.put("success", false);
            response.put("error", "Conteúdo da mensagem não fornecido");
            return;
        }
        Map<String, Object> message = messageService.sendPrivateMessage(sessionId, receiverId, content);
        response.put("success", true);
        response.put("data", message);
    }

    /**
     * Envia arquivo privado entre usuários.
     */
    private void handleSendPrivateFile(WebSocket conn, Map<String,Object> data, Map<String,Object> response) throws Exception {
        String sessionId = sessions.get(conn);
        if (sessionId == null) {
            response.put("success", false);
            response.put("error", "Usuário não autenticado");
            return;
        }
        String receiverId = (String) data.getOrDefault("receiverId", data.get("userId"));
        String fileName   = (String) data.get("fileName");
        String fileType   = (String) data.get("fileType");
        String fileDataB64 = (String) data.get("fileData");
        if (receiverId == null || fileName == null || fileDataB64 == null) {
            response.put("success", false);
            response.put("error", "Parâmetros faltando");
            return;
        }
        byte[] bytes = java.util.Base64.getDecoder().decode(fileDataB64);
        try {
            Map<String,Object> msg = messageService.sendPrivateFile(sessionId, receiverId, fileName, fileType, bytes);
            response.put("success", true);
            response.put("data", msg);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Erro ao enviar arquivo: " + e.getMessage());
            throw e;
        }
    }
}
