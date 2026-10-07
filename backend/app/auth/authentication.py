from fastapi import Header, HTTPException
from app.config.settings import APP_TOKEN

def require_token(authorization: str | None = Header(default=None)) -> str:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(401, "ტოკენი სჭირდება")
    token = authorization.removeprefix("Bearer ").strip()
    if token != APP_TOKEN:
        raise HTTPException(403, "ტოკენი არასწორია")
    return token
