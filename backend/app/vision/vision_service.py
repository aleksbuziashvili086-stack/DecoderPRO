import base64
import httpx
from app.config.settings import GEMINI_API_KEY, GEMINI_MODEL

async def ask(system: str, messages: list[dict], model: str | None = None, image_b64: str | None = None, mime: str = "image/jpeg") -> str:
    if not GEMINI_API_KEY:
        return "სერვერზე GEMINI_API_KEY არ წერია. ჩაწერე backend/.env-ში."
    contents = []
    for msg in messages:
        parts = [{"text": msg.get("text", "")}]
        if image_b64 and msg is messages[-1]:
            parts.append({"inline_data": {"mime_type": mime, "data": image_b64}})
        contents.append({"role": "model" if msg.get("role") == "assistant" else "user", "parts": parts})
    body = {"systemInstruction": {"parts": [{"text": system}]}, "contents": contents, "generationConfig": {"temperature": 0.6}}
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model or GEMINI_MODEL}:generateContent"
    async with httpx.AsyncClient(timeout=60) as client:
        response = await client.post(url, headers={"x-goog-api-key": GEMINI_API_KEY}, json=body)
        if response.status_code >= 400:
            return f"მოდელმა უარი თქვა ({response.status_code})."
        data = response.json()
    parts = data.get("candidates", [{}])[0].get("content", {}).get("parts", [])
    return "\n".join(part.get("text", "") for part in parts).strip() or "პასუხი ცარიელია."

def encode_image(data: bytes) -> str:
    return base64.b64encode(data).decode("ascii")
