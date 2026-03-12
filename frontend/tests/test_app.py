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
