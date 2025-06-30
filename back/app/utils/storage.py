import json
import os

def load_json(path):
    if not os.path.exists(path):
        # Se for arquivo de mensagens (private/group), retorna lista
        if "messages/private" in path or "messages/group" in path:
            return []
        # Para outros arquivos .json, retorna dict
        return {}
    with open(path, 'r', encoding='utf-8') as f:
        return json.load(f)

def save_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
