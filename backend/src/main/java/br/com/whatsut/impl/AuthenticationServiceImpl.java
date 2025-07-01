package br.com.whatsut.impl;

import br.com.whatsut.dao.UserDAO;
import br.com.whatsut.model.User;
import br.com.whatsut.security.PasswordEncryptor;
import br.com.whatsut.security.SessionManager;
import br.com.whatsut.service.AuthenticationService;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationServiceImpl implements AuthenticationService {
    private final UserDAO userDAO = new UserDAO();
    
    public AuthenticationServiceImpl() throws RemoteException {
        super();
    }
    
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
            
            if (!PasswordEncryptor.checkPassword(password, user.getPasswordHash())) {
                result.put("success", false);
                result.put("error", "Senha incorreta");
                return result;
            }
            
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
    
    @Override
    public Map<String, Object> register(String username, String email, String displayName, String password) throws RemoteException {
        Map<String, Object> result = new HashMap<>();
        
        try {
            User existingUser = userDAO.getUserByEmail(email);
            if (existingUser != null) {
                result.put("success", false);
                result.put("error", "Email já está em uso");
                return result;
            }
            
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
    
    @Override
    public boolean logout(String sessionId) throws RemoteException {
        try {
            SessionManager.removeSession(sessionId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean validateSession(String sessionId) throws RemoteException {
        return SessionManager.isValidSession(sessionId);
    }
}
