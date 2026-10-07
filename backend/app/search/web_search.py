import httpx

async def search(query: str) -> list[dict]:
    url = "https://api.duckduckgo.com/"
    params = {"q": query, "format": "json", "no_html": 1, "skip_disambig": 1}
    async with httpx.AsyncClient(timeout=15) as client:
        response = await client.get(url, params=params)
        response.raise_for_status()
        data = response.json()
    results = []
    if data.get("AbstractText"):
        results.append({"title": data.get("Heading") or query, "text": data["AbstractText"], "url": data.get("AbstractURL", "")})
    for topic in data.get("RelatedTopics", [])[:5]:
        if isinstance(topic, dict) and topic.get("Text"):
            results.append({"title": topic.get("Text", "")[:80], "text": topic.get("Text", ""), "url": topic.get("FirstURL", "")})
    return results
