import io, zipfile
from pathlib import Path
TEXT_EXT = {".txt", ".md", ".py", ".java", ".kt", ".js", ".html", ".css", ".json", ".xml", ".gradle"}

def parse(name: str, data: bytes) -> str:
    suffix = Path(name).suffix.lower()
    if suffix in TEXT_EXT or suffix == "":
        return data.decode("utf-8", errors="replace")[:20000]
    if suffix == ".pdf":
        from pypdf import PdfReader
        reader = PdfReader(io.BytesIO(data))
        return "\n".join((page.extract_text() or "") for page in reader.pages[:20])[:20000]
    if suffix == ".docx":
        from docx import Document
        doc = Document(io.BytesIO(data))
        return "\n".join(p.text for p in doc.paragraphs)[:20000]
    if suffix == ".zip":
        parts = []
        with zipfile.ZipFile(io.BytesIO(data)) as zf:
            for info in zf.infolist()[:40]:
                if info.is_dir():
                    continue
                ext = Path(info.filename).suffix.lower()
                if ext not in TEXT_EXT:
                    parts.append(f"[გამოტოვებული] {info.filename}")
                    continue
                raw = zf.read(info)[:8000]
                parts.append(f"--- {info.filename} ---\n" + raw.decode("utf-8", errors="replace"))
        return "\n".join(parts)[:20000]
    return f"ამ ტიპს ({suffix}) ჯერ პირდაპირ ვერ ვკითხულობ."

def chunks(text: str, size: int = 1200) -> list[str]:
    return [text[i:i + size] for i in range(0, min(len(text), 12000), size)]
