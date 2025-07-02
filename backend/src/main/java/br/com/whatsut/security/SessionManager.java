package br.com.whatsut.security;

import br.com.whatsut.util.DataPersistenceUtil;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador de sessões de usuários
 */
public class SessionManager {
    private static final String SESSIONS_FILE = "sessions";
    private static Map<String, String> activeSessions;
    
    // Bloco estático para carregar sessões ao iniciar
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
    
    /**
     * Cria uma nova sessão para o usuário
     * @param userId ID do usuário
     * @return ID da sessão criada
     */
    public static String createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, userId);
        saveSessions();
        return sessionId;
    }
    
    /**
     * Obtém o ID do usuário associado à sessão
     * @param sessionId ID da sessão
     * @return ID do usuário ou null
     */
    public static String getUserIdFromSession(String sessionId) {
        return activeSessions.get(sessionId);
    }
    
    /**
     * Verifica se a sessão é válida
     * @param sessionId ID da sessão
     * @return true se a sessão for válida
     */
    public static boolean isValidSession(String sessionId) {
        return sessionId != null && activeSessions.containsKey(sessionId);
    }
    
    /**
     * Remove uma sessão ativa
     * @param sessionId ID da sessão
     */
    public static void removeSession(String sessionId) {
        activeSessions.remove(sessionId);
        saveSessions();
    }
}
