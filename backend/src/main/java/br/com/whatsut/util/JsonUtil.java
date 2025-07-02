package br.com.whatsut.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário para operações com JSON
 */
public class JsonUtil {
    // Instância do ObjectMapper para serialização/deserialização
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    // Bloco estático para configuração do ObjectMapper
    static {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }
    
    /**
     * Converte um objeto para uma string JSON
     * @param object Objeto a ser convertido
     * @return String JSON
     */
    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter objeto para JSON", e);
        }
    }
    
    /**
     * Converte uma string JSON para um objeto
     * @param json String JSON
     * @param clazz Classe do objeto
     * @param <T> Tipo do objeto
     * @return Objeto convertido
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao converter JSON para objeto", e);
        }
    }
    
    /**
     * Cria uma resposta de sucesso padronizada
     * @param data Dados a serem retornados
     * @return Mapa com sucesso e dados
     */
    public static Map<String, Object> createSuccessResponse(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        return response;
    }
    
    /**
     * Cria uma resposta de erro padronizada
     * @param errorMessage Mensagem de erro
     * @return Mapa com erro
     */
    public static Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", errorMessage);
        return response;
    }
}
