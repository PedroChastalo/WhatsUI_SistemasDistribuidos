package br.com.whatsut.util;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Utilitários de segurança para o sistema WhatsUT.
 * Fornece métodos para hash de senha, verificação de senha e geração de tokens.
 */
public class SecurityUtils {
    private static final Logger logger = Logger.getLogger(SecurityUtils.class.getName());
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;
    
    /**
     * Gera um hash BCrypt para a senha fornecida.
     *
     * @param password Senha em texto plano
     * @return Hash BCrypt da senha
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Senha não pode ser nula ou vazia");
        }
        
        // Gerar salt e hash
        String salt = BCrypt.gensalt(12); // 2^12 rounds
        return BCrypt.hashpw(password, salt);
    }
    
    /**
     * Verifica se uma senha corresponde a um hash BCrypt.
     *
     * @param password Senha em texto plano
     * @param hashedPassword Hash BCrypt da senha
     * @return true se a senha corresponder ao hash, false caso contrário
     */
    public static boolean checkPassword(String password, String hashedPassword) {
        if (password == null || password.isEmpty() || hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (Exception e) {
            logger.warning("Erro ao verificar senha: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gera um token aleatório seguro para autenticação.
     *
     * @return Token aleatório em formato Base64
     */
    public static String generateToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    /**
     * Verifica se uma senha atende aos requisitos mínimos de segurança.
     *
     * @param password Senha a ser verificada
     * @return true se a senha atender aos requisitos, false caso contrário
     */
    public static boolean isPasswordSecure(String password) {
        if (password == null) {
            return false;
        }
        
        int minLength = ConfigManager.getIntProperty("security.password.min.length", 6);
        
        // Verificar comprimento mínimo
        if (password.length() < minLength) {
            return false;
        }
        
        // Verificar se contém pelo menos um número
        boolean hasDigit = password.matches(".*\\d.*");
        
        // Verificar se contém pelo menos uma letra
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        
        return hasDigit && hasLetter;
    }
}
