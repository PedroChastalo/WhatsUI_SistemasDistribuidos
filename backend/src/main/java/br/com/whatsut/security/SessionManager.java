package br.com.whatsut.security;

import br.com.whatsut.util.DataPersistenceUtil;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final String SESSIONS_FILE = "sessions";
    private static Map<String, String> activeSessions;
    
    static {
        loadSessions();
    }
    
    /**
     * Carrega as sessões ativas do arquivo JSON
     */
    private static void loadSessions() {
        TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
        activeSessions = DataPersistenceUtil.loadData(SESSIONS_FILE, typeRef, new ConcurrentHashMap<>());
        System.out.println("Sessões carregadas: " + activeSessions.size() + " sessões ativas");
    }
    
    /**
     * Salva as sessões ativas em arquivo JSON
     */
    private static void saveSessions() {
        DataPersistenceUtil.saveData(SESSIONS_FILE, activeSessions);
    }
    
    public static String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, userId);
        saveSessions();
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
        saveSessions();
    }
}
