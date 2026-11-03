from __future__ import annotations

from collections.abc import Callable

from fastapi import APIRouter, Depends, File, Query, UploadFile

from app.schemas import (
    BackendHealthResponse,
    RequirementResponse,
    SummaryResponse,
    TaskCreate,
    TaskResponse,
)
from app.services import BackendHealthService, RequirementService, TaskService


def create_api_router(
    task_service: TaskService,
    requirement_service: RequirementService,
    health_service: BackendHealthService,
    identity_dependency: Callable[..., dict[str, str]],
) -> APIRouter:
    router = APIRouter()

    @router.get("/api/tasks", response_model=list[TaskResponse])
    def list_tasks(
        owner: str | None = Query(default=None),
        identity: dict[str, str] = Depends(identity_dependency),
    ) -> list[TaskResponse]:
        return task_service.list_tasks(owner, identity)

    @router.post("/api/tasks", status_code=201, response_model=TaskResponse)
    def create_task(
        payload: TaskCreate,
        identity: dict[str, str] = Depends(identity_dependency),
    ) -> TaskResponse:
        return task_service.create_task(payload, identity)

    @router.get("/api/summary", response_model=SummaryResponse)
    def summary(
        owner: str | None = Query(default=None),
        identity: dict[str, str] = Depends(identity_dependency),
    ) -> SummaryResponse:
        return task_service.summary(owner, identity)

    @router.post("/api/requirements", status_code=201, response_model=RequirementResponse)
    async def upload_requirement(
        file: UploadFile = File(...),
        identity: dict[str, str] = Depends(identity_dependency),
    ) -> RequirementResponse:
        return await requirement_service.upload(file, identity["username"])

    @router.get("/api/backend-health", response_model=BackendHealthResponse)
    def backend_health(
        identity: dict[str, str] = Depends(identity_dependency),
    ) -> BackendHealthResponse:
        return health_service.check(identity["username"])

    return router