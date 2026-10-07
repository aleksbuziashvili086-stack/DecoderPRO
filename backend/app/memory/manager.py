from app.database.database import connect

def remember(user_id: str, fact: str) -> None:
    with connect() as conn:
        conn.execute("INSERT INTO memories(user_id, fact) VALUES (?, ?)", (user_id, fact[:500]))

def recall(user_id: str, limit: int = 8) -> list[str]:
    with connect() as conn:
        rows = conn.execute("SELECT fact FROM memories WHERE user_id = ? ORDER BY id DESC LIMIT ?", (user_id, limit)).fetchall()
    return [row["fact"] for row in rows]

def save_chunks(user_id: str, name: str, chunks: list[str]) -> None:
    with connect() as conn:
        conn.executemany("INSERT INTO documents(user_id, name, chunk) VALUES (?, ?, ?)", [(user_id, name, chunk[:2000]) for chunk in chunks if chunk.strip()])

def search_docs(user_id: str, query: str, limit: int = 4) -> list[str]:
    words = [w for w in query.split() if len(w) > 2][:6]
    if not words:
        return []
    clause = " OR ".join("chunk LIKE ?" for _ in words)
    params = [user_id] + [f"%{w}%" for w in words]
    with connect() as conn:
        rows = conn.execute(f"SELECT chunk FROM documents WHERE user_id = ? AND ({clause}) LIMIT ?", params + [limit]).fetchall()
    return [row["chunk"] for row in rows]

def save_chat(user_id: str, chat_id: str, title: str, messages: list[dict]) -> None:
    with connect() as conn:
        conn.execute("INSERT INTO chats(id, user_id, title) VALUES (?, ?, ?) ON CONFLICT(id) DO UPDATE SET title = excluded.title, updated_at = CURRENT_TIMESTAMP", (chat_id, user_id, title[:80]))
        conn.execute("DELETE FROM messages WHERE chat_id = ?", (chat_id,))
        conn.executemany("INSERT INTO messages(chat_id, role, text) VALUES (?, ?, ?)", [(chat_id, m.get("role", "user"), m.get("text", "")[:8000]) for m in messages])
