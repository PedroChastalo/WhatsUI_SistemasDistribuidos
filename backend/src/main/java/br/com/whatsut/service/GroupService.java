package br.com.whatsut.service;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Interface para o serviço RMI de grupos
 */
public interface GroupService extends Remote {
    
    /**
     * Obtém todos os grupos do usuário
     * @param sessionId ID da sessão do usuário autenticado
     * @return Lista de grupos
     * @throws RemoteException Erro RMI
     */
    List<Map<String, Object>> getGroups(String sessionId) throws RemoteException;
    
    /**
     * Cria um novo grupo
     * @param sessionId ID da sessão do usuário autenticado
     * @param name Nome do grupo
     * @param members Lista de IDs dos membros iniciais
     * @return Dados do grupo criado
     * @throws RemoteException Erro RMI
     */
    Map<String, Object> createGroup(String sessionId, String name, List<String> members) throws RemoteException;
    
    /**
     * Obtém as mensagens de um grupo
     * @param sessionId ID da sessão do usuário autenticado
     * @param groupId ID do grupo
     * @return Lista de mensagens do grupo
     * @throws RemoteException Erro RMI
     */
    List<Map<String, Object>> getGroupMessages(String sessionId, String groupId) throws RemoteException;
    
    /**
     * Envia uma mensagem para um grupo
     * @param sessionId ID da sessão do usuário autenticado
     * @param groupId ID do grupo
     * @param content Conteúdo da mensagem
     * @return Dados da mensagem enviada
     * @throws RemoteException Erro RMI
     */
    Map<String, Object> sendGroupMessage(String sessionId, String groupId, String content) throws RemoteException;
    
    /**
     * Obtém os membros de um grupo
     * @param sessionId ID da sessão do usuário autenticado
     * @param groupId ID do grupo
     * @return Lista de membros do grupo
     * @throws RemoteException Erro RMI
     */
    List<Map<String, Object>> getGroupMembers(String sessionId, String groupId) throws RemoteException;

    /**
     * Adiciona um usuário a um grupo
     * @param sessionId ID da sessão do usuário autenticado
     * @param groupId ID do grupo
     * @param userIdToAdd ID do usuário a ser adicionado
     * @throws RemoteException Erro RMI
     */
    void addUserToGroup(String sessionId, String groupId, String userIdToAdd) throws RemoteException;

    /**
     * Remove um usuário de um grupo
     * @param sessionId ID da sessão do usuário autenticado
     * @param groupId ID do grupo
     * @param userIdToRemove ID do usuário a ser removido
     * @throws RemoteException Erro RMI
     */
    void removeUserFromGroup(String sessionId, String groupId, String userIdToRemove) throws RemoteException;

    /**
     * Define um usuário como admin de um grupo
     * @param sessionId ID da sessão do usuário autenticado
     * @param groupId ID do grupo
     * @param userIdToSetAdmin ID do usuário a ser promovido a admin
     * @throws RemoteException Erro RMI
     */
    void setGroupAdmin(String sessionId, String groupId, String userIdToSetAdmin) throws RemoteException;

    /**
     * Permite que um usuário saia de um grupo
     * @param sessionId ID da sessão do usuário autenticado
     * @param groupId ID do grupo
     * @throws RemoteException Erro RMI
     */
    void leaveGroup(String sessionId, String groupId) throws RemoteException;

    /**
     * Exclui um grupo
     * @param sessionId ID da sessão do usuário autenticado
     * @param groupId ID do grupo a ser excluído
     * @throws RemoteException Erro RMI
     */
    void deleteGroup(String sessionId, String groupId) throws RemoteException;
}
