from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.routes import chat, files, health, memory, search, vision, voice
from app.database.database import init_db

app = FastAPI(title="Kartveli AI")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
app.include_router(health.router, prefix="/v1")
app.include_router(chat.router, prefix="/v1")
app.include_router(files.router, prefix="/v1")
app.include_router(vision.router, prefix="/v1")
app.include_router(search.router, prefix="/v1")
app.include_router(memory.router, prefix="/v1")
app.include_router(voice.router, prefix="/v1")

@app.on_event("startup")
def startup() -> None:
    init_db()
