# Serviço de gerenciamento de grupos: criação, entrada, saída e moderação
import Pyro5.api
import uuid
import time
from app.utils.storage import load_json, save_json

GROUPS_FILE = "app/data/groups.json"
MEMBERS_FILE = "app/data/group_members.json"

@Pyro5.api.expose
class GroupService:
    def __init__(self):
        self.groups = load_json(GROUPS_FILE)
        self.members = load_json(MEMBERS_FILE)

    def getGroups(self, sessionId):
        return self.groups

    def getGroupMembers(self, sessionId, groupId):
        return [m for m in self.members if m["groupId"] == groupId]

    def createGroup(self, sessionId, groupData):
        group_id = str(uuid.uuid4())
        group = {
            "groupId": group_id,
            "name": groupData["name"],
            "description": groupData.get("description", ""),
            "creatorId": sessionId,
            "createdAt": int(time.time()),
            "lastMessage": "",
            "timestamp": int(time.time())
        }

        member = {
            "userId": sessionId,
            "groupId": group_id,
            "role": "admin",
            "joinedAt": int(time.time())
        }

        self.groups.append(group)
        self.members.append(member)
        save_json(GROUPS_FILE, self.groups)
        save_json(MEMBERS_FILE, self.members)
        return group

    def addUserToGroup(self, sessionId, groupId, userId):
        self.members.append({
            "userId": userId,
            "groupId": groupId,
            "role": "member",
            "joinedAt": int(time.time())
        })
        save_json(MEMBERS_FILE, self.members)
        return True

    def removeUserFromGroup(self, sessionId, groupId, userId):
        self.members = [m for m in self.members if not (m["groupId"] == groupId and m["userId"] == userId)]
        save_json(MEMBERS_FILE, self.members)
        return True

    def setGroupAdmin(self, sessionId, groupId, userId):
        for m in self.members:
            if m["groupId"] == groupId and m["userId"] == userId:
                m["role"] = "admin"
        save_json(MEMBERS_FILE, self.members)
        return True

    def leaveGroup(self, sessionId, groupId, deleteIfAdmin=False):
        is_admin = any(m["userId"] == sessionId and m["groupId"] == groupId and m["role"] == "admin" for m in self.members)
        self.members = [m for m in self.members if not (m["groupId"] == groupId and m["userId"] == sessionId)]

        if deleteIfAdmin and is_admin:
            self.groups = [g for g in self.groups if g["groupId"] != groupId]
            self.members = [m for m in self.members if m["groupId"] != groupId]
            save_json(GROUPS_FILE, self.groups)
        save_json(MEMBERS_FILE, self.members)
        return True

    def deleteGroup(self, sessionId, groupId):
        self.groups = [g for g in self.groups if g["groupId"] != groupId]
        self.members = [m for m in self.members if m["groupId"] != groupId]
        save_json(GROUPS_FILE, self.groups)
        save_json(MEMBERS_FILE, self.members)
        return True
