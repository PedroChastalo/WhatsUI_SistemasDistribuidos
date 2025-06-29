package br.com.whatsut.dao;

import java.util.List;

/**
 * Interface genérica para operações de persistência.
 *
 * @param <T> Tipo da entidade
 * @param <ID> Tipo do identificador da entidade
 */
public interface DAO<T, ID> {
    
    /**
     * Busca uma entidade pelo ID.
     *
     * @param id ID da entidade
     * @return A entidade encontrada ou null se não existir
     */
    T findById(ID id);
    
    /**
     * Lista todas as entidades.
     *
     * @return Lista de entidades
     */
    List<T> findAll();
    
    /**
     * Salva uma entidade (cria ou atualiza).
     *
     * @param entity Entidade a ser salva
     * @return true se salvo com sucesso, false caso contrário
     */
    boolean save(T entity);
    
    /**
     * Exclui uma entidade pelo ID.
     *
     * @param id ID da entidade a ser excluída
     * @return true se excluída com sucesso, false caso contrário
     */
    boolean delete(ID id);
}
