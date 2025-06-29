package br.com.whatsut.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gerenciador de configurações do sistema WhatsUT.
 * Carrega e fornece acesso às configurações armazenadas em arquivos .properties.
 */
public class ConfigManager {
    private static final Logger logger = Logger.getLogger(ConfigManager.class.getName());
    private static final Properties properties = new Properties();
    private static final Properties storageProperties = new Properties();
    private static boolean initialized = false;
    
    /**
     * Inicializa o gerenciador de configurações carregando os arquivos de configuração.
     */
    public static synchronized void init() {
        if (initialized) {
            return;
        }
        
        try {
            // Carregar configurações gerais
            File configFile = new File("config/config.properties");
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                    logger.info("Configurações gerais carregadas com sucesso");
                }
            } else {
                logger.warning("Arquivo de configurações gerais não encontrado: " + configFile.getAbsolutePath());
            }
            
            // Carregar configurações de armazenamento
            File storageConfigFile = new File("config/storage.properties");
            if (storageConfigFile.exists()) {
                try (FileInputStream fis = new FileInputStream(storageConfigFile)) {
                    storageProperties.load(fis);
                    logger.info("Configurações de armazenamento carregadas com sucesso");
                }
            } else {
                logger.warning("Arquivo de configurações de armazenamento não encontrado: " + storageConfigFile.getAbsolutePath());
            }
            
            initialized = true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao carregar arquivos de configuração", e);
        }
    }
    
    /**
     * Obtém uma propriedade de configuração geral.
     *
     * @param key Chave da propriedade
     * @param defaultValue Valor padrão caso a propriedade não exista
     * @return Valor da propriedade ou o valor padrão
     */
    public static String getProperty(String key, String defaultValue) {
        if (!initialized) {
            init();
        }
        
        // Verificar primeiro nas propriedades gerais
        String value = properties.getProperty(key);
        if (value != null) {
            return value;
        }
        
        // Verificar nas propriedades de armazenamento
        value = storageProperties.getProperty(key);
        if (value != null) {
            return value;
        }
        
        return defaultValue;
    }
    
    /**
     * Obtém uma propriedade de configuração como inteiro.
     *
     * @param key Chave da propriedade
     * @param defaultValue Valor padrão caso a propriedade não exista ou não seja um número válido
     * @return Valor da propriedade como inteiro ou o valor padrão
     */
    public static int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key, null);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            logger.warning("Valor inválido para propriedade " + key + ": " + value);
            return defaultValue;
        }
    }
    
    /**
     * Obtém uma propriedade de configuração como long.
     *
     * @param key Chave da propriedade
     * @param defaultValue Valor padrão caso a propriedade não exista ou não seja um número válido
     * @return Valor da propriedade como long ou o valor padrão
     */
    public static long getLongProperty(String key, long defaultValue) {
        String value = getProperty(key, null);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            logger.warning("Valor inválido para propriedade " + key + ": " + value);
            return defaultValue;
        }
    }
    
    /**
     * Obtém uma propriedade de configuração como boolean.
     *
     * @param key Chave da propriedade
     * @param defaultValue Valor padrão caso a propriedade não exista
     * @return Valor da propriedade como boolean ou o valor padrão
     */
    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = getProperty(key, null);
        if (value == null) {
            return defaultValue;
        }
        
        return Boolean.parseBoolean(value);
    }
}
