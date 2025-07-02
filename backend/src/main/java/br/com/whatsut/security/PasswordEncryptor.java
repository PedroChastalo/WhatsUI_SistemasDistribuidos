package br.com.whatsut.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilitário para criptografia e verificação de senhas
 */
public class PasswordEncryptor {
    /**
     * Gera o hash da senha em texto puro
     * @param plainTextPassword Senha em texto puro
     * @return Hash da senha
     */
    public static String hashPassword(String plainTextPassword) {
        return BCrypt.hashpw(plainTextPassword, BCrypt.gensalt(12));
    }
    
    /**
     * Verifica se a senha em texto puro corresponde ao hash
     * @param plainTextPassword Senha em texto puro
     * @param hashedPassword Hash da senha
     * @return true se a senha corresponder ao hash
     */
    public static boolean checkPassword(String plainTextPassword, String hashedPassword) {
        return BCrypt.checkpw(plainTextPassword, hashedPassword);
    }
}
