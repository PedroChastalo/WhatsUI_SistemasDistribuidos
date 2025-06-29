package br.com.whatsut.service;

import br.com.whatsut.model.Session;
import br.com.whatsut.model.User;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface remota para serviços de autenticação no sistema WhatsUT.
 */
public interface AuthService extends Remote {
    
    /**
     * Registra um novo usuário no sistema.
     * 
     * @param username Nome de usuário único
     * @param displayName Nome de exibição
     * @param password Senha (será armazenada como hash)
     * @param email Email do usuário
     * @return O usuário criado ou null se o username já existir
     * @throws RemoteException Erro de comunicação RMI
     */
    User register(String username, String displayName, String password, String email) throws RemoteException;
    
    /**
     * Autentica um usuário com username e senha.
     * 
     * @param username Nome de usuário
     * @param password Senha
     * @param clientAddress Endereço IP do cliente
     * @return Sessão com token de autenticação ou null se credenciais inválidas
     * @throws RemoteException Erro de comunicação RMI
     */
    Session login(String username, String password, String clientAddress) throws RemoteException;
    
    /**
     * Valida um token de sessão.
     * 
     * @param sessionId ID da sessão
     * @param token Token de autenticação
     * @return true se o token for válido, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean validateToken(String sessionId, String token) throws RemoteException;
    
    /**
     * Encerra uma sessão de usuário (logout).
     * 
     * @param sessionId ID da sessão a ser encerrada
     * @return true se a sessão foi encerrada com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean logout(String sessionId) throws RemoteException;
    
    /**
     * Altera a senha de um usuário.
     * 
     * @param userId ID do usuário
     * @param currentPassword Senha atual
     * @param newPassword Nova senha
     * @return true se a senha foi alterada com sucesso, false caso contrário
     * @throws RemoteException Erro de comunicação RMI
     */
    boolean changePassword(String userId, String currentPassword, String newPassword) throws RemoteException;
}
