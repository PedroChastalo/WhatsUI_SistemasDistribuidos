"""Simple WebSocket echo server that can later forward events to connected web clients.
The implementation uses *websockets* library for simplicity. This module is **optional** for
running the RMI services but is useful for real-time push notifications.

Run with: `python -m app.websocket.server`
"""
from __future__ import annotations

import asyncio
import json
import logging
from typing import Set
import datetime

import websockets
from websockets.server import WebSocketServerProtocol

from app.rmi.auth_service import AuthService  # Importa o AuthService
from app.rmi.user_service import UserService
from app.rmi.group_service import GroupService
from app.rmi.message_service import MessageService

logging.basicConfig(level=logging.INFO)

def send_response(ws: WebSocketServerProtocol, msg_type: str, request_id: str | None, success: bool = True, data: dict | None = None, error: str | None = None):
    """Utility to send a standard response payload expected by the frontend."""
    payload = {
        "type": msg_type,
        "requestId": request_id,
        "success": success,
    }
    if success:
        payload["data"] = data or {}
    else:
        payload["error"] = error or "unknown_error"
    return asyncio.create_task(ws.send(json.dumps(payload)))

connected: Set[WebSocketServerProtocol] = set()
auth_service = AuthService()
user_service = UserService()
group_service = GroupService()
message_service = MessageService()

def _get_user_id(session_id: str):
    """Map a sessionId to the corresponding userId using AuthService's session store."""
    session = auth_service.sessions.get(session_id)
    if session:
        return session.get("userId")
    return None

async def handler(ws: WebSocketServerProtocol):
    connected.add(ws)
    logging.info("Client connected: %s", ws.remote_address)
    try:
        async for message in ws:
            logging.info("Mensagem recebida de %s: %s", ws.remote_address, message)
            try:
                data = json.loads(message)
            except Exception:
                logging.error("JSON inválido recebido de %s: %s", ws.remote_address, message)
                await ws.send(json.dumps({"type": "error", "error": "invalid_json"}))
                continue

            msg_type = data.get("type")
            request_id = data.get("requestId")
            logging.info("Tipo de mensagem: %s | requestId: %s", msg_type, request_id)

            if msg_type == "ping":
                logging.info("Respondendo pong para %s", ws.remote_address)
                await ws.send(json.dumps({"type": "pong"}))
            elif msg_type == "register":
                user_data = data.get("data", {})
                logging.info("Tentando registrar usuário: %s", user_data)
                # Verifica campos obrigatórios
                if not all(k in user_data for k in ("username", "email", "password")):
                    logging.warning("Campos obrigatórios ausentes em userData: %s", user_data)
                    await ws.send(json.dumps({
                        "type": "register",
                        "requestId": request_id,
                        "success": False,
                        "error": "Campos obrigatórios ausentes em userData"
                    }))
                    continue
                result = auth_service.register(user_data)
                logging.info("Resultado do registro: %s", result)
                await ws.send(json.dumps({
                    "type": "register",
                    "requestId": request_id,
                    **result
                }))
            elif msg_type == "login":
                email = data["data"].get("email")
                password = data["data"].get("password")
                logging.info("Tentando login para: %s", email)
                result = auth_service.login(email, password)
                logging.info("Resultado do login: %s", result)
                await send_response(ws, "login", request_id, success=result.get("success", False), data={k: v for k, v in result.items() if k not in ("success",)} , error=result.get("error"))
            elif msg_type == "logout":
                session_id = data["data"].get("sessionId")
                success = auth_service.logout(session_id)
                await send_response(ws, "logout", request_id, success=success)
            elif msg_type == "getUsers":
                session_id = data["data"].get("sessionId")
                user_id = _get_user_id(session_id)
                if not user_id:
                    await ws.send(json.dumps({"type": "getUsers", "requestId": request_id, "success": False, "error": "Sessão inválida"}))
                    continue
                users = user_service.getUsers(user_id)
                await send_response(ws, "getUsers", request_id, data={"users": users})
            elif msg_type == "updateStatus":
                session_id = data["data"].get("sessionId")
                status = data["data"].get("status")
                user_id = _get_user_id(session_id)
                success = user_service.updateStatus(user_id, status)
                await send_response(ws, "updateStatus", request_id, success=success)
            elif msg_type == "getPrivateConversations":
                session_id = data["data"].get("sessionId")
                user_id = _get_user_id(session_id)
                convs = message_service.getPrivateConversations(user_id)
                # Monta lista no formato esperado pelo frontend
                conversations = []
                for conv in convs:
                    other_user_id = conv.get("with")
                    # Busca usuário pelo id
                    other_user = None
                    for u in user_service.users:
                        if u["userId"] == other_user_id:
                            other_user = u
                            break
                    username = other_user["username"] if other_user else other_user_id
                    display_name = other_user.get("displayName", username) if other_user else username

                    # Busca última mensagem
                    messages = []
                    try:
                        messages = message_service.getPrivateMessages(user_id, other_user_id, limit=1)
                    except Exception:
                        pass
                    last_msg = messages[-1] if messages else None
                    last_message = last_msg["content"] if last_msg else ""
                    # timestamp: ISO 8601
                    if last_msg and isinstance(last_msg.get("timestamp"), int):
                        ts = datetime.datetime.utcfromtimestamp(last_msg["timestamp"]/1000.0).isoformat() + "Z"
                    else:
                        ts = None

                    conversations.append({
                        "userId": other_user_id,
                        "username": username,
                        "displayName": display_name,
                        "lastMessage": last_message,
                        "timestamp": ts,
                        "unreadCount": 0
                    })
                await send_response(ws, "getPrivateConversations", request_id, data={"conversations": conversations})
            elif msg_type == "getPrivateMessages":
                session_id = data["data"].get("sessionId")
                other_user_id = data["data"].get("otherUserId")
                limit = data["data"].get("limit", 50)
                user_id = _get_user_id(session_id)
                # Passe user_id (não session_id) para o serviço
                messages = message_service.getPrivateMessages(user_id, other_user_id, limit)
                # Enriquecer mensagens com senderName e timestamp ISO
                def get_name(uid):
                    user = next((u for u in user_service.users if u["userId"] == uid), None)
                    return user["displayName"] if user and "displayName" in user else (user["username"] if user else uid)
                formatted = []
                for m in messages:
                    formatted.append({
                        "messageId": m["messageId"],
                        "senderId": m["senderId"],
                        "senderName": get_name(m["senderId"]),
                        "content": m["content"],
                        "timestamp": (
                            datetime.datetime.utcfromtimestamp(m["timestamp"]/1000.0).isoformat() + "Z"
                            if isinstance(m.get("timestamp"), int) else None
                        ),
                        "status": m.get("status", "sent")
                    })
                await send_response(ws, "getPrivateMessages", request_id, data={"messages": formatted})

            elif msg_type == "sendPrivateMessage":
                session_id = data["data"].get("sessionId")
                receiver_id = data["data"].get("receiverId")
                content = data["data"].get("content")
                user_id = _get_user_id(session_id)
                # Passe user_id (não session_id) para o serviço
                msg = message_service.sendPrivateMessage(user_id, receiver_id, content)
                # Adiciona senderName e timestamp ISO
                sender_name = "Você"
                user = next((u for u in user_service.users if u["userId"] == user_id), None)
                if user:
                    sender_name = user.get("displayName") or user.get("username") or "Você"
                formatted = {
                    "messageId": msg["messageId"],
                    "senderId": msg["senderId"],
                    "senderName": sender_name,
                    "content": msg["content"],
                    "timestamp": (
                        datetime.datetime.utcfromtimestamp(msg["timestamp"]/1000.0).isoformat() + "Z"
                        if isinstance(msg.get("timestamp"), int) else None
                    ),
                    "status": msg.get("status", "sent")
                }
                await send_response(ws, "sendPrivateMessage", request_id, data=formatted)
            elif msg_type == "getGroups":
                session_id = data["data"].get("sessionId")
                user_id = _get_user_id(session_id)
                groups = group_service.getGroups(user_id)
                await send_response(ws, "getGroups", request_id, data={"groups": groups})
            elif msg_type == "getGroupMembers":
                session_id = data["data"].get("sessionId")
                group_id = data["data"].get("groupId")
                user_id = _get_user_id(session_id)
                members = group_service.getGroupMembers(user_id, group_id)
                await send_response(ws, "getGroupMembers", request_id, data={"members": members})
            elif msg_type == "createGroup":
                session_id = data["data"].get("sessionId")
                group_data = data["data"].get("groupData", {})
                user_id = _get_user_id(session_id)
                group = group_service.createGroup(user_id, group_data)
                await send_response(ws, "createGroup", request_id, data={"group": group})
            elif msg_type == "getGroupMessages":
                session_id = data["data"].get("sessionId")
                group_id = data["data"].get("groupId")
                limit = data["data"].get("limit", 50)
                user_id = _get_user_id(session_id)
                msgs = message_service.getGroupMessages(user_id, group_id, limit)
                await send_response(ws, "getGroupMessages", request_id, data={"messages": msgs})
            elif msg_type == "sendGroupMessage":
                session_id = data["data"].get("sessionId")
                group_id = data["data"].get("groupId")
                content = data["data"].get("content")
                user_id = _get_user_id(session_id)
                msg = message_service.sendGroupMessage(user_id, group_id, content)
                await send_response(ws, "sendGroupMessage", request_id, data=msg)
            elif msg_type == "addUserToGroup":
                session_id = data["data"].get("sessionId")
                group_id = data["data"].get("groupId")
                user_id_to_add = data["data"].get("userId")
                user_id = _get_user_id(session_id)
                res = group_service.addUserToGroup(user_id, group_id, user_id_to_add)
                await send_response(ws, "addUserToGroup", request_id, success=res)
            elif msg_type == "removeUserFromGroup":
                session_id = data["data"].get("sessionId")
                group_id = data["data"].get("groupId")
                user_id_to_remove = data["data"].get("userId")
                user_id = _get_user_id(session_id)
                res = group_service.removeUserFromGroup(user_id, group_id, user_id_to_remove)
                await send_response(ws, "removeUserFromGroup", request_id, success=res)
            elif msg_type == "setGroupAdmin":
                session_id = data["data"].get("sessionId")
                group_id = data["data"].get("groupId")
                new_admin_id = data["data"].get("userId")
                user_id = _get_user_id(session_id)
                res = group_service.setGroupAdmin(user_id, group_id, new_admin_id)
                await send_response(ws, "setGroupAdmin", request_id, success=res)
            elif msg_type == "leaveGroup":
                session_id = data["data"].get("sessionId")
                group_id = data["data"].get("groupId")
                delete_if_admin = data["data"].get("deleteIfAdmin", False)
                user_id = _get_user_id(session_id)
                res = group_service.leaveGroup(user_id, group_id, delete_if_admin)
                await send_response(ws, "leaveGroup", request_id, success=res)
            elif msg_type == "deleteGroup":
                session_id = data["data"].get("sessionId")
                group_id = data["data"].get("groupId")
                user_id = _get_user_id(session_id)
                res = group_service.deleteGroup(user_id, group_id)
                await send_response(ws, "deleteGroup", request_id, success=res)
            elif msg_type == "get_conversations":
                # Espera-se: data: { sessionId }
                session_id = data.get("data", {}).get("sessionId")
                user_id = _get_user_id(session_id)
                if not user_id:
                    await ws.send(json.dumps({
                        "type": "conversations",
                        "data": [],
                        "error": "Sessão inválida"
                    }))
                    continue

                # Busca conversas privadas
                private_convs = message_service.getPrivateConversations(user_id)
                # Monta lista no formato esperado
                conv_list = []
                for conv in private_convs:
                    # conv: {"conversationId": ..., "with": ...}
                    # Busca nome do usuário
                    other_user_id = conv.get("with")
                    # Busca usuário pelo id
                    other_user = None
                    for u in user_service.users:
                        if u["userId"] == other_user_id:
                            other_user = u
                            break
                    name = other_user["username"] if other_user else other_user_id

                    # Busca última mensagem
                    path = message_service._get_private_path(user_id, other_user_id)
                    messages = []
                    try:
                        messages = message_service.getPrivateMessages(user_id, other_user_id, limit=1)
                    except Exception:
                        pass
                    last_msg = messages[-1] if messages else None
                    last_message = last_msg["content"] if last_msg else ""
                    # timestamp: ISO 8601
                    if last_msg and isinstance(last_msg.get("timestamp"), int):
                        ts = datetime.datetime.utcfromtimestamp(last_msg["timestamp"]/1000.0).isoformat() + "Z"
                    else:
                        ts = None

                    conv_list.append({
                        "id": conv["conversationId"],
                        "name": name,
                        "lastMessage": last_message,
                        "timestamp": ts
                    })

                await ws.send(json.dumps({
                    "type": "conversations",
                    "data": conv_list
                }))
            else:
                logging.warning("Tipo de mensagem desconhecido: %s", msg_type)
                await ws.send(json.dumps({
                    "type": "error",
                    "error": f"Unknown type: {msg_type}",
                    "requestId": request_id
                }))
    except websockets.exceptions.ConnectionClosed:
        logging.info("Conexão fechada: %s", ws.remote_address)
    finally:
        connected.discard(ws)
        logging.info("Client disconnected: %s", ws.remote_address)

def main(host: str = "localhost", port: int = 8765):
    logging.info("Starting WebSocket server at ws://%s:%d", host, port)

    async def start():
        async with websockets.serve(handler, host, port):
            await asyncio.Future()  # run forever

    asyncio.run(start())

if __name__ == "__main__":
    main()