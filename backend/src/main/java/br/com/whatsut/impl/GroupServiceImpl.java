package br.com.whatsut.impl;

import br.com.whatsut.dao.GroupDAO;
import br.com.whatsut.dao.GroupMemberDAO;
import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.Group;
import br.com.whatsut.model.GroupMember;
import br.com.whatsut.model.User;
import br.com.whatsut.service.GroupService;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação do serviço de grupos.
 */
public class GroupServiceImpl extends UnicastRemoteObject implements GroupService {
    private static final Logger logger = Logger.getLogger(GroupServiceImpl.class.getName());
    private final GroupDAO groupDAO;
    private final GroupMemberDAO groupMemberDAO;
    private final UserDAO userDAO;
    
    public GroupServiceImpl() throws RemoteException {
        super();
        this.groupDAO = new GroupDAO();
        this.groupMemberDAO = new GroupMemberDAO();
        this.userDAO = new UserDAO();
    }
    
    @Override
    public Group createGroup(String name, String description, String adminId, boolean deleteOnAdminExit) throws RemoteException {
        try {
            User creator = userDAO.findById(adminId);
            if (creator == null) {
                logger.info("Administrador não encontrado para criação de grupo: " + adminId);
                return null;
            }
            
            // Criar novo grupo
            Group group = new Group();
            group.setGroupId(UUID.randomUUID().toString());
            group.setName(name);
            group.setDescription(description);
            group.setAdminId(adminId);
            group.setCreatorId(adminId);
            group.setCreatedAt(System.currentTimeMillis());
            group.setDeleteOnAdminExit(deleteOnAdminExit);
            
            // Salvar grupo
            boolean saved = groupDAO.save(group);
            if (!saved) {
                logger.warning("Falha ao salvar novo grupo: " + name);
                return null;
            }
            
            // Adicionar criador como administrador do grupo
            GroupMember member = new GroupMember();
            member.setMemberId(UUID.randomUUID().toString());
            member.setGroupId(group.getGroupId());
            member.setUserId(adminId);
            member.setAdmin(true);
            member.setJoinedAt(System.currentTimeMillis());
            
            boolean memberSaved = groupMemberDAO.save(member);
            if (!memberSaved) {
                logger.warning("Falha ao adicionar administrador como membro do grupo: " + name);
                groupDAO.delete(group.getGroupId());
                return null;
            }
            
            logger.info("Novo grupo criado: " + name + " por " + adminId);
            return group;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao criar grupo", e);
            throw new RemoteException("Erro ao criar grupo", e);
        }
    }
    
    @Override
    public Group getGroup(String groupId) throws RemoteException {
        try {
            return groupDAO.findById(groupId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar grupo", e);
            throw new RemoteException("Erro ao buscar grupo", e);
        }
    }
    
    @Override
    public List<Group> getAllGroups() throws RemoteException {
        try {
            return groupDAO.findAll();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar todos os grupos", e);
            throw new RemoteException("Erro ao buscar todos os grupos", e);
        }
    }
    
    @Override
    public List<Group> getUserGroups(String userId) throws RemoteException {
        try {
            List<GroupMember> memberships = groupMemberDAO.findByUserId(userId);
            List<Group> groups = new ArrayList<>();
            
            for (GroupMember membership : memberships) {
                Group group = groupDAO.findById(membership.getGroupId());
                if (group != null) {
                    groups.add(group);
                }
            }
            
            return groups;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar grupos do usuário", e);
            throw new RemoteException("Erro ao buscar grupos do usuário", e);
        }
    }
    
    /**
     * Método auxiliar para atualizar informações de um grupo.
     * Não faz parte da interface pública, mas pode ser usado internamente.
     * 
     * @param groupId ID do grupo
     * @param name Novo nome do grupo
     * @param description Nova descrição do grupo
     * @param updaterId ID do usuário que está atualizando
     * @return true se atualizado com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    private boolean updateGroup(String groupId, String name, String description, String updaterId) throws RemoteException {
        try {
            Group group = groupDAO.findById(groupId);
            if (group == null) {
                logger.info("Grupo não encontrado para atualização: " + groupId);
                return false;
            }
            
            // Verificar se o usuário é administrador do grupo
            GroupMember member = groupMemberDAO.findByGroupAndUser(groupId, updaterId);
            if (member == null || !member.isAdmin()) {
                logger.info("Usuário não é administrador do grupo: " + updaterId);
                return false;
            }
            
            // Atualizar campos
            if (name != null && !name.isEmpty()) {
                group.setName(name);
            }
            
            if (description != null) {
                group.setDescription(description);
            }
            
            // Salvar grupo
            boolean saved = groupDAO.save(group);
            if (saved) {
                logger.info("Grupo atualizado com sucesso: " + groupId);
                return true;
            } else {
                logger.warning("Falha ao atualizar grupo: " + groupId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao atualizar grupo", e);
            throw new RemoteException("Erro ao atualizar grupo", e);
        }
    }
    
    @Override
    public boolean deleteGroup(String groupId, String deleterId) throws RemoteException {
        try {
            Group group = groupDAO.findById(groupId);
            if (group == null) {
                logger.info("Grupo não encontrado para exclusão: " + groupId);
                return false;
            }
            
            // Verificar se o usuário é o criador do grupo
            if (!group.getCreatorId().equals(deleterId)) {
                logger.info("Usuário não é o criador do grupo: " + deleterId);
                return false;
            }
            
            // Excluir todos os membros do grupo
            List<GroupMember> members = groupMemberDAO.findByGroupId(groupId);
            for (GroupMember member : members) {
                groupMemberDAO.delete(member.getMemberId());
            }
            
            // Excluir grupo
            boolean deleted = groupDAO.delete(groupId);
            if (deleted) {
                logger.info("Grupo excluído com sucesso: " + groupId);
                return true;
            } else {
                logger.warning("Falha ao excluir grupo: " + groupId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao excluir grupo", e);
            throw new RemoteException("Erro ao excluir grupo", e);
        }
    }
    
    @Override
    public List<GroupMember> getGroupMembers(String groupId) throws RemoteException {
        try {
            List<GroupMember> members = groupMemberDAO.findByGroupId(groupId);
            return members;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar membros do grupo", e);
            throw new RemoteException("Erro ao buscar membros do grupo", e);
        }
    }
    
    @Override
    public boolean addUserToGroup(String groupId, String userId, String adderId) throws RemoteException {
        try {
            Group group = groupDAO.findById(groupId);
            User user = userDAO.findById(userId);
            
            if (group == null || user == null) {
                logger.info("Grupo ou usuário não encontrado para adição ao grupo");
                return false;
            }
            
            // Verificar se o usuário que está adicionando é administrador do grupo
            GroupMember adderMember = groupMemberDAO.findByGroupAndUser(groupId, adderId);
            if (adderMember == null || !adderMember.isAdmin()) {
                logger.info("Usuário não tem permissão para adicionar membros: " + adderId);
                return false;
            }
            
            // Verificar se o usuário já é membro do grupo
            GroupMember existingMember = groupMemberDAO.findByGroupAndUser(groupId, userId);
            if (existingMember != null) {
                logger.info("Usuário já é membro do grupo: " + userId);
                return false;
            }
            
            // Adicionar usuário ao grupo
            GroupMember newMember = new GroupMember();
            newMember.setMemberId(UUID.randomUUID().toString());
            newMember.setGroupId(groupId);
            newMember.setUserId(userId);
            newMember.setJoinedAt(System.currentTimeMillis());
            newMember.setAdmin(false);
            
            boolean saved = groupMemberDAO.save(newMember);
            if (saved) {
                logger.info("Usuário adicionado ao grupo com sucesso: " + userId + " ao grupo " + groupId);
                return true;
            } else {
                logger.warning("Falha ao adicionar usuário ao grupo: " + userId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao adicionar usuário ao grupo", e);
            throw new RemoteException("Erro ao adicionar usuário ao grupo", e);
        }
    }
    
    @Override
    public boolean removeUserFromGroup(String groupId, String userId, String removerId) throws RemoteException {
        try {
            Group group = groupDAO.findById(groupId);
            if (group == null) {
                logger.info("Grupo não encontrado para remoção de usuário: " + groupId);
                return false;
            }
            
            // Verificar se o usuário é membro do grupo
            GroupMember member = groupMemberDAO.findByGroupAndUser(groupId, userId);
            if (member == null) {
                logger.info("Usuário não é membro do grupo: " + userId);
                return false;
            }
            
            // Verificar se o removedor é administrador do grupo ou o próprio usuário (saindo do grupo)
            if (!userId.equals(removerId)) {
                GroupMember remover = groupMemberDAO.findByGroupAndUser(groupId, removerId);
                if (remover == null || !remover.isAdmin()) {
                    logger.info("Usuário não tem permissão para remover membros do grupo: " + removerId);
                    return false;
                }
            }
            
            // Não permitir a remoção do criador do grupo
            if (userId.equals(group.getCreatorId())) {
                logger.info("Não é possível remover o criador do grupo: " + userId);
                return false;
            }
            
            // Remover usuário do grupo
            boolean removed = groupMemberDAO.delete(member.getMemberId());
            if (removed) {
                logger.info("Usuário removido do grupo com sucesso: " + userId + " do grupo " + groupId);
                return true;
            } else {
                logger.warning("Falha ao remover usuário do grupo: " + userId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao remover usuário do grupo", e);
            throw new RemoteException("Erro ao remover usuário do grupo", e);
        }
    }
    
    @Override
    public boolean changeGroupAdmin(String groupId, String currentAdminId, String newAdminId) throws RemoteException {
        try {
            Group group = groupDAO.findById(groupId);
            if (group == null) {
                logger.info("Grupo não encontrado para mudança de administrador: " + groupId);
                return false;
            }
            
            // Verificar se o solicitante é o administrador atual
            if (!group.getAdminId().equals(currentAdminId)) {
                logger.info("Apenas o administrador atual pode transferir administração: " + currentAdminId);
                return false;
            }
            
            // Verificar se o novo administrador é membro do grupo
            GroupMember newAdminMember = groupMemberDAO.findByGroupAndUser(groupId, newAdminId);
            if (newAdminMember == null) {
                logger.info("Novo administrador não é membro do grupo: " + newAdminId);
                return false;
            }
            
            // Atualizar administrador do grupo
            group.setAdminId(newAdminId);
            boolean updated = groupDAO.save(group);
            
            if (updated) {
                // Atualizar status de administrador dos membros
                GroupMember currentAdminMember = groupMemberDAO.findByGroupAndUser(groupId, currentAdminId);
                if (currentAdminMember != null) {
                    currentAdminMember.setAdmin(false);
                    groupMemberDAO.save(currentAdminMember);
                }
                
                newAdminMember.setAdmin(true);
                groupMemberDAO.save(newAdminMember);
                
                logger.info("Administrador do grupo alterado com sucesso: de " + currentAdminId + " para " + newAdminId);
                return true;
            } else {
                logger.warning("Falha ao atualizar administrador do grupo: " + groupId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao mudar administrador do grupo", e);
            throw new RemoteException("Erro ao mudar administrador do grupo", e);
        }
    }
    
    @Override
    public boolean leaveGroup(String groupId, String userId) throws RemoteException {
        try {
            Group group = groupDAO.findById(groupId);
            if (group == null) {
                logger.info("Grupo não encontrado para saída: " + groupId);
                return false;
            }
            
            // Verificar se o usuário é membro do grupo
            GroupMember member = groupMemberDAO.findByGroupAndUser(groupId, userId);
            if (member == null) {
                logger.info("Usuário não é membro do grupo: " + userId);
                return false;
            }
            
            // Verificar se o usuário é o administrador
            if (userId.equals(group.getAdminId())) {
                // Se o grupo deve ser excluído quando o administrador sair
                if (group.isDeleteOnAdminExit()) {
                    // Excluir todos os membros do grupo
                    List<GroupMember> members = groupMemberDAO.findByGroupId(groupId);
                    for (GroupMember m : members) {
                        groupMemberDAO.delete(m.getMemberId());
                    }
                    
                    // Excluir o grupo
                    boolean deleted = groupDAO.delete(groupId);
                    if (deleted) {
                        logger.info("Grupo excluído após saída do administrador: " + groupId);
                        return true;
                    } else {
                        logger.warning("Falha ao excluir grupo após saída do administrador: " + groupId);
                        return false;
                    }
                } else {
                    // Encontrar outro membro para ser administrador
                    List<GroupMember> members = groupMemberDAO.findByGroupId(groupId);
                    if (members.size() > 1) {
                        for (GroupMember m : members) {
                            if (!m.getUserId().equals(userId)) {
                                // Definir novo administrador
                                group.setAdminId(m.getUserId());
                                m.setAdmin(true);
                                groupDAO.save(group);
                                groupMemberDAO.save(m);
                                break;
                            }
                        }
                    }
                }
            }
            
            // Remover usuário do grupo
            boolean removed = groupMemberDAO.delete(member.getMemberId());
            if (removed) {
                logger.info("Usuário saiu do grupo com sucesso: " + userId + " do grupo " + groupId);
                return true;
            } else {
                logger.warning("Falha ao remover usuário do grupo: " + userId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao sair do grupo", e);
            throw new RemoteException("Erro ao sair do grupo", e);
        }
    }
    
    @Override
    public boolean requestGroupJoin(String groupId, String userId) throws RemoteException {
        try {
            Group group = groupDAO.findById(groupId);
            if (group == null) {
                logger.info("Grupo não encontrado para solicitação de entrada: " + groupId);
                return false;
            }
            
            // Verificar se o usuário já é membro do grupo
            GroupMember existingMember = groupMemberDAO.findByGroupAndUser(groupId, userId);
            if (existingMember != null) {
                logger.info("Usuário já é membro do grupo: " + userId);
                return false;
            }
            
            // Criar solicitação de entrada (usando o mesmo modelo GroupMember com flag pendente)
            GroupMember request = new GroupMember();
            request.setMemberId(UUID.randomUUID().toString());
            request.setGroupId(groupId);
            request.setUserId(userId);
            request.setAdmin(false);
            request.setJoinedAt(System.currentTimeMillis());
            // Aqui poderia ser adicionado um campo para indicar que é uma solicitação pendente
            // Por enquanto, vamos simular isso com um campo na implementação
            
            boolean saved = groupMemberDAO.saveJoinRequest(request);
            if (saved) {
                logger.info("Solicitação de entrada no grupo registrada: " + userId + " para o grupo " + groupId);
                return true;
            } else {
                logger.warning("Falha ao registrar solicitação de entrada no grupo: " + userId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao solicitar entrada no grupo", e);
            throw new RemoteException("Erro ao solicitar entrada no grupo", e);
        }
    }
    
    @Override
    public boolean processJoinRequest(String groupId, String userId, String adminId, boolean approved) throws RemoteException {
        try {
            Group group = groupDAO.findById(groupId);
            if (group == null) {
                logger.info("Grupo não encontrado para processamento de solicitação: " + groupId);
                return false;
            }
            
            // Verificar se o administrador é o administrador do grupo
            if (!group.getAdminId().equals(adminId)) {
                logger.info("Apenas o administrador pode processar solicitações: " + adminId);
                return false;
            }
            
            // Buscar solicitação pendente
            GroupMember request = groupMemberDAO.findJoinRequest(groupId, userId);
            if (request == null) {
                logger.info("Solicitação de entrada não encontrada: " + userId);
                return false;
            }
            
            if (approved) {
                // Aprovar solicitação (converter para membro)
                request.setJoinedAt(System.currentTimeMillis()); // Atualizar data de entrada
                boolean saved = groupMemberDAO.save(request);
                if (saved) {
                    logger.info("Solicitação de entrada aprovada: " + userId + " no grupo " + groupId);
                    return true;
                } else {
                    logger.warning("Falha ao aprovar solicitação de entrada: " + userId);
                    return false;
                }
            } else {
                // Rejeitar solicitação (excluir)
                boolean deleted = groupMemberDAO.deleteJoinRequest(request.getMemberId());
                if (deleted) {
                    logger.info("Solicitação de entrada rejeitada: " + userId + " no grupo " + groupId);
                    return true;
                } else {
                    logger.warning("Falha ao rejeitar solicitação de entrada: " + userId);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao processar solicitação de entrada no grupo", e);
            throw new RemoteException("Erro ao processar solicitação de entrada no grupo", e);
        }
    }
    
    @Override
    public List<User> getGroupUsers(String groupId) throws RemoteException {
        try {
            List<GroupMember> members = groupMemberDAO.findByGroupId(groupId);
            List<User> users = new ArrayList<>();
            
            for (GroupMember member : members) {
                User user = userDAO.findById(member.getUserId());
                if (user != null) {
                    users.add(user);
                }
            }
            
            return users;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao buscar usuários do grupo", e);
            throw new RemoteException("Erro ao buscar usuários do grupo", e);
        }
    }
    
    private boolean setGroupAdmin(String groupId, String userId, String adminId, boolean isAdmin) throws RemoteException {
        try {
            Group group = groupDAO.findById(groupId);
            if (group == null) {
                logger.info("Grupo não encontrado para definição de administrador: " + groupId);
                return false;
            }
            
            // Verificar se o usuário é membro do grupo
            GroupMember member = groupMemberDAO.findByGroupAndUser(groupId, userId);
            if (member == null) {
                logger.info("Usuário não é membro do grupo: " + userId);
                return false;
            }
            
            // Verificar se o administrador é o criador do grupo
            if (!group.getCreatorId().equals(adminId)) {
                logger.info("Apenas o criador do grupo pode definir administradores: " + adminId);
                return false;
            }
            
            // Atualizar status de administrador
            member.setAdmin(isAdmin);
            
            // Salvar membro
            boolean saved = groupMemberDAO.save(member);
            if (saved) {
                logger.info("Status de administrador atualizado com sucesso: " + userId + " no grupo " + groupId + " - Admin: " + isAdmin);
                return true;
            } else {
                logger.warning("Falha ao atualizar status de administrador: " + userId);
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Erro ao definir administrador do grupo", e);
            throw new RemoteException("Erro ao definir administrador do grupo", e);
        }
    }
}
