package br.com.whatsut.dao;

import br.com.whatsut.util.ConfigManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementação base para DAOs que utilizam arquivos JSON para persistência.
 *
 * @param <T> Tipo da entidade
 * @param <ID> Tipo do identificador da entidade
 */
public abstract class AbstractJsonDAO<T, ID> implements DAO<T, ID> {
    private static final Logger logger = Logger.getLogger(AbstractJsonDAO.class.getName());
    protected final ObjectMapper objectMapper;
    protected final String filePath;
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * Construtor que inicializa o DAO com o caminho do arquivo JSON.
     *
     * @param fileName Nome do arquivo JSON
     */
    public AbstractJsonDAO(String fileName) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        String basePath = ConfigManager.getProperty("storage.base.path", "data");
        this.filePath = basePath + File.separator + fileName;
        
        // Garantir que o diretório exista
        Path directory = Paths.get(basePath);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao criar diretório para persistência", e);
        }
        
        // Criar arquivo se não existir
        File file = new File(filePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
                // Inicializar com uma lista vazia
                objectMapper.writeValue(file, new ArrayList<>());
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Erro ao criar arquivo de persistência", e);
            }
        }
    }
    
    /**
     * Obtém o ID de uma entidade.
     *
     * @param entity Entidade
     * @return ID da entidade
     */
    protected abstract ID getId(T entity);
    
    /**
     * Carrega todas as entidades do arquivo JSON.
     *
     * @return Lista de entidades
     */
    protected List<T> loadAll() {
        lock.readLock().lock();
        try {
            File file = new File(filePath);
            if (file.length() == 0) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(file, getTypeReference());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao carregar entidades do arquivo: " + filePath, e);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Salva todas as entidades no arquivo JSON.
     *
     * @param entities Lista de entidades
     * @return true se salvo com sucesso, false caso contrário
     */
    protected boolean saveAll(List<T> entities) {
        lock.writeLock().lock();
        try {
            File file = new File(filePath);
            objectMapper.writeValue(file, entities);
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Erro ao salvar entidades no arquivo: " + filePath, e);
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Obtém a referência de tipo para deserialização.
     *
     * @return TypeReference para a lista de entidades
     */
    protected abstract TypeReference<List<T>> getTypeReference();
    
    @Override
    public List<T> findAll() {
        return loadAll();
    }
    
    @Override
    public T findById(ID id) {
        List<T> entities = loadAll();
        for (T entity : entities) {
            if (getId(entity).equals(id)) {
                return entity;
            }
        }
        return null;
    }
    
    @Override
    public boolean save(T entity) {
        List<T> entities = loadAll();
        ID id = getId(entity);
        
        // Remover entidade existente com mesmo ID
        entities.removeIf(e -> getId(e).equals(id));
        
        // Adicionar nova entidade
        entities.add(entity);
        
        return saveAll(entities);
    }
    
    @Override
    public boolean delete(ID id) {
        List<T> entities = loadAll();
        boolean removed = entities.removeIf(e -> getId(e).equals(id));
        
        if (removed) {
            return saveAll(entities);
        }
        
        return false;
    }
}
