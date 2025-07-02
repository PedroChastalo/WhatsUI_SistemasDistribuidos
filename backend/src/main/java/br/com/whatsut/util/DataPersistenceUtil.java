package br.com.whatsut.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilitário para persistência de dados em arquivos JSON
 */
public class DataPersistenceUtil {
    // Instância do ObjectMapper para serialização/deserialização
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    // Diretório padrão para armazenamento dos arquivos de dados
    private static final String DATA_DIR = "data";
    
    // Bloco estático para garantir que o diretório de dados exista
    static {
        // Criar diretório de dados se não existir
        try {
            Path dataDir = Paths.get(DATA_DIR);
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                System.out.println("Diretório de dados criado: " + dataDir.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("Erro ao criar diretório de dados: " + e.getMessage());
        }
    }
    
    /**
     * Salva dados em um arquivo JSON
     * @param filename Nome do arquivo (sem extensão)
     * @param data Dados a serem salvos
     * @param <T> Tipo dos dados
     */
    public static <T> void saveData(String filename, T data) {
        try {
            File file = new File(DATA_DIR, filename + ".json");
            objectMapper.writeValue(file, data);
            System.out.println("Dados salvos em: " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Erro ao salvar dados em " + filename + ".json: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carrega dados de um arquivo JSON
     * @param filename Nome do arquivo (sem extensão)
     * @param typeReference Referência do tipo para deserialização
     * @param defaultValue Valor padrão caso o arquivo não exista
     * @param <T> Tipo dos dados
     * @return Dados carregados ou valor padrão
     */
    public static <T> T loadData(String filename, TypeReference<T> typeReference, T defaultValue) {
        File file = new File(DATA_DIR, filename + ".json");
        if (!file.exists()) {
            System.out.println("Arquivo " + filename + ".json não encontrado. Usando valores padrão.");
            return defaultValue;
        }
        
        try {
            return objectMapper.readValue(file, typeReference);
        } catch (IOException e) {
            System.err.println("Erro ao carregar dados de " + filename + ".json: " + e.getMessage());
            e.printStackTrace();
            return defaultValue;
        }
    }
    
    /**
     * Carrega um mapa de dados de um arquivo JSON
     * @param filename Nome do arquivo (sem extensão)
     * @param <K> Tipo da chave
     * @param <V> Tipo do valor
     * @return Mapa carregado ou mapa vazio
     */
    public static <K, V> Map<K, V> loadMap(String filename, Class<K> keyClass, Class<V> valueClass) {
        TypeReference<Map<K, V>> typeRef = new TypeReference<Map<K, V>>() {};
        Map<K, V> result = loadData(filename, typeRef, new HashMap<>());
        
        // Converter para ConcurrentHashMap para thread-safety
        return new ConcurrentHashMap<>(result);
    }
}
