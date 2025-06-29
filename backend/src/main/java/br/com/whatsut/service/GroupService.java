package br.com.whatsut.service;

import br.com.whatsut.model.Group;
import br.com.whatsut.model.GroupMember;
import br.com.whatsut.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface remota para serviços relacionados a grupos no sistema WhatsUT.
 */
public interface GroupService extends Remote {
    
    /**
     * Cria um novo grupo.
     * 
     * @param name Nome do grupo
     * @param description Descrição do grupo
     * @param adminId ID do usuário administrador
     * @param deleteOnAdminExit Se o grupo deve ser excluído quando o administrador sair
     * @return O grupo criado ou null em caso de erro
     * @throws RemoteException Erro de comunicação RMI
     */
    Group createGroup(String name, String description, String adminId, boolean deleteOnAdminExit) throws RemoteException;
    
    /**
     * Busca um grupo pelo ID.
     * 
     * @param groupId ID do grupo
     * @return O grupo encontrado ou null se não existir
     * @throws RemoteException Erro de comunicação RMI
     */
    Group getGroup(String groupId) throws RemoteException;
    
    /**
     * Lista todos os grupos disponíveis.
     * 
     * @return Lista de grupos
     * @throws RemoteException Erro de comunicação RMI
     */
    List<Group> getAllGroups() throws RemoteException;
    
    /**
     * Lista os grupos de um usuário.
     * 
     * @param userId ID do usuário
     * @return Lista de grupos
     * @throws RemoteException Erro de comunicação RMI
     */
    List<Group> getUserGroups(String userId) throws RemoteException;
    
    /**
     * Adiciona um usuário a um grupo.
     * 
     * @param groupId ID do grupo
     * @param userId ID do usuário a ser adicionado
     * @param adminId ID do administrador que está adicionando
     * @return true se adicionado com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean addUserToGroup(String groupId, String userId, String adminId) throws RemoteException;
    
    /**
     * Remove um usuário de um grupo.
     * 
     * @param groupId ID do grupo
     * @param userId ID do usuário a ser removido
     * @param adminId ID do administrador que está removendo
     * @return true se removido com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean removeUserFromGroup(String groupId, String userId, String adminId) throws RemoteException;
    
    /**
     * Lista os membros de um grupo.
     * 
     * @param groupId ID do grupo
     * @return Lista de membros do grupo
     * @throws RemoteException Erro de comunicação RMI
     */
    List<GroupMember> getGroupMembers(String groupId) throws RemoteException;
    
    /**
     * Lista os usuários de um grupo.
     * 
     * @param groupId ID do grupo
     * @return Lista de usuários do grupo
     * @throws RemoteException Erro de comunicação RMI
     */
    List<User> getGroupUsers(String groupId) throws RemoteException;
    
    /**
     * Altera o administrador de um grupo.
     * 
     * @param groupId ID do grupo
     * @param currentAdminId ID do administrador atual
     * @param newAdminId ID do novo administrador
     * @return true se alterado com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean changeGroupAdmin(String groupId, String currentAdminId, String newAdminId) throws RemoteException;
    
    /**
     * Usuário sai de um grupo.
     * 
     * @param groupId ID do grupo
     * @param userId ID do usuário que está saindo
     * @return true se saiu com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean leaveGroup(String groupId, String userId) throws RemoteException;
    
    /**
     * Exclui um grupo.
     * 
     * @param groupId ID do grupo
     * @param adminId ID do administrador
     * @return true se excluído com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean deleteGroup(String groupId, String adminId) throws RemoteException;
    
    /**
     * Solicita entrada em um grupo.
     * 
     * @param groupId ID do grupo
     * @param userId ID do usuário solicitante
     * @return true se a solicitação foi registrada com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean requestGroupJoin(String groupId, String userId) throws RemoteException;
    
    /**
     * Aprova ou rejeita uma solicitação de entrada em grupo.
     * 
     * @param groupId ID do grupo
     * @param userId ID do usuário solicitante
     * @param adminId ID do administrador
     * @param approved true para aprovar, false para rejeitar
     * @return true se processado com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean processJoinRequest(String groupId, String userId, String adminId, boolean approved) throws RemoteException;
}
