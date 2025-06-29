package br.com.whatsut.dao;

import br.com.whatsut.model.Session;
import br.com.whatsut.util.ConfigManager;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * DAO para operações de persistência de sessões.
 */
public class SessionDAO extends AbstractJsonDAO<Session, String> {
    private static final Logger logger = Logger.getLogger(SessionDAO.class.getName());
    
    // Cache para melhorar performance
    private final ConcurrentHashMap<String, Session> sessionCache;
    
    public SessionDAO() {
        super(ConfigManager.getProperty("storage.sessions.file", "sessions.json"));
        this.sessionCache = new ConcurrentHashMap<>();
        
        // Pré-carregar sessões válidas no cache
        List<Session> sessions = super.findAll();
        for (Session session : sessions) {
            if (session.isValid()) {
                sessionCache.put(session.getSessionId(), session);
            }
        }
        
        logger.info("SessionDAO inicializado com " + sessionCache.size() + " sessões em cache");
    }
    
    @Override
    protected String getId(Session entity) {
        return entity.getSessionId();
    }
    
    @Override
    protected TypeReference<List<Session>> getTypeReference() {
        return new TypeReference<List<Session>>() {};
    }
    
    @Override
    public Session findById(String id) {
        // Verificar no cache primeiro
        Session cachedSession = sessionCache.get(id);
        if (cachedSession != null) {
            // Verificar se a sessão ainda é válida
            if (!cachedSession.isValid()) {
                sessionCache.remove(id);
                super.delete(id);
                return null;
            }
            return cachedSession;
        }
        
        // Se não estiver no cache, buscar no arquivo
        Session session = super.findById(id);
        
        // Verificar se a sessão é válida
        if (session != null) {
            if (!session.isValid()) {
                super.delete(id);
                return null;
            }
            
            // Adicionar ao cache
            sessionCache.put(id, session);
        }
        
        return session;
    }
    
    /**
     * Busca sessões ativas de um usuário.
     *
     * @param userId ID do usuário
     * @return Lista de sessões ativas
     */
    public List<Session> findByUserId(String userId) {
        // Verificar no cache primeiro
        List<Session> cachedSessions = sessionCache.values().stream()
                .filter(s -> s.getUserId().equals(userId) && s.isValid())
                .collect(Collectors.toList());
        
        if (!cachedSessions.isEmpty()) {
            return cachedSessions;
        }
        
        // Se não estiver no cache, buscar no arquivo
        List<Session> sessions = super.findAll().stream()
                .filter(s -> s.getUserId().equals(userId) && s.isValid())
                .collect(Collectors.toList());
        
        // Adicionar ao cache
        for (Session session : sessions) {
            sessionCache.put(session.getSessionId(), session);
        }
        
        return sessions;
    }
    
    @Override
    public boolean save(Session entity) {
        // Atualizar no cache
        sessionCache.put(entity.getSessionId(), entity);
        
        // Salvar no arquivo
        return super.save(entity);
    }
    
    @Override
    public boolean delete(String id) {
        // Remover do cache
        sessionCache.remove(id);
        
        // Remover do arquivo
        return super.delete(id);
    }
    
    /**
     * Remove sessões expiradas.
     */
    public void cleanExpiredSessions() {
        // Remover do cache
        sessionCache.entrySet().removeIf(entry -> !entry.getValue().isValid());
        
        // Remover do arquivo
        List<Session> sessions = super.findAll();
        List<Session> validSessions = sessions.stream()
                .filter(Session::isValid)
                .collect(Collectors.toList());
        
        if (validSessions.size() < sessions.size()) {
            super.saveAll(validSessions);
            logger.info("Sessões expiradas removidas");
        }
    }
}
