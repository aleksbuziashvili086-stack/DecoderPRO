def validate(task: str, answer: str) -> str:
    if not answer.strip():
        return "პასუხი ცარიელი დაბრუნდა."
    if task in {"code", "fix"} and "```" not in answer and len(answer) < 40:
        return answer + "\n\nშენიშვნა: კოდის ბლოკი მოკლეა, გადაამოწმე ლოკალურად."
    return answer
