# Serviço de autenticação com hash de senha e controle de sessão
import Pyro5.api
import uuid
import hashlib
import json
import os

from app.utils.storage import load_json, save_json

USERS_FILE = "app/data/users.json"
SESSIONS_FILE = "app/data/sessions.json"

@Pyro5.api.expose
class AuthService:
    def __init__(self):
        self.users = load_json(USERS_FILE)
        self.sessions = load_json(SESSIONS_FILE)

    def _hash_password(self, password):
        return hashlib.sha256(password.encode()).hexdigest()

    def login(self, email, password):
        for user in self.users:
            if user["email"] == email and user["passwordHash"] == self._hash_password(password):
                session_id = str(uuid.uuid4())
                self.sessions[session_id] = {"userId": user["userId"], "email": user["email"]}
                save_json(SESSIONS_FILE, self.sessions)
                user_copy = user.copy()
                user_copy.pop("passwordHash", None)
                return {
                    "success": True,
                    "sessionId": session_id,
                    "userId": user["userId"],
                    "user": user_copy
                }
        return {"success": False, "error": "Credenciais inválidas"}

    def logout(self, sessionId):
        if sessionId in self.sessions:
            del self.sessions[sessionId]
            save_json(SESSIONS_FILE, self.sessions)
            return True
        return False

    def register(self, userData):
        # Verifica se o e-mail já existe
        if any(u["email"] == userData["email"] for u in self.users):
            return {"success": False, "error": "E-mail já cadastrado"}

        user_id = str(uuid.uuid4())
        new_user = {
            "userId": user_id,
            "username": userData["username"],
            "displayName": userData.get("displayName", userData["username"]),
            "email": userData["email"],
            "passwordHash": self._hash_password(userData["password"]),
            "status": "offline",
            "lastSeen": None,
            "profilePicture": None
        }

        self.users.append(new_user)
        save_json(USERS_FILE, self.users)
        return {"success": True, "userId": user_id}

    def validateSession(self, sessionId):
        return sessionId in self.sessions
