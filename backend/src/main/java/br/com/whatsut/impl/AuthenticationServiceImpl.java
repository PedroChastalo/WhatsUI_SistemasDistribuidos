package br.com.whatsut.impl;

import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.User;
import br.com.whatsut.security.PasswordEncryptor;
import br.com.whatsut.security.SessionManager;
import br.com.whatsut.service.AuthenticationService;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementação do serviço de autenticação
 */
public class AuthenticationServiceImpl implements AuthenticationService {
    // DAO para acesso aos dados de usuários
    private final UserDAO userDAO = new UserDAO();
    
    public AuthenticationServiceImpl() throws RemoteException {
        super();
    }
    
    /**
     * Realiza o login do usuário
     * @param email Email ou username do usuário
     * @param password Senha do usuário
     * @return Mapa com resultado do login
     */
    @Override
    public Map<String, Object> login(String email, String password) throws RemoteException {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Tentar localizar por email primeiro (insensível a maiúsculas/minúsculas)
            User user = userDAO.getUserByEmail(email);
            // Se não encontrado por email, tentar por username (case-insensitive)
            if (user == null) {
                user = userDAO.getUserByUsernameIgnoreCase(email);
            }

            if (user == null) {
                result.put("success", false);
                result.put("error", "Usuário não encontrado");
                return result;
            }
            
            // Verifica a senha
            if (!PasswordEncryptor.checkPassword(password, user.getPasswordHash())) {
                result.put("success", false);
                result.put("error", "Senha incorreta");
                return result;
            }
            
            // Cria sessão
            String sessionId = SessionManager.createSession(user.getUserId());
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", user.getUserId());
            userData.put("username", user.getUsername());
            userData.put("email", user.getEmail());
            userData.put("displayName", user.getDisplayName());
            userData.put("sessionId", sessionId);
            
            result.put("success", true);
            result.put("data", userData);
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Erro ao fazer login: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Realiza o registro de um novo usuário
     * @param username Nome de usuário
     * @param email Email do usuário
     * @param displayName Nome de exibição
     * @param password Senha
     * @return Mapa com resultado do registro
     */
    @Override
    public Map<String, Object> register(String username, String email, String displayName, String password) throws RemoteException {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Verifica se já existe usuário com o email informado
            User existingUser = userDAO.getUserByEmail(email);
            if (existingUser != null) {
                result.put("success", false);
                result.put("error", "Email já está em uso");
                return result;
            }
            
            // Cria novo usuário
            User newUser = userDAO.createUser(username, email, displayName, password);
            String sessionId = SessionManager.createSession(newUser.getUserId());
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("userId", newUser.getUserId());
            userData.put("username", newUser.getUsername());
            userData.put("email", newUser.getEmail());
            userData.put("displayName", newUser.getDisplayName());
            userData.put("sessionId", sessionId);
            
            result.put("success", true);
            result.put("data", userData);
            
            return result;
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "Erro ao registrar usuário: " + e.getMessage());
            return result;
        }
    }
    
    /**
     * Realiza o logout do usuário
     * @param sessionId ID da sessão
     * @return true se logout realizado com sucesso
     */
    @Override
    public boolean logout(String sessionId) throws RemoteException {
        try {
            SessionManager.removeSession(sessionId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Valida se a sessão é válida
     * @param sessionId ID da sessão
     * @return true se a sessão for válida
     */
    @Override
    public boolean validateSession(String sessionId) throws RemoteException {
        return SessionManager.isValidSession(sessionId);
    }
}
