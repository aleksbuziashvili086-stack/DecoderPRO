from fastapi import APIRouter, Depends
from pydantic import BaseModel
from app.auth.authentication import require_token
from app.ai.orchestrator import run

router = APIRouter()

class ChatIn(BaseModel):
    user_id: str = "local"
    chat_id: str = "local"
    task: str = "free"
    tone: str = "მეგობრული"
    messages: list[dict]
    attachment: str = ""
    image_b64: str | None = None

@router.post("/chat")
async def chat(body: ChatIn, _: str = Depends(require_token)):
    return await run(body.user_id, body.chat_id, body.task, body.tone, body.messages, body.attachment, body.image_b64)
