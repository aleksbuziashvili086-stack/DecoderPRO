from fastapi import APIRouter
from app.voice.speech_to_text import speak_note, transcribe_note

router = APIRouter()

@router.get("/voice")
async def voice():
    return {"stt": transcribe_note(), "tts": speak_note()}
