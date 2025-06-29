package br.com.whatsut.service;

import br.com.whatsut.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface remota para serviços relacionados a usuários no sistema WhatsUT.
 */
public interface UserService extends Remote {
    
    /**
     * Busca um usuário pelo ID.
     * 
     * @param userId ID do usuário
     * @return O usuário encontrado ou null se não existir
     * @throws RemoteException Erro de comunicação RMI
     */
    User getUser(String userId) throws RemoteException;
    
    /**
     * Busca um usuário pelo nome de usuário.
     * 
     * @param username Nome de usuário
     * @return O usuário encontrado ou null se não existir
     * @throws RemoteException Erro de comunicação RMI
     */
    User getUserByUsername(String username) throws RemoteException;
    
    /**
     * Lista todos os usuários do sistema.
     * 
     * @return Lista de usuários
     * @throws RemoteException Erro de comunicação RMI
     */
    List<User> getAllUsers() throws RemoteException;
    
    /**
     * Atualiza as informações de um usuário.
     * 
     * @param user Usuário com informações atualizadas
     * @return true se atualizado com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean updateUser(User user) throws RemoteException;
    
    /**
     * Atualiza o status online de um usuário.
     * 
     * @param userId ID do usuário
     * @param online Status online
     * @return true se atualizado com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean updateUserStatus(String userId, boolean online) throws RemoteException;
    
    /**
     * Solicita banimento de um usuário do sistema.
     * 
     * @param requesterId ID do usuário que solicita o banimento
     * @param targetUserId ID do usuário a ser banido
     * @param reason Motivo do banimento
     * @return true se a solicitação foi registrada com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean requestUserBan(String requesterId, String targetUserId, String reason) throws RemoteException;
    
    /**
     * Bane um usuário do sistema (apenas para administradores).
     * 
     * @param adminId ID do administrador
     * @param userId ID do usuário a ser banido
     * @return true se banido com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean banUser(String adminId, String userId) throws RemoteException;
}
