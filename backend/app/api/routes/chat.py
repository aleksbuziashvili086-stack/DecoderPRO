from fastapi import APIRouter, Depends
from pydantic import BaseModel
from app.auth.authentication import require_token
from app.ai.orchestrator import run
from app.database.database import allow_usage

router = APIRouter()

class ChatIn(BaseModel):
    user_id: str = "local"
    chat_id: str = "local"
    task: str = "free"
    tone: str = "მეგობრული"
    instruction: str = ""
    messages: list[dict]
    attachment: str = ""
    image_b64: str | None = None
    pro: bool = False

@router.post("/chat")
async def chat(body: ChatIn, _: str = Depends(require_token)):
    if not body.messages:
        return {"ok": False, "text": "შეტყობინება ცარიელია."}
    allowed = allow_usage(body.user_id, body.pro)
    if not allowed:
        return {"ok": False, "text": "დღევანდელი 20 უფასო პასუხი სერვერზეც ამოიწურა."}
    result = await run(body.user_id, body.chat_id, body.task, body.tone, body.messages, body.attachment, body.image_b64, body.instruction)
    result["ok"] = True
    return result
