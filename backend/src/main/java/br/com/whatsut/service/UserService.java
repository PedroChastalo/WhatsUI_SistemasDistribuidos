package br.com.whatsut.service;

import br.com.whatsut.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Interface para o serviço RMI de gerenciamento de usuários
 */
public interface UserService extends Remote {
    
    /**
     * Obtém a lista de todos os usuários
     * @param sessionId ID da sessão do usuário autenticado
     * @return Lista de usuários
     * @throws RemoteException Erro RMI
     */
    List<User> getAllUsers(String sessionId) throws RemoteException;
    
    /**
     * Obtém um usuário pelo ID
     * @param sessionId ID da sessão do usuário autenticado
     * @param userId ID do usuário a ser obtido
     * @return Usuário encontrado ou null
     * @throws RemoteException Erro RMI
     */
    User getUserById(String sessionId, String userId) throws RemoteException;
    
    /**
     * Atualiza o status de um usuário
     * @param sessionId ID da sessão do usuário autenticado
     * @param status Novo status do usuário
     * @return true se atualizado com sucesso
     * @throws RemoteException Erro RMI
     */
    boolean updateStatus(String sessionId, String status) throws RemoteException;
}
