"""Message domain model."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Optional
import time


def _now_ms() -> int:
    return int(time.time() * 1000)


@dataclass
class Message:
    messageId: str
    senderId: str
    content: str
    timestamp: int = field(default_factory=_now_ms)

    # Either receiverId (private) or groupId (group)
    receiverId: Optional[str] = None
    groupId: Optional[str] = None

    type: str = "user"  # or "group"
    status: str = "sent"
