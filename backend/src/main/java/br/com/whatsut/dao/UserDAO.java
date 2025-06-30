package br.com.whatsut.dao;

import br.com.whatsut.model.User;
import br.com.whatsut.security.PasswordEncryptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserDAO {
    private static final Map<String, User> users = new ConcurrentHashMap<>();
    private static final Map<String, String> emailToUserId = new ConcurrentHashMap<>();
    
    public UserDAO() {
        // Criar um usuário de teste automaticamente
        try {
            createUser("teste", "teste@email.com", "Usuário Teste", "teste123");
            System.out.println("Usuário de teste criado com sucesso: teste@email.com / teste123");
        } catch (Exception e) {
            System.out.println("Usuário de teste já existe.");
        }
    }
    
    public User createUser(String username, String email, String displayName, String password) {
        // Verificar se o email já está em uso
        if (emailToUserId.containsKey(email)) {
            throw new IllegalArgumentException("Email já está em uso");
        }
        
        String userId = UUID.randomUUID().toString();
        String passwordHash = PasswordEncryptor.hashPassword(password);
        
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordHash);
        user.setStatus("online");
        user.setActive(true);
        
        users.put(userId, user);
        emailToUserId.put(email, userId);
        
        return user;
    }
    
    public User getUserById(String userId) {
        return users.get(userId);
    }
    
    public User getUserByEmail(String email) {
        String userId = emailToUserId.get(email);
        return userId != null ? users.get(userId) : null;
    }
    
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    public boolean updateUserStatus(String userId, String status) {
        User user = users.get(userId);
        if (user != null) {
            user.setStatus(status);
            return true;
        }
        return false;
    }
}
