from fastapi import APIRouter, Depends, File, Form, UploadFile
from app.auth.authentication import require_token
from app.vision.vision_service import ask, encode_image

router = APIRouter()

@router.post("/vision")
async def vision(prompt: str = Form("აღწერე ეს სურათი ქართულად"), file: UploadFile = File(...), _: str = Depends(require_token)):
    data = await file.read()
    text = await ask("აღწერე სურათი ქართულად.", [{"role": "user", "text": prompt}], image_b64=encode_image(data), mime=file.content_type or "image/jpeg")
    return {"text": text}
