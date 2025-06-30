"""Re-export das *dataclasses* de domínio.
Os modelos são definidos em arquivos separados para corresponder à estrutura documentada no README.
Importe daqui para evitar caminhos longos nos demais módulos.
"""
from __future__ import annotations



from .user import User
from .group import Group
from .message import Message
from .session import Session

__all__ = [
    "User",
    "Group",
    "Message",
    "Session",
]















