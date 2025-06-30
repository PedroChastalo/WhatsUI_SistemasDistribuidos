"""User domain model."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Optional


@dataclass
class User:
    userId: str
    username: str
    displayName: str
    email: str
    passwordHash: str

    # Runtime attributes
    status: str = "offline"
    lastSeen: Optional[int] = None  # epoch ms
    profilePicture: Optional[str] = None
