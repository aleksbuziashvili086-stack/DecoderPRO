from app.ai.model_router import route
from app.ai.prompt_manager import read_prompt
from app.ai.response_validator import validate
from app.ai.safety import check
from app.config.settings import GEMINI_LITE_MODEL, GEMINI_MODEL
from app.memory.manager import recall, save_chat, search_docs
from app.search.web_search import search
from app.vision.vision_service import ask

async def run(user_id: str, chat_id: str, task: str, tone: str, messages: list[dict], attachment: str = "", image_b64: str | None = None, instruction: str = "") -> dict:
    last = messages[-1].get("text", "") if messages else ""
    blocked = check(last)
    if blocked:
        return {"text": blocked, "route": "blocked", "sources": []}
    kind = route(task, bool(image_b64), bool(attachment))
    model = GEMINI_LITE_MODEL if kind == "lite" else GEMINI_MODEL
    sources = []
    extra = ""
    if kind == "research":
        sources = await search(last)
        extra = "\n".join(f"- {s.get('title')}: {s.get('text')} ({s.get('url')})" for s in sources)
    memories = recall(user_id)
    docs = search_docs(user_id, last + " " + attachment[:300])
    system = "\n".join([
        read_prompt("system", "georgian.txt"),
        read_prompt("system", "safety.txt"),
        read_prompt("system", "accuracy.txt"),
        instruction[:2000],
        f"ტონი: {tone}. რეჟიმი: {task}. მარშრუტი: {kind}.",
        "დამახსოვრებული: " + "; ".join(memories) if memories else "",
        "დოკუმენტებიდან: " + "\n".join(docs) if docs else "",
        "წყაროები: " + extra if extra else "",
        "ფაილი: " + attachment[:6000] if attachment else "",
    ])
    answer = await ask(system, messages, model, image_b64)
    answer = validate(task, answer)
    save_chat(user_id, chat_id or "local", last[:60], messages + [{"role": "assistant", "text": answer}])
    return {"text": answer, "route": kind, "model": model, "sources": sources}
