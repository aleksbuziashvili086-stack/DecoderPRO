from pathlib import Path

def prompts_dir() -> Path:
    here = Path(__file__).resolve()
    candidates = [here.parents[2] / "prompts", here.parents[2].parent / "prompts"]
    for path in candidates:
        if path.exists():
            return path
    return candidates[-1]

def read_prompt(*parts: str) -> str:
    path = prompts_dir().joinpath(*parts)
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8").strip()
