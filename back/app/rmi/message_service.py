# Serviço de mensagens privadas e em grupo
import Pyro5.api
import uuid
import time
import os
from app.utils.storage import load_json, save_json


USERS_FILE = "app/data/users.json"
PRIVATE_DIR = "app/data/messages/private"
GROUP_DIR = "app/data/messages/group"

@Pyro5.api.expose
class MessageService:
    def __init__(self):
        os.makedirs(PRIVATE_DIR, exist_ok=True)
        os.makedirs(GROUP_DIR, exist_ok=True)
        self.users = load_json(USERS_FILE)

    def _get_private_path(self, user1, user2):
        users = sorted([user1, user2])
        return f"{PRIVATE_DIR}/{users[0]}_{users[1]}.json"

    def _get_group_path(self, groupId):
        return f"{GROUP_DIR}/{groupId}.json"

    def getPrivateMessages(self, userId, otherUserId, limit=50):
        path = self._get_private_path(userId, otherUserId)
        messages = load_json(path)
        if not isinstance(messages, list):
            messages = []
        return messages[-limit:]

    def getGroupMessages(self, sessionId, groupId, limit=50):
        path = self._get_group_path(groupId)
        messages = load_json(path)
        return messages[-limit:]

    def sendPrivateMessage(self, userId, receiverId, content):
        path = self._get_private_path(userId, receiverId)
        messages = load_json(path)
        if not isinstance(messages, list):
            messages = []
        new_message = {
            "messageId": str(uuid.uuid4()),
            "senderId": userId,
            "receiverId": receiverId,
            "content": content,
            "timestamp": int(time.time() * 1000),
            "type": "user",
            "status": "sent"
        }
        messages.append(new_message)
        save_json(path, messages)
        return new_message

    def sendGroupMessage(self, sessionId, groupId, content):
        path = self._get_group_path(groupId)
        messages = load_json(path)

        new_message = {
            "messageId": str(uuid.uuid4()),
            "senderId": sessionId,
            "groupId": groupId,
            "content": content,
            "timestamp": int(time.time() * 1000),
            "type": "group",
            "status": "sent"
        }

        messages.append(new_message)
        save_json(path, messages)
        return new_message

    def getPrivateConversations(self, sessionId):
        # Lista todos os arquivos no diretório privado e extrai com quem são as conversas
        convs = []
        for filename in os.listdir(PRIVATE_DIR):
            if sessionId in filename:
                users = filename.replace(".json", "").split("_")
                other = users[0] if users[1] == sessionId else users[1]
                convs.append({"conversationId": filename, "with": other})
        return convs

