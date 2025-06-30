"""Group domain model."""
from __future__ import annotations

from dataclasses import dataclass, field
import time


def _now_ms() -> int:
    return int(time.time() * 1000)


@dataclass
class Group:
    groupId: str
    name: str
    description: str
    creatorId: str
    createdAt: int = field(default_factory=_now_ms)
    lastMessage: str = ""
    timestamp: int = field(default_factory=_now_ms)
