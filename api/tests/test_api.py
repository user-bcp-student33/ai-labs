import base64
import hashlib
import hmac
import json
import time
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import create_app

TEST_SECRET = "test-only-secret-for-api-token-verification-32-bytes"
OTHER_SECRET = "another-test-only-secret-value-with-32-bytes"
JWT_HEADER = {"alg": "HS256", "typ": "JWT"}


def encode_segment(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode()


def expires_at(seconds: int = 3600) -> int:
    return int(time.time()) + seconds


def build_token(claims: dict[str, object], secret: str = TEST_SECRET, header: dict[str, str] | None = None) -> str:
    encoded_header = encode_segment(json.dumps(header or JWT_HEADER).encode())
    encoded_claims = encode_segment(json.dumps(claims).encode())
    signing_input = f"{encoded_header}.{encoded_claims}"
    digest = hmac.new(secret.encode(), signing_input.encode(), hashlib.sha256).digest()
    return f"{signing_input}.{encode_segment(digest)}"


def token(username: str = "student", role: str = "STUDENT") -> str:
    # Mismo contrato que auth TokenSigner: JWT HS256 con sub, role y exp.
    return build_token({"sub": username, "role": role, "exp": expires_at()})


@pytest.fixture(autouse=True)
def configured_token_secret(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("AUTH_TOKEN_SECRET", TEST_SECRET)


def status_for_tasks(tmp_path: Path, bearer_token: str) -> int:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": "Bearer " + bearer_token})
    return response.status_code


def headers(username: str = "student", role: str = "STUDENT") -> dict[str, str]:
    return {"Authorization": f"Bearer {token(username, role)}"}


def test_lists_seeded_tasks(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers=headers())
    assert response.status_code == 200
    assert len(response.json()) == 2


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


def test_owner_query_and_payload_are_ignored(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        tasks = client.get("/api/tasks", params={"owner": "x' OR '1'='1"}, headers=headers())
        summary = client.get("/api/summary", params={"owner": "trainer"}, headers=headers())
        created = client.post(
            "/api/tasks",
            headers=headers(),
            json={"title": "Ajena", "description": "d", "status": "TODO", "priority": "LOW", "owner": "trainer"},
        )
    assert tasks.status_code == 200
    assert len(tasks.json()) == 2
    assert all(task["owner"] == "student" for task in tasks.json())
    assert summary.json()["owner"] == "student"
    assert created.json()["owner"] == "student"


def test_create_task_stores_sql_metacharacters_literally(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    payload = {"title": "O'Reilly'); DROP TABLE tasks; --", "description": "d'e", "status": "TODO", "priority": "LOW"}
    with TestClient(app) as client:
        created = client.post("/api/tasks", headers=headers(), json=payload)
        listed = client.get("/api/tasks", headers=headers())
    assert created.status_code == 201
    assert created.json()["title"] == payload["title"]
    assert created.json()["description"] == payload["description"]
    assert len(listed.json()) == 3


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
    assert (upload_dir / "requirements.txt").exists()


def test_rejects_invalid_token(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": "Bearer invalid"})
    assert response.status_code == 401


def test_accepts_token_issued_by_auth_contract(tmp_path: Path) -> None:
    assert status_for_tasks(tmp_path, token()) == 200


# Caracterización (G1): documenta la aceptación actual sin prefijo Bearer; no es un requisito aprobado.
def test_characterizes_token_accepted_without_bearer_prefix(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": token()})
    assert response.status_code == 200


# Caracterización (G3): documenta la aceptación actual de un secreto menor de 32 bytes.
def test_characterizes_token_accepted_with_short_secret(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    short_secret = "short-test-secret"
    monkeypatch.setenv("AUTH_TOKEN_SECRET", short_secret)
    short_token = build_token({"sub": "student", "role": "STUDENT", "exp": expires_at()}, secret=short_secret)
    assert status_for_tasks(tmp_path, short_token) == 200


# Caracterización (G4): documenta la aceptación actual de un sub vacío.
def test_characterizes_token_accepted_with_empty_subject(tmp_path: Path) -> None:
    empty_subject = build_token({"sub": "", "role": "STUDENT", "exp": expires_at()})
    assert status_for_tasks(tmp_path, empty_subject) == 200


# Caracterización (G4): PyJWT rechaza un sub numérico ("Subject must be a string"); el diseño esperaba 200.
def test_characterizes_token_rejected_with_numeric_subject(tmp_path: Path) -> None:
    numeric_subject = build_token({"sub": 42, "role": "STUDENT", "exp": expires_at()})
    assert status_for_tasks(tmp_path, numeric_subject) == 401


# Fixture fija con el formato de TokenSigner (HS256, sub/role/exp) firmada con TEST_SECRET; exp lejano para no depender del reloj.
AUTH_ISSUED_TOKEN = ".".join(
    [
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9",
        "eyJzdWIiOiJzdHVkZW50Iiwicm9sZSI6IlNUVURFTlQiLCJleHAiOjQxMDI0NDQ4MDB9",
        "KlBO6GvjP4DdmrmFsrsjwUiNl3UTIkiXLqf6DYZ7FF4",
    ]
)


def test_accepts_fixed_auth_issued_token_fixture(tmp_path: Path) -> None:
    assert status_for_tasks(tmp_path, AUTH_ISSUED_TOKEN) == 200


def test_identity_comes_from_verified_claims(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/summary", headers=headers("trainer", "TRAINER"))
    assert response.status_code == 200
    assert response.json()["owner"] == "trainer"


def test_rejects_token_with_invalid_signature(tmp_path: Path) -> None:
    forged = build_token({"sub": "student", "role": "STUDENT", "exp": expires_at()}, secret=OTHER_SECRET)
    assert status_for_tasks(tmp_path, forged) == 401


def test_rejects_token_with_altered_claims(tmp_path: Path) -> None:
    _, _, signature = token("student", "STUDENT").split(".")
    altered_claims = encode_segment(json.dumps({"sub": "student", "role": "ADMIN", "exp": expires_at()}).encode())
    altered_header = encode_segment(json.dumps(JWT_HEADER).encode())
    assert status_for_tasks(tmp_path, f"{altered_header}.{altered_claims}.{signature}") == 401


def test_rejects_expired_token(tmp_path: Path) -> None:
    expired = build_token({"sub": "student", "role": "STUDENT", "exp": expires_at(-60)})
    assert status_for_tasks(tmp_path, expired) == 401


def test_rejects_token_without_role_claim(tmp_path: Path) -> None:
    incomplete = build_token({"sub": "student", "exp": expires_at()})
    assert status_for_tasks(tmp_path, incomplete) == 401


def test_rejects_unsigned_token(tmp_path: Path) -> None:
    unsigned_header = encode_segment(json.dumps({"alg": "none", "typ": "JWT"}).encode())
    unsigned_claims = encode_segment(json.dumps({"sub": "student", "role": "ADMIN", "exp": expires_at()}).encode())
    assert status_for_tasks(tmp_path, f"{unsigned_header}.{unsigned_claims}.") == 401


def test_rejects_legacy_base64_token(tmp_path: Path) -> None:
    legacy = base64.b64encode(f"student|STUDENT|{expires_at()}|WORKSHOP_SECRET_2026".encode()).decode()
    assert status_for_tasks(tmp_path, legacy) == 401


def test_rejects_every_token_when_secret_is_not_configured(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("AUTH_TOKEN_SECRET")
    assert status_for_tasks(tmp_path, token()) == 401


PROTECTED_ROUTES = [
    ("GET", "/api/tasks", {}),
    ("POST", "/api/tasks", {"json": {"title": "Add tests", "description": "Improve coverage"}}),
    ("GET", "/api/summary", {}),
    ("POST", "/api/requirements", {"files": {"file": ("requirements.txt", b"Login", "text/plain")}}),
    ("GET", "/api/backend-health", {}),
]


@pytest.mark.parametrize(("method", "path", "request_body"), PROTECTED_ROUTES)
def test_protected_routes_reject_forged_token(tmp_path: Path, method: str, path: str, request_body: dict) -> None:
    forged = build_token({"sub": "student", "role": "ADMIN", "exp": expires_at()}, secret=OTHER_SECRET)
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.request(method, path, headers={"Authorization": "Bearer " + forged}, **request_body)
    assert response.status_code == 401


@pytest.mark.parametrize(("method", "path", "request_body"), PROTECTED_ROUTES)
def test_protected_routes_reject_missing_token(tmp_path: Path, method: str, path: str, request_body: dict) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.request(method, path, **request_body)
    assert response.status_code == 401

