import threading
import Pyro5.api
import Pyro5.server

from app.rmi.auth_service import AuthService
from app.rmi.message_service import MessageService
from app.rmi.group_service import GroupService
from app.rmi.user_service import UserService
from app.rmi.notification_service import NotificationService

def start_websocket_server():
    import asyncio
    import websockets
    from app.websocket.server import handler

    host = "localhost"
    port = 8765
    print(f"Iniciando WebSocket server em ws://{host}:{port}")

    async def run():
        try:
            async with websockets.serve(handler, host, port):
                print("WebSocket server rodando!")
                await asyncio.Future()  # run forever
        except Exception as e:
            print(f"Erro ao iniciar WebSocket server: {e}")

    try:
        asyncio.run(run())
    except Exception as e:
        print(f"Erro no loop do WebSocket: {e}")

def main():
    # Inicia o WebSocket em uma thread separada
    ws_thread = threading.Thread(target=start_websocket_server, daemon=True)
    ws_thread.start()

    # Pyro5 na thread principal
    daemon = Pyro5.server.Daemon()
    ns = Pyro5.api.locate_ns()

    services = {
        "auth": AuthService(),
        "message": MessageService(),
        "group": GroupService(),
        "user": UserService(),
        "notification": NotificationService(),
    }

    for name, obj in services.items():
        uri = daemon.register(obj)
        ns.register(f"whatsui.{name}", uri)
        print(f"Serviço '{name}' registrado em {uri}")

    print("Todos os serviços registrados. Aguardando requisições ...")
    daemon.requestLoop()

if __name__ == "__main__":
    main()