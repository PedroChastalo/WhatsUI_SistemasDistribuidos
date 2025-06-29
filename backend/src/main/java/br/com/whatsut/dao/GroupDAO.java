package br.com.whatsut.dao;

import br.com.whatsut.model.Group;
import br.com.whatsut.util.ConfigManager;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * DAO para operações de persistência de grupos.
 */
public class GroupDAO extends AbstractJsonDAO<Group, String> {
    private static final Logger logger = Logger.getLogger(GroupDAO.class.getName());
    
    // Cache para melhorar performance e evitar loops infinitos de requisições
    private final ConcurrentHashMap<String, Group> groupCache;
    private final int maxCacheSize;
    
    public GroupDAO() {
        super(ConfigManager.getProperty("storage.groups.file", "groups.json"));
        this.maxCacheSize = ConfigManager.getIntProperty("cache.groups.max.size", 100);
        this.groupCache = new ConcurrentHashMap<>();
        
        // Pré-carregar grupos no cache
        List<Group> groups = super.findAll();
        for (Group group : groups) {
            if (groupCache.size() >= maxCacheSize) break;
            groupCache.put(group.getGroupId(), group);
        }
        
        logger.info("GroupDAO inicializado com " + groupCache.size() + " grupos em cache");
    }
    
    @Override
    protected String getId(Group entity) {
        return entity.getGroupId();
    }
    
    @Override
    protected TypeReference<List<Group>> getTypeReference() {
        return new TypeReference<List<Group>>() {};
    }
    
    @Override
    public Group findById(String id) {
        // Verificar no cache primeiro
        Group cachedGroup = groupCache.get(id);
        if (cachedGroup != null) {
            return cachedGroup;
        }
        
        // Se não estiver no cache, buscar no arquivo
        Group group = super.findById(id);
        
        // Adicionar ao cache se encontrado
        if (group != null && groupCache.size() < maxCacheSize) {
            groupCache.put(id, group);
        }
        
        return group;
    }
    
    /**
     * Busca grupos por nome (busca parcial, case-insensitive).
     *
     * @param name Nome ou parte do nome do grupo
     * @return Lista de grupos que correspondem à busca
     */
    public List<Group> findByName(String name) {
        String nameLower = name.toLowerCase();
        
        // Verificar no cache primeiro
        List<Group> cachedGroups = groupCache.values().stream()
                .filter(g -> g.getName().toLowerCase().contains(nameLower))
                .collect(Collectors.toList());
        
        if (!cachedGroups.isEmpty()) {
            return cachedGroups;
        }
        
        // Se não estiver no cache, buscar no arquivo
        List<Group> groups = super.findAll().stream()
                .filter(g -> g.getName().toLowerCase().contains(nameLower))
                .collect(Collectors.toList());
        
        // Adicionar ao cache
        for (Group group : groups) {
            if (groupCache.size() >= maxCacheSize) break;
            groupCache.put(group.getGroupId(), group);
        }
        
        return groups;
    }
    
    /**
     * Busca grupos por administrador.
     *
     * @param adminId ID do administrador
     * @return Lista de grupos administrados pelo usuário
     */
    public List<Group> findByAdmin(String adminId) {
        // Verificar no cache primeiro
        List<Group> cachedGroups = groupCache.values().stream()
                .filter(g -> g.getAdminId().equals(adminId))
                .collect(Collectors.toList());
        
        if (!cachedGroups.isEmpty()) {
            return cachedGroups;
        }
        
        // Se não estiver no cache, buscar no arquivo
        List<Group> groups = super.findAll().stream()
                .filter(g -> g.getAdminId().equals(adminId))
                .collect(Collectors.toList());
        
        // Adicionar ao cache
        for (Group group : groups) {
            if (groupCache.size() >= maxCacheSize) break;
            groupCache.put(group.getGroupId(), group);
        }
        
        return groups;
    }
    
    @Override
    public boolean save(Group entity) {
        // Atualizar no cache
        groupCache.put(entity.getGroupId(), entity);
        
        // Salvar no arquivo
        return super.save(entity);
    }
    
    @Override
    public boolean delete(String id) {
        // Remover do cache
        groupCache.remove(id);
        
        // Remover do arquivo
        return super.delete(id);
    }
    
    /**
     * Limpa o cache de grupos.
     */
    public void clearCache() {
        groupCache.clear();
        logger.info("Cache de grupos limpo");
    }
}
