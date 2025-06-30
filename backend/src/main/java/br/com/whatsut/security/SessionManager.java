package br.com.whatsut.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    
    public static String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, userId);
        return sessionId;
    }
    
    public static String getUserIdFromSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    public static boolean isValidSession(String sessionId) {
        return sessionId != null && activeSessions.containsKey(sessionId);
    }
    
    public static void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
    }
}
