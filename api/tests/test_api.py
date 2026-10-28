import base64
import os
import time
from pathlib import Path

import jwt
import pytest
from fastapi.testclient import TestClient

from app.main import create_app

TEST_TOKEN_SECRET = os.environ["WORKSHOP_AUTH_TOKEN_SECRET"]


def token(username: str = "student", role: str = "STUDENT") -> str:
    claims = {"username": username, "role": role, "exp": int(time.time()) + 3600}
    return jwt.encode(claims, TEST_TOKEN_SECRET, algorithm="HS256")


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


def test_rejects_legacy_base64_token(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    legacy = base64.b64encode(b"student|STUDENT|4102444800|not-a-real-secret").decode()
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": "Bearer " + legacy})
    assert response.status_code == 401


def test_rejects_token_signed_with_other_secret(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    forged = jwt.encode({"username": "student", "role": "STUDENT", "exp": int(time.time()) + 3600}, "other-test-secret-not-for-production-4567", algorithm="HS256")
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": "Bearer " + forged})
    assert response.status_code == 401


def test_rejects_tampered_payload(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    header, _, signature = token().split(".")
    forged_payload = base64.urlsafe_b64encode(b'{"username":"trainer","role":"TRAINER","exp":4102444800}').decode().rstrip("=")
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": f"Bearer {header}.{forged_payload}.{signature}"})
    assert response.status_code == 401


def test_rejects_expired_token(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    expired = jwt.encode({"username": "student", "role": "STUDENT", "exp": int(time.time()) - 10}, TEST_TOKEN_SECRET, algorithm="HS256")
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": "Bearer " + expired})
    assert response.status_code == 401


def test_rejects_missing_bearer_prefix(tmp_path: Path) -> None:
    app = create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))
    with TestClient(app) as client:
        response = client.get("/api/tasks", headers={"Authorization": token()})
    assert response.status_code == 401


def test_requires_token_secret_configuration(tmp_path: Path, monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.delenv("WORKSHOP_AUTH_TOKEN_SECRET")
    with pytest.raises(RuntimeError, match="WORKSHOP_AUTH_TOKEN_SECRET is required"):
        create_app(str(tmp_path / "workshop.db"), str(tmp_path / "uploads"))


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
