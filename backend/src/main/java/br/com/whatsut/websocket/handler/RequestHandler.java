package br.com.whatsut.websocket.handler;

import java.util.Map;
import org.java_websocket.WebSocket;

/**
 * Interface genérica para handlers de requisições WebSocket.
 * Cada implementação trata um tipo de requisição (ex: login, mensagens, grupos).
 */
public interface RequestHandler {
    /**
     * Processa uma requisição de determinado tipo.
     *
     * @param type     tipo da requisição (ex: "login", "getUsers")
     * @param conn     conexão WebSocket do cliente
     * @param data     dados recebidos do cliente (pode ser null)
     * @param response mapa de resposta a ser preenchido e enviado ao cliente
     * @throws Exception em caso de erro no processamento
     */
    void handle(String type, WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception;
}
