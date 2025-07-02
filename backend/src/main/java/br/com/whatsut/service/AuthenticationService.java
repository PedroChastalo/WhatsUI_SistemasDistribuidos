package br.com.whatsut.service;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

/**
 * Interface para o serviço de autenticação
 * Define os métodos para login, registro, logout e validação de sessão
 */
public interface AuthenticationService extends Remote {
    /**
     * Realiza o login do usuário
     * @param email Email ou username do usuário
     * @param password Senha do usuário
     * @return Mapa com dados do usuário e sessão
     * @throws RemoteException Erro RMI
     */
    Map<String, Object> login(String email, String password) throws RemoteException;

    /**
     * Realiza o registro de um novo usuário
     * @param username Nome de usuário
     * @param email Email do usuário
     * @param displayName Nome de exibição
     * @param password Senha
     * @return Mapa com dados do usuário e sessão
     * @throws RemoteException Erro RMI
     */
    Map<String, Object> register(String username, String email, String displayName, String password) throws RemoteException;

    /**
     * Realiza o logout do usuário
     * @param sessionId ID da sessão
     * @return true se logout realizado com sucesso
     * @throws RemoteException Erro RMI
     */
    boolean logout(String sessionId) throws RemoteException;

    /**
     * Valida se a sessão é válida
     * @param sessionId ID da sessão
     * @return true se a sessão for válida
     * @throws RemoteException Erro RMI
     */
    boolean validateSession(String sessionId) throws RemoteException;
}
