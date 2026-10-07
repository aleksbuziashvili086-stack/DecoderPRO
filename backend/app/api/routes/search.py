from fastapi import APIRouter, Depends
from pydantic import BaseModel
from app.auth.authentication import require_token
from app.search.web_search import search

router = APIRouter()

class Query(BaseModel):
    q: str

@router.post("/search")
async def do_search(body: Query, _: str = Depends(require_token)):
    return {"results": await search(body.q)}
