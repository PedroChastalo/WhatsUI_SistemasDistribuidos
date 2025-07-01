package br.com.whatsut.websocket.handler;

import java.util.Map;
import org.java_websocket.WebSocket;

/**
 * Generic handler for a WebSocket request type.
 */
public interface RequestHandler {
    /**
     * Handle a request of a given type.
     *
     * @param type     the request type string (e.g. "login")
     * @param conn     the client connection
     * @param data     the data map received from the client (may be null)
     * @param response response map to be filled and returned to the client
     * @throws Exception in case of any processing error
     */
    void handle(String type, WebSocket conn, Map<String, Object> data, Map<String, Object> response) throws Exception;
}
