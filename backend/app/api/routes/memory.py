from fastapi import APIRouter, Depends
from pydantic import BaseModel
from app.auth.authentication import require_token
from app.memory.manager import recall, remember

router = APIRouter()

class Fact(BaseModel):
    user_id: str = "local"
    fact: str

@router.post("/memory")
async def add_memory(body: Fact, _: str = Depends(require_token)):
    remember(body.user_id, body.fact)
    return {"saved": True}

@router.get("/memory")
async def list_memory(user_id: str = "local", _: str = Depends(require_token)):
    return {"facts": recall(user_id)}
