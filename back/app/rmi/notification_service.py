"""Serviço de notificações.
Futuramente tratará de eventos como novas mensagens, convites de grupo, etc.
Por enquanto apenas expõe um método ping para teste.
"""
import Pyro5.api


@Pyro5.api.expose
class NotificationService:  # pragma: no cover
    def ping(self) -> str:
        """Retorna 'pong' para verificação de conectividade."""
        return "pong"
