def route(task: str, has_image: bool, has_file: bool) -> str:
    if has_image:
        return "vision"
    if has_file or task in {"analyze", "project"}:
        return "files"
    if task in {"search", "research"}:
        return "research"
    if task in {"code", "fix"}:
        return "code"
    if task in {"spell", "rewrite", "plan", "email"}:
        return "lite"
    return "chat"
