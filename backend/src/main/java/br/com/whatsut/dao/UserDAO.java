package br.com.whatsut.dao;

import br.com.whatsut.model.User;
import br.com.whatsut.util.ConfigManager;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * DAO para operações de persistência de usuários.
 */
public class UserDAO extends AbstractJsonDAO<User, String> {
    private static final Logger logger = Logger.getLogger(UserDAO.class.getName());
    
    // Cache para melhorar performance e evitar loops infinitos de requisições
    private final ConcurrentHashMap<String, User> userCache;
    private final int maxCacheSize;
    
    public UserDAO() {
        super(ConfigManager.getProperty("storage.users.file", "users.json"));
        this.maxCacheSize = ConfigManager.getIntProperty("cache.users.max.size", 500);
        this.userCache = new ConcurrentHashMap<>();
        
        // Pré-carregar usuários no cache
        List<User> users = super.findAll();
        for (User user : users) {
            if (userCache.size() >= maxCacheSize) break;
            userCache.put(user.getUserId(), user);
        }
        
        logger.info("UserDAO inicializado com " + userCache.size() + " usuários em cache");
    }
    
    @Override
    protected String getId(User entity) {
        return entity.getUserId();
    }
    
    @Override
    protected TypeReference<List<User>> getTypeReference() {
        return new TypeReference<List<User>>() {};
    }
    
    @Override
    public User findById(String id) {
        // Verificar no cache primeiro
        User cachedUser = userCache.get(id);
        if (cachedUser != null) {
            return cachedUser;
        }
        
        // Se não estiver no cache, buscar no arquivo
        User user = super.findById(id);
        
        // Adicionar ao cache se encontrado
        if (user != null && userCache.size() < maxCacheSize) {
            userCache.put(id, user);
        }
        
        return user;
    }
    
    /**
     * Busca um usuário pelo nome de usuário.
     *
     * @param username Nome de usuário
     * @return O usuário encontrado ou null se não existir
     */
    public User findByUsername(String username) {
        // Verificar no cache primeiro
        for (User user : userCache.values()) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        
        // Se não estiver no cache, buscar no arquivo
        List<User> users = super.findAll();
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                // Adicionar ao cache se encontrado
                if (userCache.size() < maxCacheSize) {
                    userCache.put(user.getUserId(), user);
                }
                return user;
            }
        }
        
        return null;
    }
    
    @Override
    public boolean save(User entity) {
        // Atualizar no cache
        userCache.put(entity.getUserId(), entity);
        
        // Salvar no arquivo
        return super.save(entity);
    }
    
    @Override
    public boolean delete(String id) {
        // Remover do cache
        userCache.remove(id);
        
        // Remover do arquivo
        return super.delete(id);
    }
    
    /**
     * Limpa o cache de usuários.
     */
    public void clearCache() {
        userCache.clear();
        logger.info("Cache de usuários limpo");
    }
}
