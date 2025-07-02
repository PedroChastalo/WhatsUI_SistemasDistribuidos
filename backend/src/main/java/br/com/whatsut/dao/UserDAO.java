package br.com.whatsut.dao;

import br.com.whatsut.model.User;
import br.com.whatsut.security.PasswordEncryptor;
import br.com.whatsut.util.DataPersistenceUtil;

import com.fasterxml.jackson.core.type.TypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAO para gerenciar usuários
 */
public class UserDAO {
    private static final String USERS_FILE = "users";
    private static final String EMAIL_MAP_FILE = "email_map";
    
    private static Map<String, User> users;
    private static Map<String, String> emailToUserId;
    
    public UserDAO() {
        // Carregar dados do arquivo ou criar novos mapas se não existirem
        loadData();
        
        // Criar um usuário de teste automaticamente se não existir
        if (getUserByEmail("teste@email.com") == null) {
            try {
                createUser("teste", "teste@email.com", "Usuário Teste", "teste123");
                System.out.println("Usuário de teste criado com sucesso: teste@email.com / teste123");
            } catch (Exception e) {
                System.out.println("Erro ao criar usuário de teste: " + e.getMessage());
            }
        } else {
            System.out.println("Usuário de teste já existe.");
        }
    }
    
    /**
     * Carrega os dados dos arquivos JSON
     */
    private void loadData() {
        // Carregar usuários
        TypeReference<Map<String, User>> userTypeRef = new TypeReference<Map<String, User>>() {};
        users = DataPersistenceUtil.loadData(USERS_FILE, userTypeRef, new ConcurrentHashMap<>());
        
        // Carregar mapa de email para userId
        TypeReference<Map<String, String>> emailMapTypeRef = new TypeReference<Map<String, String>>() {};
        emailToUserId = DataPersistenceUtil.loadData(EMAIL_MAP_FILE, emailMapTypeRef, new ConcurrentHashMap<>());
        
        System.out.println("Dados de usuários carregados: " + users.size() + " usuários");
    }
    
    /**
     * Salva os dados em arquivos JSON
     */
    private void saveData() {
        DataPersistenceUtil.saveData(USERS_FILE, users);
        DataPersistenceUtil.saveData(EMAIL_MAP_FILE, emailToUserId);
    }
    
    public User createUser(String username, String email, String displayName, String password) {
     
        String normalizedEmail = email.toLowerCase();
       
        if (emailToUserId.containsKey(normalizedEmail)) {
            throw new IllegalArgumentException("Email já está em uso");
        }
        
        String userId = UUID.randomUUID().toString();
        String passwordHash = PasswordEncryptor.hashPassword(password);
        
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setEmail(normalizedEmail);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordHash);
        user.setStatus("online");
        user.setActive(true);
        
        users.put(userId, user);
        emailToUserId.put(normalizedEmail, userId);
        
       
        saveData();
        
        return user;
    }
    
    public User getUserById(String userId) {
        return users.get(userId);
    }
    
    public User getUserByEmail(String email) {
        if (email == null) return null;
        String userId = emailToUserId.get(email.toLowerCase());
        return userId != null ? users.get(userId) : null;
    }
    
    public User getUserByUsernameIgnoreCase(String username) {
        if (username == null) return null;
        String lower = username.toLowerCase();
        for (User u : users.values()) {
            if (u.getUsername() != null && u.getUsername().toLowerCase().equals(lower)) {
                return u;
            }
        }
        return null;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }
    
    public boolean updateUserStatus(String userId, String status) {
        User user = users.get(userId);
        if (user != null) {
            user.setStatus(status);
           
            saveData();
            return true;
        }
        return false;
    }
}
