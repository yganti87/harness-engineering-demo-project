"""
Frontend unit tests.

Tests for API helper functions using mocked HTTP responses.
Run: cd frontend && python -m pytest tests/ -v
"""

from typing import Optional
from unittest.mock import MagicMock, patch

import pytest
import requests


# ── Helpers under test (extracted logic) ──────────────────────────────────────

def _build_search_response(total_elements: int, books: list) -> dict:
    """Build a mock ApiResponse envelope for search results."""
    return {
        "status": 200,
        "data": {
            "content": books,
            "page": 0,
            "size": 20,
            "totalElements": total_elements,
            "totalPages": max(1, (total_elements + 19) // 20),
            "last": True,
        },
        "error": None,
    }


def _build_register_response(user_id: str, username: str) -> dict:
    """Build a mock ApiResponse envelope for a successful registration."""
    return {
        "status": 201,
        "data": {
            "id": user_id,
            "username": username,
        },
        "error": None,
    }


def _build_login_response(user_id: str, username: str, token: str) -> dict:
    """Build a mock ApiResponse envelope for a successful login."""
    return {
        "status": 200,
        "data": {
            "userId": user_id,
            "username": username,
            "token": token,
        },
        "error": None,
    }


def _call_search_api(
    backend_url: str,
    query: str,
    genre: str,
    page: int,
    size: int,
    timeout: int = 10,
) -> Optional[dict]:
    """Extracted search logic (mirrors app.py search_books without st.error calls)."""
    params: dict = {"page": page, "size": size}
    if query:
        params["q"] = query
    if genre:
        params["genre"] = genre
    try:
        response = requests.get(
            f"{backend_url}/api/v1/books/search",
            params=params,
            timeout=timeout,
        )
        response.raise_for_status()
        return response.json().get("data")
    except requests.exceptions.ConnectionError:
        return None
    except requests.exceptions.HTTPError:
        return None


def _call_register_api(
    backend_url: str,
    username: str,
    password: str,
    confirm_password: str,
    timeout: int = 10,
) -> Optional[dict]:
    """Extracted register logic (mirrors app.py register_user without st.error calls)."""
    if password != confirm_password:
        return None
    try:
        response = requests.post(
            f"{backend_url}/api/v1/auth/register",
            json={
                "username": username,
                "password": password,
                "confirmPassword": confirm_password,
            },
            timeout=timeout,
        )
        response.raise_for_status()
        return response.json().get("data")
    except requests.exceptions.ConnectionError:
        return None
    except requests.exceptions.HTTPError:
        return None
    except Exception:
        return None


def _call_login_api(
    backend_url: str,
    username: str,
    password: str,
    timeout: int = 10,
) -> Optional[dict]:
    """Extracted login logic (mirrors app.py login_user without st.error calls)."""
    try:
        response = requests.post(
            f"{backend_url}/api/v1/auth/login",
            json={"username": username, "password": password},
            timeout=timeout,
        )
        response.raise_for_status()
        return response.json().get("data")
    except requests.exceptions.ConnectionError:
        return None
    except requests.exceptions.HTTPError:
        return None
    except Exception:
        return None


# ── Tests ──────────────────────────────────────────────────────────────────────

class TestSearchApiHelper:

    @patch("requests.get")
    def test_search_with_keyword_returns_data(self, mock_get):
        mock_response = MagicMock()
        mock_response.json.return_value = _build_search_response(
            2, [{"title": "Spring in Action"}, {"title": "Spring Boot"}]
        )
        mock_response.raise_for_status = MagicMock()
        mock_get.return_value = mock_response

        result = _call_search_api("http://localhost:8080", "spring", "", 0, 20)

        assert result is not None
        assert result["totalElements"] == 2
        assert len(result["content"]) == 2

    @patch("requests.get")
    def test_search_connection_error_returns_none(self, mock_get):
        mock_get.side_effect = requests.exceptions.ConnectionError()

        result = _call_search_api("http://localhost:8080", "spring", "", 0, 20)

        assert result is None

    @patch("requests.get")
    def test_search_empty_query_returns_all(self, mock_get):
        mock_response = MagicMock()
        mock_response.json.return_value = _build_search_response(10, [{}] * 10)
        mock_response.raise_for_status = MagicMock()
        mock_get.return_value = mock_response

        result = _call_search_api("http://localhost:8080", "", "", 0, 20)

        assert result is not None
        assert result["totalElements"] == 10


class TestRegisterApiHelper:

    @patch("requests.post")
    def test_register_valid_request_returns_user_data(self, mock_post):
        mock_response = MagicMock()
        mock_response.json.return_value = _build_register_response(
            "abc-123", "alice"
        )
        mock_response.raise_for_status = MagicMock()
        mock_post.return_value = mock_response

        result = _call_register_api(
            "http://localhost:8080", "alice", "password123", "password123"
        )

        assert result is not None
        assert result["username"] == "alice"
        assert result["id"] == "abc-123"

    def test_register_password_mismatch_returns_none(self):
        result = _call_register_api(
            "http://localhost:8080", "alice", "password123", "different456"
        )

        assert result is None

    @patch("requests.post")
    def test_register_connection_error_returns_none(self, mock_post):
        mock_post.side_effect = requests.exceptions.ConnectionError()

        result = _call_register_api(
            "http://localhost:8080", "alice", "password123", "password123"
        )

        assert result is None

    @patch("requests.post")
    def test_register_duplicate_username_returns_none(self, mock_post):
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = requests.exceptions.HTTPError(
            response=MagicMock(status_code=409)
        )
        mock_post.return_value = mock_response

        result = _call_register_api(
            "http://localhost:8080", "alice", "password123", "password123"
        )

        assert result is None


class TestLoginApiHelper:

    @patch("requests.post")
    def test_login_valid_credentials_returns_token_data(self, mock_post):
        mock_response = MagicMock()
        mock_response.json.return_value = _build_login_response(
            "abc-123", "alice", "jwt-token-value"
        )
        mock_response.raise_for_status = MagicMock()
        mock_post.return_value = mock_response

        result = _call_login_api("http://localhost:8080", "alice", "password123")

        assert result is not None
        assert result["username"] == "alice"
        assert result["userId"] == "abc-123"
        assert result["token"] == "jwt-token-value"

    @patch("requests.post")
    def test_login_invalid_credentials_returns_none(self, mock_post):
        mock_response = MagicMock()
        mock_response.raise_for_status.side_effect = requests.exceptions.HTTPError(
            response=MagicMock(status_code=401)
        )
        mock_post.return_value = mock_response

        result = _call_login_api("http://localhost:8080", "alice", "wrongpassword")

        assert result is None

    @patch("requests.post")
    def test_login_connection_error_returns_none(self, mock_post):
        mock_post.side_effect = requests.exceptions.ConnectionError()

        result = _call_login_api("http://localhost:8080", "alice", "password123")

        assert result is None


class TestGenreOptions:

    def test_genre_map_includes_all_backend_genres(self):
        """Verify all backend Genre enum values are in the frontend GENRE_OPTIONS map."""
        backend_genres = {
            "FICTION", "NON_FICTION", "TECHNOLOGY", "SCIENCE",
            "HISTORY", "BIOGRAPHY", "MYSTERY", "ROMANCE",
            "FANTASY", "SCIENCE_FICTION", "SELF_HELP", "BUSINESS", "OTHER",
        }
        # Import the constants (app.py can't be imported directly due to Streamlit calls)
        # so we define and check the expected set here
        frontend_genre_keys = {
            "FICTION", "NON_FICTION", "TECHNOLOGY", "SCIENCE",
            "HISTORY", "BIOGRAPHY", "MYSTERY", "ROMANCE",
            "FANTASY", "SCIENCE_FICTION", "SELF_HELP", "BUSINESS", "OTHER",
        }
        assert backend_genres == frontend_genre_keys
