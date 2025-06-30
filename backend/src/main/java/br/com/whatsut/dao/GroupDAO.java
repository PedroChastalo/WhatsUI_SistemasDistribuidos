package br.com.whatsut.dao;

import br.com.whatsut.model.Group;
import br.com.whatsut.util.DataPersistenceUtil;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * DAO para gerenciar grupos com persistência em JSON
 */
public class GroupDAO {
    private static final String GROUPS_FILE = "groups";
    
    // Armazenar grupos: chave é groupId
    private static Map<String, Group> groups;
    
    public GroupDAO() {
        loadData();
    }
    
    /**
     * Carrega os dados dos arquivos JSON
     */
    private void loadData() {
        TypeReference<Map<String, Group>> typeRef = new TypeReference<Map<String, Group>>() {};
        groups = DataPersistenceUtil.loadData(GROUPS_FILE, typeRef, new ConcurrentHashMap<>());
        System.out.println("Dados de grupos carregados: " + groups.size() + " grupos");
    }
    
    /**
     * Salva os dados em arquivos JSON
     */
    public void saveData() {
        DataPersistenceUtil.saveData(GROUPS_FILE, groups);
    }
    
    /**
     * Cria um novo grupo
     * @param name Nome do grupo
     * @param adminId ID do administrador
     * @param members Lista de IDs dos membros iniciais
     * @return O grupo criado
     */
    public Group createGroup(String name, String adminId, List<String> members) {
        Group group = new Group(name, adminId);
        
        // Adicionar membros
        if (members != null) {
            for (String memberId : members) {
                group.addMember(memberId);
            }
        }
        
        groups.put(group.getGroupId(), group);
        
        // Persistir dados
        saveData();
        
        return group;
    }
    
    /**
     * Obtém um grupo pelo ID
     * @param groupId ID do grupo
     * @return O grupo encontrado ou null
     */
    public Group getGroupById(String groupId) {
        return groups.get(groupId);
    }
    
    /**
     * Obtém todos os grupos
     * @return Lista de todos os grupos
     */
    public List<Group> getAllGroups() {
        return new ArrayList<>(groups.values());
    }
    
    /**
     * Obtém os grupos de um usuário
     * @param userId ID do usuário
     * @return Lista de grupos dos quais o usuário é membro
     */
    public List<Group> getUserGroups(String userId) {
        return groups.values().stream()
                .filter(group -> group.getMembers().contains(userId))
                .collect(Collectors.toList());
    }
    
    /**
     * Adiciona um usuário a um grupo
     * @param groupId ID do grupo
     * @param userId ID do usuário
     * @return true se adicionado com sucesso
     */
    public boolean addMemberToGroup(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group == null) {
            return false;
        }
        
        group.addMember(userId);
        
        // Persistir dados
        saveData();
        
        return true;
    }
    
    /**
     * Remove um usuário de um grupo
     * @param groupId ID do grupo
     * @param userId ID do usuário
     * @return true se removido com sucesso
     */
    public boolean removeMemberFromGroup(String groupId, String userId) {
        Group group = groups.get(groupId);
        if (group == null) {
            return false;
        }
        
        // Não permitir remover o administrador
        if (userId.equals(group.getAdminId())) {
            return false;
        }
        
        group.removeMember(userId);
        
        // Persistir dados
        saveData();
        
        return true;
    }
    
    /**
     * Define um novo administrador para o grupo
     * @param groupId ID do grupo
     * @param newAdminId ID do novo administrador
     * @return true se alterado com sucesso
     */
    public boolean setGroupAdmin(String groupId, String newAdminId) {
        Group group = groups.get(groupId);
        if (group == null || !group.getMembers().contains(newAdminId)) {
            return false;
        }
        
        group.setAdminId(newAdminId);
        
        // Persistir dados
        saveData();
        
        return true;
    }
    
    /**
     * Exclui um grupo
     * @param groupId ID do grupo
     * @param requesterId ID do usuário que solicitou a exclusão
     * @return true se excluído com sucesso
     */
    public boolean deleteGroup(String groupId, String requesterId) {
        Group group = groups.get(groupId);
        if (group == null || !requesterId.equals(group.getAdminId())) {
            return false;
        }
        
        groups.remove(groupId);
        
        // Persistir dados
        saveData();
        
        return true;
    }
    
    /**
     * Atualiza a última mensagem de um grupo
     * @param groupId ID do grupo
     * @param lastMessage Conteúdo da última mensagem
     * @return true se atualizado com sucesso
     */
    public boolean updateLastMessage(String groupId, String lastMessage) {
        Group group = groups.get(groupId);
        if (group == null) {
            return false;
        }
        
        group.setLastMessage(lastMessage);
        
        // Persistir dados
        saveData();
        
        return true;
    }
}
