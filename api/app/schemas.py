from __future__ import annotations

from pydantic import BaseModel, ConfigDict, Field, field_validator


class ApiResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)


class TaskCreate(BaseModel):
    model_config = ConfigDict(extra="allow")

    title: str = ""
    description: str = ""
    status: str = "TODO"
    priority: str = "MEDIUM"
    owner: str | None = None

    @field_validator("title", "description", "status", "priority", "owner", mode="before")
    @classmethod
    def stringify_values(cls, value: object) -> str:
        return str(value)


class TaskResponse(ApiResponse):
    id: int
    title: str
    description: str
    status: str
    owner: str
    priority: str
    created_at: str = Field(alias="createdAt")


class SummaryResponse(ApiResponse):
    owner: str
    total: int = 0
    by_status: dict[str, int] = Field(default_factory=dict, alias="byStatus")
    high_priority_open: int = Field(default=0, alias="highPriorityOpen")


class RequirementResponse(ApiResponse):
    owner: str
    file_name: str = Field(alias="fileName")
    stored_at: str = Field(alias="storedAt")
    size: int


class BackendHealthResponse(ApiResponse):
    requested_by: str = Field(alias="requestedBy")
    status: int
    body: str