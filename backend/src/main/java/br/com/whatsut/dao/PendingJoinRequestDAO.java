package br.com.whatsut.dao;

import br.com.whatsut.util.DataPersistenceUtil;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DAO para gerenciar solicitações de entrada em grupos
 */
public class PendingJoinRequestDAO {
    /** Nome do arquivo JSON onde as solicitações pendentes são persistidas */
    private static final String FILE = "pending_join_requests";
    /**
     * Estrutura em memória: chave é o ID do admin e o valor é a lista de
     * solicitações pendentes direcionadas a ele.
     */
    private Map<String, List<JoinRequest>> pendingRequests;

    public PendingJoinRequestDAO() {
        loadData();
    }

    /**
     * Remove uma solicitação específica (identificada por <code>groupId</code> e
     * <code>userId</code>) da fila de pendências de um admin.
     *
     * @param adminId  ID do administrador proprietário da fila
     * @param groupId  ID do grupo alvo
     * @param userId   ID do usuário solicitante
     * @return <code>true</code> se a solicitação foi removida, caso contrário
     *         <code>false</code>
     */
    public synchronized boolean removeRequest(String adminId, String groupId, String userId) {
        List<JoinRequest> list = pendingRequests.get(adminId);
        if (list == null) return false;
        boolean removed = list.removeIf(req -> req.groupId.equals(groupId) && req.userId.equals(userId));
        if (removed) saveData();
        return removed;
    }

    /** Carrega as pendências do disco para memória, criando estrutura vazia caso não exista. */
    private void loadData() {
        pendingRequests = DataPersistenceUtil.loadData(FILE, 
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, List<JoinRequest>>>() {}, 
            new ConcurrentHashMap<>());
    }

    /**
     * Obtém uma cópia da lista de solicitações pendentes para determinado admin.
     *
     * @param adminId ID do administrador
     * @return Lista de solicitações (pode estar vazia, nunca <code>null</code>)
     */
    public synchronized List<JoinRequest> getRequests(String adminId) {
        List<JoinRequest> list = pendingRequests.get(adminId);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    /**
     * Adiciona uma nova solicitação à fila do admin, evitando duplicatas.
     *
     * @param adminId ID do administrador
     * @param req     Objeto <code>JoinRequest</code> a adicionar
     * @return <code>true</code> se adicionada com sucesso, <code>false</code> se já existia
     */
    public synchronized boolean addRequest(String adminId, JoinRequest req) {
        List<JoinRequest> list = pendingRequests.computeIfAbsent(adminId, k -> new ArrayList<>());
        boolean exists = list.stream().anyMatch(r -> r.groupId.equals(req.groupId) && r.userId.equals(req.userId));
        if (exists) {
            return false; 
        }
        list.add(req);
        saveData();
        return true;
    }

    /**
     * Recupera todas as solicitações de um admin e limpa a fila correspondente.
     *
     * @param adminId ID do administrador
     * @return Lista que estava armazenada (pode estar vazia)
     */
    public synchronized List<JoinRequest> getAndRemoveRequests(String adminId) {
        List<JoinRequest> list = pendingRequests.remove(adminId);
        saveData();
        return list != null ? list : new ArrayList<>();
    }

    /** Persiste o mapa <code>pendingRequests</code> no disco em formato JSON. */
    private void saveData() {
        DataPersistenceUtil.saveData(FILE, pendingRequests);
    }

    /**
     * DTO simples representando uma solicitação de entrada em grupo aguardando decisão do admin.
     */
    public static class JoinRequest {
        /** ID do grupo ao qual o usuário deseja entrar */
        public String groupId;
        /** Nome do grupo (facilita exibição no cliente) */
        public String groupName;
        /** ID do usuário solicitante */
        public String userId;
        /** Nome a ser exibido do usuário solicitante */
        public String userName;

        /** Construtor padrão necessário para desserialização Jackson */
        public JoinRequest() {}
        /**
         * Construtor utilitário.
         */
        public JoinRequest(String groupId, String groupName, String userId, String userName) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.userId = userId;
            this.userName = userName;
        }
    }
}