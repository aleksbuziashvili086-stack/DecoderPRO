from fastapi import APIRouter, Depends, File, Form, UploadFile
from app.auth.authentication import require_token
from app.files.parser import chunks, parse
from app.memory.manager import save_chunks

router = APIRouter()

@router.post("/files/analyze")
async def analyze(user_id: str = Form("local"), file: UploadFile = File(...), _: str = Depends(require_token)):
    data = await file.read()
    if len(data) > 8_000_000:
        return {"error": "ფაილი 8 მბ-ზე დიდია"}
    text = parse(file.filename or "file.txt", data)
    save_chunks(user_id, file.filename or "file", chunks(text))
    return {"name": file.filename, "text": text[:4000], "saved": True}
