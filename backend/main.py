import os

import httpx
from fastapi import FastAPI, File, HTTPException, UploadFile

ELEVENLABS_API_KEY = os.environ["ELEVENLABS_API_KEY"]

app = FastAPI()


@app.post("/transcribe")
async def transcribe(audio: UploadFile = File(...)):
    data = await audio.read()
    if not data:
        raise HTTPException(400, "empty file")

    async with httpx.AsyncClient(timeout=120) as client:
        resp = await client.post(
            "https://api.elevenlabs.io/v1/speech-to-text",
            headers={"xi-api-key": ELEVENLABS_API_KEY},
            data={"model_id": "scribe_v1"},
            files={
                "file": (
                    audio.filename or "audio.ogg",
                    data,
                    audio.content_type or "application/octet-stream",
                )
            },
        )

    if resp.status_code != 200:
        raise HTTPException(resp.status_code, resp.text)

    return {"transcript": resp.json().get("text", "")}
