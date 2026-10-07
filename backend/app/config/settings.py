import os
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

def load_env() -> None:
    env = ROOT / ".env"
    if not env.exists():
        return
    for line in env.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        os.environ.setdefault(key.strip(), value.strip())

load_env()
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
GEMINI_LITE_MODEL = os.getenv("GEMINI_LITE_MODEL", "gemini-2.5-flash-lite")
APP_TOKEN = os.getenv("APP_TOKEN", "change-me")
DATABASE_PATH = os.getenv("DATABASE_PATH", str(ROOT / "kartveli.db"))
