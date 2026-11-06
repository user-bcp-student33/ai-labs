from __future__ import annotations

import logging
import sqlite3
from pathlib import Path, PurePosixPath

import urllib3
from fastapi import HTTPException, UploadFile

from app.repositories import TaskRepository
from app.schemas import (
    BackendHealthResponse,
    RequirementResponse,
    SummaryResponse,
    TaskCreate,
    TaskResponse,
)


HEALTH_CHECK_UNAVAILABLE_DETAIL = "Health check unavailable"
OWNER_ACCESS_DENIED_DETAIL = "Owner access denied"
LOGGER = logging.getLogger(__name__)


class TaskService:
    def __init__(self, repository: TaskRepository) -> None:
        self._repository = repository

    def list_tasks(self, owner: str | None, identity: dict[str, str]) -> list[TaskResponse]:
        requested_owner = owner or identity["username"]
        self._validate_owner_access(requested_owner, identity)
        return [self._as_task(row) for row in self._repository.list_by_owner(requested_owner)]

    def create_task(self, payload: TaskCreate, identity: dict[str, str]) -> TaskResponse:
        owner = str(payload.owner) if "owner" in payload.model_fields_set else identity["username"]
        self._validate_owner_access(owner, identity)
        row = self._repository.create(
            title=payload.title,
            description=payload.description,
            status=payload.status,
            owner=owner,
            priority=payload.priority,
        )
        return self._as_task(row)

    def summary(self, owner: str | None, identity: dict[str, str]) -> SummaryResponse:
        requested_owner = owner or identity["username"]
        self._validate_owner_access(requested_owner, identity)
        response = SummaryResponse(owner=requested_owner)
        for row in self._repository.summary_rows(requested_owner):
            response.total += 1
            response.by_status[row["status"]] = response.by_status.get(row["status"], 0) + 1
            if row["priority"] == "HIGH" and row["status"] not in {"DONE", "ARCHIVED"}:
                response.high_priority_open += 1
        return response

    @staticmethod
    def _validate_owner_access(owner: str, identity: dict[str, str]) -> None:
        if owner != identity["username"]:
            raise HTTPException(status_code=403, detail=OWNER_ACCESS_DENIED_DETAIL)

    @staticmethod
    def _as_task(row: sqlite3.Row) -> TaskResponse:
        return TaskResponse(
            id=row["id"],
            title=row["title"],
            description=row["description"],
            status=row["status"],
            owner=row["owner"],
            priority=row["priority"],
            createdAt=row["created_at"],
        )


class RequirementService:
    def __init__(self, upload_dir: Path) -> None:
        self._upload_dir = upload_dir

    async def upload(self, file: UploadFile, username: str) -> RequirementResponse:
        file_name = file.filename or ""
        safe_file_name = PurePosixPath(file_name.replace("\\", "/")).name
        target = self._upload_dir / safe_file_name
        content = await file.read()
        target.write_bytes(content)
        return RequirementResponse(
            owner=username,
            fileName=file_name,
            storedAt=str(target),
            size=len(content),
        )


class BackendHealthService:
    def __init__(self, url: str) -> None:
        self._url = url

    def check(self, username: str) -> BackendHealthResponse:
        try:
            response = urllib3.PoolManager().request("GET", self._url, timeout=3.0)
            return BackendHealthResponse(
                requestedBy=username,
                status=response.status,
                body=response.data.decode("utf-8", errors="replace"),
            )
        except urllib3.exceptions.HTTPError as error:
            LOGGER.warning("Backend health request failed: %s", error)
            return BackendHealthResponse(
                requestedBy=username,
                status=0,
                body=HEALTH_CHECK_UNAVAILABLE_DETAIL,
            )