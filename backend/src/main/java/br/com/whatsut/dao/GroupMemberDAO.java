package br.com.whatsut.dao;

import br.com.whatsut.model.GroupMember;
import br.com.whatsut.util.ConfigManager;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * DAO para operações de persistência de membros de grupo.
 */
public class GroupMemberDAO extends AbstractJsonDAO<GroupMember, String> {
    private static final Logger logger = Logger.getLogger(GroupMemberDAO.class.getName());
    
    // Cache para melhorar performance e evitar loops infinitos de requisições
    private final Map<String, List<GroupMember>> groupMembersCache;
    private final int maxCacheSize;
    
    public GroupMemberDAO() {
        super(ConfigManager.getProperty("storage.group_members.file", "group_members.json"));
        this.maxCacheSize = ConfigManager.getIntProperty("cache.group_members.max.size", 200);
        this.groupMembersCache = new ConcurrentHashMap<>();
        
        logger.info("GroupMemberDAO inicializado");
    }
    
    @Override
    protected String getId(GroupMember entity) {
        return entity.getGroupId() + ":" + entity.getUserId();
    }
    
    @Override
    protected TypeReference<List<GroupMember>> getTypeReference() {
        return new TypeReference<List<GroupMember>>() {};
    }
    
    /**
     * Busca um membro específico de um grupo.
     *
     * @param groupId ID do grupo
     * @param userId ID do usuário
     * @return O membro do grupo ou null se não existir
     */
    public GroupMember findGroupMember(String groupId, String userId) {
        String id = groupId + ":" + userId;
        
        // Verificar no cache primeiro
        List<GroupMember> cachedMembers = groupMembersCache.get(groupId);
        if (cachedMembers != null) {
            for (GroupMember member : cachedMembers) {
                if (member.getUserId().equals(userId)) {
                    return member;
                }
            }
        }
        
        // Se não estiver no cache, buscar no arquivo
        return super.findById(id);
    }
    
    /**
     * Lista todos os membros de um grupo.
     *
     * @param groupId ID do grupo
     * @return Lista de membros do grupo
     */
    public List<GroupMember> findByGroupId(String groupId) {
        // Verificar no cache primeiro
        List<GroupMember> cachedMembers = groupMembersCache.get(groupId);
        if (cachedMembers != null) {
            return cachedMembers;
        }
        
        // Se não estiver no cache, buscar no arquivo
        List<GroupMember> members = super.findAll().stream()
                .filter(m -> m.getGroupId().equals(groupId))
                .collect(Collectors.toList());
        
        // Adicionar ao cache se não exceder o limite
        if (groupMembersCache.size() < maxCacheSize) {
            groupMembersCache.put(groupId, members);
        }
        
        return members;
    }
    
    /**
     * Lista todos os grupos de um usuário.
     *
     * @param userId ID do usuário
     * @return Lista de membros de grupo do usuário
     */
    public List<GroupMember> findByUserId(String userId) {
        // Buscar diretamente no arquivo, pois o cache é organizado por grupo
        return super.findAll().stream()
                .filter(m -> m.getUserId().equals(userId))
                .collect(Collectors.toList());
    }
    
    /**
     * Busca um membro específico de um grupo pelo ID do grupo e do usuário.
     * Este método é uma alternativa ao findGroupMember com nome mais claro.
     *
     * @param groupId ID do grupo
     * @param userId ID do usuário
     * @return O membro do grupo ou null se não existir
     */
    public GroupMember findByGroupAndUser(String groupId, String userId) {
        return findGroupMember(groupId, userId);
    }
    
    @Override
    public boolean save(GroupMember entity) {
        // Atualizar no cache
        String groupId = entity.getGroupId();
        List<GroupMember> cachedMembers = groupMembersCache.get(groupId);
        if (cachedMembers != null) {
            // Remover membro existente com mesmo ID
            cachedMembers.removeIf(m -> m.getUserId().equals(entity.getUserId()));
            // Adicionar novo membro
            cachedMembers.add(entity);
        }
        
        // Salvar no arquivo
        return super.save(entity);
    }
    
    @Override
    public boolean delete(String id) {
        // Extrair groupId e userId do id composto
        String[] parts = id.split(":");
        if (parts.length != 2) {
            logger.warning("ID inválido para GroupMember: " + id);
            return false;
        }
        
        String groupId = parts[0];
        String userId = parts[1];
        
        // Atualizar cache
        List<GroupMember> cachedMembers = groupMembersCache.get(groupId);
        if (cachedMembers != null) {
            cachedMembers.removeIf(m -> m.getUserId().equals(userId));
        }
        
        // Remover do arquivo
        return super.delete(id);
    }
    
    /**
     * Remove um membro de um grupo.
     *
     * @param groupId ID do grupo
     * @param userId ID do usuário
     * @return true se removido com sucesso, false caso contrário
     */
    public boolean deleteGroupMember(String groupId, String userId) {
        return delete(groupId + ":" + userId);
    }
    
    /**
     * Remove todos os membros de um grupo.
     *
     * @param groupId ID do grupo
     * @return true se removidos com sucesso, false caso contrário
     */
    public boolean deleteAllGroupMembers(String groupId) {
        // Remover do cache
        groupMembersCache.remove(groupId);
        
        // Remover do arquivo
        List<GroupMember> allMembers = super.findAll();
        List<GroupMember> remainingMembers = allMembers.stream()
                .filter(m -> !m.getGroupId().equals(groupId))
                .collect(Collectors.toList());
        
        return super.saveAll(remainingMembers);
    }
    
    /**
     * Limpa o cache de membros de grupo.
     */
    public void clearCache() {
        groupMembersCache.clear();
        logger.info("Cache de membros de grupo limpo");
    }
    
    /**
     * Limpa o cache de membros de um grupo específico.
     *
     * @param groupId ID do grupo
     */
    public void clearGroupCache(String groupId) {
        groupMembersCache.remove(groupId);
        logger.info("Cache de membros do grupo " + groupId + " limpo");
    }
    
    /**
     * Salva uma solicitação de entrada em grupo.
     * Na implementação atual, usamos o mesmo modelo GroupMember para solicitações,
     * mas com um campo joinedAt = 0 para indicar que é uma solicitação pendente.
     *
     * @param request Solicitação de entrada em grupo
     * @return true se salvo com sucesso, false caso contrário
     */
    public boolean saveJoinRequest(GroupMember request) {
        // Definir joinedAt como 0 para indicar que é uma solicitação pendente
        request.setJoinedAt(0);
        return save(request);
    }
    
    /**
     * Busca uma solicitação de entrada em grupo.
     *
     * @param groupId ID do grupo
     * @param userId ID do usuário solicitante
     * @return A solicitação encontrada ou null se não existir
     */
    public GroupMember findJoinRequest(String groupId, String userId) {
        GroupMember member = findByGroupAndUser(groupId, userId);
        // Verificar se é uma solicitação pendente (joinedAt = 0)
        if (member != null && member.getJoinedAt() == 0) {
            return member;
        }
        return null;
    }
    
    /**
     * Remove uma solicitação de entrada em grupo.
     *
     * @param memberId ID do membro/solicitação
     * @return true se removido com sucesso, false caso contrário
     */
    public boolean deleteJoinRequest(String memberId) {
        return delete(memberId);
    }
}
