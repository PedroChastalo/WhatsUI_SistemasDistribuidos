package br.com.whatsut.service;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface AuthenticationService extends Remote {
    Map<String, Object> login(String email, String password) throws RemoteException;
    Map<String, Object> register(String username, String email, String displayName, String password) throws RemoteException;
    boolean logout(String sessionId) throws RemoteException;
    boolean validateSession(String sessionId) throws RemoteException;
}
