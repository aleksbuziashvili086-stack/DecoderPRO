BLOCKED = ("ბომბის აწყობა", "პაროლის მოპარვა", "ვინმეს მოკვლა")

def check(text: str) -> str | None:
    low = text.lower()
    for item in BLOCKED:
        if item in low:
            return "ამას ვერ გავაკეთებ."
    return None
