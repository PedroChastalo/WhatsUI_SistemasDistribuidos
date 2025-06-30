package br.com.whatsut.dao;

import br.com.whatsut.util.DataPersistenceUtil;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PendingJoinRequestDAO {
    private static final String FILE = "pending_join_requests";
    // adminId -> List<JoinRequest>
    private Map<String, List<JoinRequest>> pendingRequests;

    public PendingJoinRequestDAO() {
        loadData();
    }

    private void loadData() {
        pendingRequests = DataPersistenceUtil.loadData(FILE, 
            new com.fasterxml.jackson.core.type.TypeReference<Map<String, List<JoinRequest>>>() {}, 
            new ConcurrentHashMap<>());
    }

    public synchronized void addRequest(String adminId, JoinRequest req) {
        pendingRequests.computeIfAbsent(adminId, k -> new ArrayList<>()).add(req);
        saveData();
    }

    public synchronized List<JoinRequest> getAndRemoveRequests(String adminId) {
        List<JoinRequest> list = pendingRequests.remove(adminId);
        saveData();
        return list != null ? list : new ArrayList<>();
    }

    private void saveData() {
        DataPersistenceUtil.saveData(FILE, pendingRequests);
    }

    public static class JoinRequest {
        public String groupId;
        public String userId;
        public String userName;

        public JoinRequest() {}
        public JoinRequest(String groupId, String userId, String userName) {
            this.groupId = groupId;
            this.userId = userId;
            this.userName = userName;
        }
    }
}