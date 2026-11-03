from __future__ import annotations

import base64
import binascii
import logging
import os
import time
from pathlib import Path

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from app.repositories import TaskRepository, open_database
from app.routes import create_api_router
from app.services import BackendHealthService, RequirementService, TaskService

LEGACY_SECRET = "WORKSHOP_SECRET_2026"
INVALID_TOKEN_DETAIL = "Invalid token"
LOGGER = logging.getLogger(__name__)


def legacy_identity(authorization: str = Header(default="")) -> dict[str, str]:
    try:
        token = authorization.replace("Bearer ", "")
        decoded = base64.b64decode(token).decode("utf-8")
        parts = decoded.split("|")
        if len(parts) != 4 or parts[3] != LEGACY_SECRET or int(parts[2]) < int(time.time()):
            raise ValueError("invalid token")
        return {"username": parts[0], "role": parts[1]}
    except (binascii.Error, UnicodeDecodeError, TypeError, ValueError) as error:
        LOGGER.debug("Token validation failed: %s", error)
        raise HTTPException(status_code=401, detail=INVALID_TOKEN_DETAIL) from error


def create_app(db_path: str | None = None, upload_dir: str | None = None) -> FastAPI:
    selected_db = db_path or os.getenv("WORKSHOP_DB_PATH", "./data/workshop.db")
    selected_uploads = upload_dir or os.getenv("WORKSHOP_UPLOAD_DIR", "./uploads")
    Path(selected_db).parent.mkdir(parents=True, exist_ok=True)
    Path(selected_uploads).mkdir(parents=True, exist_ok=True)

    connection = open_database(selected_db)

    app = FastAPI(title="Workshop Requirements API", version="0.1.0")
    app.state.connection = connection
    app.state.upload_dir = selected_uploads
    task_service = TaskService(TaskRepository(connection))
    requirement_service = RequirementService(Path(selected_uploads))
    health_service = BackendHealthService(
        os.getenv("AUTH_HEALTH_URL", "http://localhost:8080/q/health")
    )
    app.include_router(
        create_api_router(task_service, requirement_service, health_service, legacy_identity)
    )
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    @app.on_event("shutdown")
    def close_connection() -> None:
        connection.close()

    return app


app = create_app()
