# Serviço de usuários: listagem, status e atualização de perfil
import Pyro5.api
from app.utils.storage import load_json, save_json

USERS_FILE = "app/data/users.json"

@Pyro5.api.expose
class UserService:
    def __init__(self):
        self.users = load_json(USERS_FILE)

    def getUsers(self, sessionId):
        return [{"userId": u["userId"], "username": u["username"], "status": u.get("status", "offline")} for u in self.users]

    def getUserById(self, sessionId, userId):
        return next((u for u in self.users if u["userId"] == userId), None)

    def updateStatus(self, sessionId, status):
        for user in self.users:
            if user["userId"] == sessionId:
                user["status"] = status
                save_json(USERS_FILE, self.users)
                return True
        return False

    def updateProfile(self, sessionId, profileData):
        for user in self.users:
            if user["userId"] == sessionId:
                user.update({
                    "displayName": profileData.get("displayName", user["username"]),
                    "profilePicture": profileData.get("profilePicture", user.get("profilePicture"))
                })
                save_json(USERS_FILE, self.users)
                return True
        return False
