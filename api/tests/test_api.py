import base64
import time
from collections.abc import Iterator
from pathlib import Path

import pytest
import urllib3
from fastapi.testclient import TestClient

from app.main import create_app


def token(username: str = "student", role: str = "STUDENT") -> str:
    raw = f"{username}|{role}|{int(time.time()) + 3600}|WORKSHOP_SECRET_2026"
    return base64.b64encode(raw.encode()).decode()


def headers(username: str = "student", role: str = "STUDENT") -> dict[str, str]:
    return {"Authorization": f"Bearer {token(username, role)}"}


@pytest.fixture
def api_client(tmp_path: Path) -> Iterator[TestClient]:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app, raise_server_exceptions=False) as client:
        yield client


def test_lists_seeded_tasks(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers=headers())
    assert response.status_code == 200
    assert len(response.json()) == 2
    assert set(response.json()[0]) == {
        "id",
        "title",
        "description",
        "status",
        "owner",
        "priority",
        "createdAt",
    }


def test_creates_task_and_summary(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        created = client.post(
            "/api/tasks",
            headers=headers(),
            json={"title": "Add tests", "description": "Improve coverage", "status": "TODO", "priority": "HIGH"},
        )
        summary = client.get("/api/summary", headers=headers())
    assert created.status_code == 201
    assert created.json()["owner"] == "student"
    assert summary.json()["total"] == 3
    assert set(summary.json()) == {"owner", "total", "byStatus", "highPriorityOpen"}


def test_create_task_preserves_apostrophes(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app, raise_server_exceptions=False) as client:
        response = client.post(
            "/api/tasks",
            headers=headers(),
            json={"title": "Owner's task", "description": "Don't break SQL"},
        )
    assert response.status_code == 201
    assert response.json()["title"] == "Owner's task"
    assert response.json()["description"] == "Don't break SQL"


def test_create_app_keeps_sqlite_databases_isolated(tmp_path: Path) -> None:
    first_app = create_app(str(tmp_path / "first.db"), str(tmp_path / "first-uploads"))
    second_app = create_app(str(tmp_path / "second.db"), str(tmp_path / "second-uploads"))
    with TestClient(first_app) as first_client, TestClient(second_app) as second_client:
        created = first_client.post(
            "/api/tasks",
            headers=headers(),
            json={"title": "First database", "description": "Isolated data"},
        )
        second_tasks = second_client.get("/api/tasks", headers=headers())
    assert created.status_code == 201
    assert len(second_tasks.json()) == 2


def test_uploads_requirement(tmp_path: Path) -> None:
    upload_dir = tmp_path / "uploads"
    app = create_app(str(tmp_path / "workshop.db"), str(upload_dir))
    with TestClient(app) as client:
        response = client.post(
            "/api/requirements",
            headers=headers(),
            files={"file": ("requirements.txt", b"Login must be accessible", "text/plain")},
        )
    assert response.status_code == 201
    assert set(response.json()) == {"owner", "fileName", "storedAt", "size"}
    assert (upload_dir / "requirements.txt").exists()


def test_rejects_invalid_token(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": "Bearer invalid"})
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid token"


def test_backend_health_hides_internal_error(monkeypatch, tmp_path: Path) -> None:
    class FailingHealthClient:
        def request(self, *args, **kwargs):
            raise urllib3.exceptions.HTTPError("local transport failure")

    monkeypatch.setattr("app.services.urllib3.PoolManager", FailingHealthClient)
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/backend-health", headers=headers())
    assert response.status_code == 200
    assert response.json()["body"] == "Health check unavailable"
    assert "local transport failure" not in response.json()["body"]


def test_backend_health_preserves_success_contract(monkeypatch, tmp_path: Path) -> None:
    class HealthyResponse:
        status = 200
        data = b'{"status":"UP"}'

    class HealthyPoolManager:
        def request(self, *args, **kwargs):
            return HealthyResponse()

    monkeypatch.setattr("app.services.urllib3.PoolManager", HealthyPoolManager)
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/backend-health", headers=headers())
    assert response.status_code == 200
    assert response.json() == {
        "requestedBy": "student",
        "status": 200,
        "body": '{"status":"UP"}',
    }


def test_list_tasks_with_empty_owner_uses_token_identity(api_client: TestClient) -> None:
    response = api_client.get("/api/tasks?owner=", headers=headers())
    assert response.status_code == 200
    assert {task["owner"] for task in response.json()} == {"student"}


def test_list_tasks_with_matching_owner_uses_selected_owner(api_client: TestClient) -> None:
    response = api_client.get("/api/tasks?owner=student", headers=headers())
    assert response.status_code == 200
    assert {task["owner"] for task in response.json()} == {"student"}


def test_list_tasks_rejects_unknown_owner(api_client: TestClient) -> None:
    response = api_client.get("/api/tasks?owner=missing", headers=headers())
    assert response.status_code == 403


def test_list_tasks_rejects_cross_owner_access(api_client: TestClient) -> None:
    response = api_client.get("/api/tasks?owner=trainer", headers=headers())
    assert response.status_code == 403


def test_list_tasks_requires_authentication(api_client: TestClient) -> None:
    response = api_client.get("/api/tasks")
    assert response.status_code == 401
    assert response.json() == {"detail": "Invalid token"}


def test_list_tasks_reports_internal_failure(api_client: TestClient) -> None:
    api_client.app.state.connection.execute("DROP TABLE tasks")
    response = api_client.get("/api/tasks", headers=headers())
    assert response.status_code == 500
    assert response.text == "Internal Server Error"


def test_summary_counts_statuses_and_high_priority_tasks(api_client: TestClient) -> None:
    response = api_client.get("/api/summary", headers=headers())
    assert response.status_code == 200
    assert response.json() == {
        "owner": "student",
        "total": 2,
        "byStatus": {"TODO": 1, "IN_PROGRESS": 1},
        "highPriorityOpen": 1,
    }


def test_summary_rejects_unknown_owner(api_client: TestClient) -> None:
    response = api_client.get("/api/summary?owner=missing", headers=headers())
    assert response.status_code == 403


def test_summary_counts_repeated_states_and_excludes_closed_high_priority(
    api_client: TestClient,
) -> None:
    for status in ("TODO", "TODO", "DONE", "ARCHIVED"):
        response = api_client.post(
            "/api/tasks",
            headers=headers(),
            json={"title": status, "description": "Summary boundary", "status": status, "priority": "HIGH"},
        )
        assert response.status_code == 201

    summary = api_client.get("/api/summary", headers=headers())
    assert summary.json() == {
        "owner": "student",
        "total": 6,
        "byStatus": {"TODO": 3, "IN_PROGRESS": 1, "DONE": 1, "ARCHIVED": 1},
        "highPriorityOpen": 3,
    }


def test_summary_rejects_cross_owner_access(api_client: TestClient) -> None:
    response = api_client.get("/api/summary?owner=trainer", headers=headers())
    assert response.status_code == 403


def test_summary_requires_authentication(api_client: TestClient) -> None:
    response = api_client.get("/api/summary")
    assert response.status_code == 401
    assert response.json() == {"detail": "Invalid token"}


def test_summary_reports_internal_failure(api_client: TestClient) -> None:
    api_client.app.state.connection.execute("DROP TABLE tasks")
    response = api_client.get("/api/summary", headers=headers())
    assert response.status_code == 500
    assert response.text == "Internal Server Error"


def test_create_task_empty_payload_uses_defaults(api_client: TestClient) -> None:
    response = api_client.post("/api/tasks", headers=headers(), json={})
    assert response.status_code == 201
    assert response.json()["title"] == ""
    assert response.json()["description"] == ""
    assert response.json()["status"] == "TODO"
    assert response.json()["priority"] == "MEDIUM"
    assert response.json()["owner"] == "student"


def test_create_task_rejects_owner_override(api_client: TestClient) -> None:
    response = api_client.post(
        "/api/tasks",
        headers=headers(),
        json={"title": "Other owner", "owner": "trainer"},
    )
    assert response.status_code == 403


def test_create_task_without_body_returns_validation_error(api_client: TestClient) -> None:
    response = api_client.post("/api/tasks", headers=headers())
    assert response.status_code == 422
    assert response.json()["detail"][0]["loc"] == ["body"]


def test_create_task_malformed_json_returns_validation_error(api_client: TestClient) -> None:
    response = api_client.post(
        "/api/tasks",
        headers={**headers(), "Content-Type": "application/json"},
        content="{",
    )
    assert response.status_code == 422
    assert response.json()["detail"][0]["type"] == "json_invalid"


def test_create_task_requires_authentication(api_client: TestClient) -> None:
    response = api_client.post("/api/tasks", json={})
    assert response.status_code == 401
    assert response.json() == {"detail": "Invalid token"}


def test_create_task_reports_internal_failure(api_client: TestClient) -> None:
    api_client.app.state.connection.execute("DROP TABLE tasks")
    response = api_client.post("/api/tasks", headers=headers(), json={})
    assert response.status_code == 500
    assert response.text == "Internal Server Error"


def test_uploads_zero_byte_file(api_client: TestClient) -> None:
    response = api_client.post(
        "/api/requirements",
        headers=headers(),
        files={"file": ("empty.txt", b"", "text/plain")},
    )
    assert response.status_code == 201
    assert response.json()["size"] == 0
    assert Path(api_client.app.state.upload_dir, "empty.txt").read_bytes() == b""


def test_uploads_file_without_extension(api_client: TestClient) -> None:
    response = api_client.post(
        "/api/requirements",
        headers=headers(),
        files={"file": ("requirements", b"content", "application/octet-stream")},
    )
    assert response.status_code == 201
    assert response.json()["fileName"] == "requirements"
    assert Path(api_client.app.state.upload_dir, "requirements").read_bytes() == b"content"


def test_upload_requires_file_part(api_client: TestClient) -> None:
    response = api_client.post("/api/requirements", headers=headers())
    assert response.status_code == 422
    assert response.json()["detail"][0]["loc"] == ["body", "file"]


def test_upload_requires_authentication(api_client: TestClient) -> None:
    response = api_client.post(
        "/api/requirements",
        files={"file": ("requirements.txt", b"text", "text/plain")},
    )
    assert response.status_code == 401
    assert response.json() == {"detail": "Invalid token"}


def test_upload_reports_internal_write_failure(api_client: TestClient, monkeypatch: pytest.MonkeyPatch) -> None:
    def fail_upload_write(path: Path, content: bytes) -> int:
        raise OSError("local disk failure")

    monkeypatch.setattr(Path, "write_bytes", fail_upload_write)
    response = api_client.post(
        "/api/requirements",
        headers=headers(),
        files={"file": ("requirements.txt", b"text", "text/plain")},
    )
    assert response.status_code == 500
    assert response.text == "Internal Server Error"


def test_upload_keeps_traversal_filename_inside_upload_directory(api_client: TestClient) -> None:
    upload_dir = Path(api_client.app.state.upload_dir).resolve()
    response = api_client.post(
        "/api/requirements",
        headers=headers(),
        files={"file": ("..\\escape.txt", b"outside", "text/plain")},
    )
    escaped_path = upload_dir.parent / "escape.txt"
    assert not escaped_path.exists()
    if response.status_code == 201:
        stored_path = Path(response.json()["storedAt"]).resolve()
        assert stored_path.parent == upload_dir


def test_backend_health_replaces_invalid_utf8(api_client: TestClient, monkeypatch: pytest.MonkeyPatch) -> None:
    class InvalidUtf8Response:
        status = 200
        data = b"\xff"

    class InvalidUtf8PoolManager:
        def request(self, *args, **kwargs):
            return InvalidUtf8Response()

    monkeypatch.setattr("app.services.urllib3.PoolManager", InvalidUtf8PoolManager)
    response = api_client.get("/api/backend-health", headers=headers())
    assert response.status_code == 200
    assert response.json()["body"] == "\ufffd"


def test_backend_health_requires_authentication(api_client: TestClient) -> None:
    response = api_client.get("/api/backend-health")
    assert response.status_code == 401
    assert response.json() == {"detail": "Invalid token"}


def test_backend_health_reports_unexpected_internal_failure(
    api_client: TestClient,
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    class UnexpectedFailurePoolManager:
        def request(self, *args, **kwargs):
            raise RuntimeError("unexpected local failure")

    monkeypatch.setattr("app.services.urllib3.PoolManager", UnexpectedFailurePoolManager)
    response = api_client.get("/api/backend-health", headers=headers())
    assert response.status_code == 500
    assert response.text == "Internal Server Error"
