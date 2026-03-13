"""
Library Catalog — Streamlit Frontend

Allows users to search the library book catalog anonymously.
Connects to the Spring Boot backend via BACKEND_URL environment variable.

Conventions:
- All session state keys prefixed with st_
- All API calls in helper functions that return None on error and call st.error()
- Logging uses JSON-compatible format to /var/log/app/frontend.log
- See docs/FRONTEND.md for full conventions
"""

import logging
import os
from typing import Optional

import requests
import streamlit as st

# ── Constants ──────────────────────────────────────────────────────────────────
BACKEND_URL = os.environ.get("BACKEND_URL", "http://localhost:8080")
API_SEARCH_ENDPOINT = f"{BACKEND_URL}/api/v1/books/search"
API_BOOK_ENDPOINT = f"{BACKEND_URL}/api/v1/books"
API_REGISTER_ENDPOINT = f"{BACKEND_URL}/api/v1/auth/register"
API_LOGIN_ENDPOINT = f"{BACKEND_URL}/api/v1/auth/login"
LOG_DIR = os.environ.get("LOG_DIR", "/var/log/app")
REQUEST_TIMEOUT_SECONDS = 10
PAGE_SIZE = 20

GENRE_OPTIONS = {
    "": "All Genres",
    "FICTION": "Fiction",
    "NON_FICTION": "Non-Fiction",
    "TECHNOLOGY": "Technology",
    "SCIENCE": "Science",
    "HISTORY": "History",
    "BIOGRAPHY": "Biography",
    "MYSTERY": "Mystery",
    "ROMANCE": "Romance",
    "FANTASY": "Fantasy",
    "SCIENCE_FICTION": "Science Fiction",
    "SELF_HELP": "Self-Help",
    "BUSINESS": "Business",
    "OTHER": "Other",
}

# ── Logging setup ──────────────────────────────────────────────────────────────
os.makedirs(LOG_DIR, exist_ok=True)

logging.basicConfig(
    level=logging.INFO,
    format='{"timestamp": "%(asctime)s", "level": "%(levelname)s", '
           '"logger": "%(name)s", "message": "%(message)s"}',
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler(os.path.join(LOG_DIR, "frontend.log")),
    ],
)
logger = logging.getLogger(__name__)


# ── API helper functions ───────────────────────────────────────────────────────

def search_books(query: str, genre: str, page: int, size: int) -> Optional[dict]:
    """
    Search books via the backend API.

    Returns the unwrapped 'data' dict from the ApiResponse envelope on success.
    Returns None and calls st.error() on any failure.
    """
    params: dict = {"page": page, "size": size}
    if query:
        params["q"] = query
    if genre:
        params["genre"] = genre

    logger.info(
        "Searching books query='%s' genre='%s' page=%d size=%d",
        query, genre, page, size
    )

    try:
        response = requests.get(
            API_SEARCH_ENDPOINT,
            params=params,
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        envelope = response.json()
        logger.info(
            "Search completed status=%d totalElements=%s",
            envelope.get("status"),
            envelope.get("data", {}).get("totalElements", "unknown"),
        )
        return envelope.get("data")

    except requests.exceptions.ConnectionError:
        logger.error("Connection refused url=%s", BACKEND_URL)
        st.error(
            f"Cannot connect to the backend at `{BACKEND_URL}`. "
            "Is it running? Try: `./scripts/start.sh`"
        )
        return None

    except requests.exceptions.Timeout:
        logger.error("Request timed out url=%s", API_SEARCH_ENDPOINT)
        st.error(
            f"The search request timed out after {REQUEST_TIMEOUT_SECONDS}s. "
            "The server may be starting up. Please wait and try again."
        )
        return None

    except requests.exceptions.HTTPError as exc:
        try:
            envelope = exc.response.json()
            error_message = envelope.get("error", str(exc))
        except Exception:
            error_message = str(exc)
        logger.warning(
            "Search HTTP error status=%d message=%s",
            exc.response.status_code, error_message
        )
        st.error(f"Search failed: {error_message}")
        return None

    except Exception as exc:
        logger.exception("Unexpected error during search")
        st.error(f"An unexpected error occurred: {exc}")
        return None


def register_user(username: str, password: str, confirm_password: str) -> Optional[dict]:
    """
    Register a new user via the backend API.

    Returns the unwrapped 'data' dict (user with id, username) on success.
    Returns None and calls st.error() on failure.
    """
    if password != confirm_password:
        st.error("Password and confirm password do not match.")
        return None

    logger.info("Registering user username='%s'", username)

    try:
        response = requests.post(
            API_REGISTER_ENDPOINT,
            json={
                "username": username,
                "password": password,
                "confirmPassword": confirm_password,
            },
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        envelope = response.json()
        logger.info("Registration successful username='%s'", username)
        return envelope.get("data")
    except requests.exceptions.ConnectionError:
        logger.error("Connection refused url=%s", BACKEND_URL)
        st.error(
            f"Cannot connect to the backend at `{BACKEND_URL}`. "
            "Is it running? Try: `./scripts/start.sh`"
        )
        return None
    except requests.exceptions.HTTPError as exc:
        try:
            envelope = exc.response.json()
            error_message = envelope.get("error", str(exc))
        except Exception:
            error_message = str(exc)
        logger.warning(
            "Register HTTP error status=%d message=%s",
            exc.response.status_code, error_message
        )
        st.error(error_message)
        return None
    except Exception as exc:
        logger.exception("Unexpected error during registration")
        st.error(f"An unexpected error occurred: {exc}")
        return None


def login_user(username: str, password: str) -> Optional[dict]:
    """
    Log in via the backend API.

    Returns the unwrapped 'data' dict (userId, username, token) on success.
    Returns None and calls st.error() on failure.
    """
    logger.info("Logging in username='%s'", username)

    try:
        response = requests.post(
            API_LOGIN_ENDPOINT,
            json={"username": username, "password": password},
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        envelope = response.json()
        data = envelope.get("data")
        logger.info("Login successful username='%s'", username)
        return data
    except requests.exceptions.ConnectionError:
        logger.error("Connection refused url=%s", BACKEND_URL)
        st.error(
            f"Cannot connect to the backend at `{BACKEND_URL}`. "
            "Is it running? Try: `./scripts/start.sh`"
        )
        return None
    except requests.exceptions.HTTPError as exc:
        try:
            envelope = exc.response.json()
            error_message = envelope.get("error", "Invalid username or password")
        except Exception:
            error_message = "Invalid username or password"
        logger.warning(
            "Login HTTP error status=%d message=%s",
            exc.response.status_code, error_message
        )
        st.error(error_message)
        return None
    except Exception as exc:
        logger.exception("Unexpected error during login")
        st.error(f"An unexpected error occurred: {exc}")
        return None


def render_book_card(book: dict) -> None:
    """Render a single book as a bordered card."""
    genre_label = GENRE_OPTIONS.get(book.get("genre", ""), book.get("genre", ""))
    year = book.get("publicationYear")
    year_str = str(year) if year else "Unknown year"
    description = book.get("description", "")
    if description and len(description) > 220:
        description = description[:217] + "..."

    with st.container(border=True):
        st.markdown(f"### {book.get('title', 'Unknown Title')}")
        col_left, col_right = st.columns([2, 1])
        with col_left:
            st.markdown(f"**Author:** {book.get('author', 'Unknown')}")
            if book.get("isbn"):
                st.markdown(f"**ISBN:** `{book['isbn']}`")
        with col_right:
            st.markdown(f"**Genre:** {genre_label}")
            st.markdown(f"**Year:** {year_str}")
        if description:
            st.caption(description)


def render_pagination(total_pages: int, current_page: int) -> None:
    """Render previous / page-info / next pagination controls."""
    col_prev, col_info, col_next = st.columns([1, 2, 1])
    with col_prev:
        if current_page > 0:
            if st.button("← Previous", key="btn_prev"):
                st.session_state.st_current_page = current_page - 1
                st.rerun()
    with col_info:
        st.markdown(
            f"<div style='text-align:center'>Page {current_page + 1} of {total_pages}</div>",
            unsafe_allow_html=True,
        )
    with col_next:
        if current_page < total_pages - 1:
            if st.button("Next →", key="btn_next"):
                st.session_state.st_current_page = current_page + 1
                st.rerun()


# ── Page configuration ─────────────────────────────────────────────────────────
# Must be the first Streamlit call in the script.
st.set_page_config(
    page_title="Library Catalog",
    page_icon="📚",
    layout="wide",
    initial_sidebar_state="collapsed",
)

# ── Session state initialization ───────────────────────────────────────────────
if "st_search_query" not in st.session_state:
    st.session_state.st_search_query = ""
if "st_genre_filter" not in st.session_state:
    st.session_state.st_genre_filter = ""
if "st_current_page" not in st.session_state:
    st.session_state.st_current_page = 0
if "st_user" not in st.session_state:
    st.session_state.st_user = None
if "st_token" not in st.session_state:
    st.session_state.st_token = None

# ── Header ─────────────────────────────────────────────────────────────────────
header_col_title, header_col_auth = st.columns([3, 1])
with header_col_title:
    st.title("📚 Library Catalog")
    st.markdown("Search our collection of books.")
with header_col_auth:
    user = st.session_state.st_user
    if user is not None:
        st.markdown(f"**Welcome, {user.get('username', 'User')}**")
        if st.button("Logout", key="btn_logout"):
            st.session_state.st_user = None
            st.session_state.st_token = None
            st.rerun()
    else:
        with st.expander("Create Account", expanded=False):
            with st.form(key="register_form"):
                reg_username = st.text_input(
                    "Username",
                    max_chars=50,
                    placeholder="3–50 chars, letters, numbers, underscore",
                    key="reg_username",
                )
                reg_password = st.text_input(
                    "Password",
                    type="password",
                    placeholder="At least 8 characters",
                    key="reg_password",
                )
                reg_confirm = st.text_input(
                    "Confirm Password",
                    type="password",
                    key="reg_confirm",
                )
                if st.form_submit_button("Create Account"):
                    if reg_username and reg_password and reg_confirm:
                        data = register_user(
                            reg_username, reg_password, reg_confirm
                        )
                        if data:
                            st.success("Account created! Please log in.")
                            st.rerun()

        with st.expander("Login", expanded=False):
            with st.form(key="login_form"):
                login_username = st.text_input(
                    "Username",
                    key="login_username",
                )
                login_password = st.text_input(
                    "Password",
                    type="password",
                    key="login_password",
                )
                if st.form_submit_button("Log In"):
                    if login_username and login_password:
                        data = login_user(login_username, login_password)
                        if data:
                            st.session_state.st_user = {
                                "id": data.get("userId"),
                                "username": data.get("username"),
                            }
                            st.session_state.st_token = data.get("token")
                            st.success("Logged in!")
                            st.rerun()

# ── Search form ────────────────────────────────────────────────────────────────
with st.form(key="search_form"):
    col_search, col_genre, col_btn = st.columns([4, 2, 1])

    with col_search:
        query_input = st.text_input(
            "Search",
            value=st.session_state.st_search_query,
            placeholder="Search by title, author, or ISBN...",
            max_chars=200,
            label_visibility="collapsed",
        )
    with col_genre:
        genre_keys = list(GENRE_OPTIONS.keys())
        current_genre_index = (
            genre_keys.index(st.session_state.st_genre_filter)
            if st.session_state.st_genre_filter in genre_keys
            else 0
        )
        genre_input = st.selectbox(
            "Genre",
            options=genre_keys,
            format_func=lambda k: GENRE_OPTIONS[k],
            index=current_genre_index,
            label_visibility="collapsed",
        )
    with col_btn:
        submitted = st.form_submit_button("Search", use_container_width=True)

if submitted:
    st.session_state.st_search_query = query_input
    st.session_state.st_genre_filter = genre_input
    st.session_state.st_current_page = 0

# ── Search results ─────────────────────────────────────────────────────────────
current_query = st.session_state.st_search_query
current_genre = st.session_state.st_genre_filter
current_page = st.session_state.st_current_page

result = search_books(
    query=current_query,
    genre=current_genre,
    page=current_page,
    size=PAGE_SIZE,
)

if result is not None:
    total_elements = result.get("totalElements", 0)
    total_pages = result.get("totalPages", 0)
    books = result.get("content", [])

    # Result summary
    if current_query or current_genre:
        parts = []
        if current_query:
            parts.append(f"matching **'{current_query}'**")
        if current_genre:
            parts.append(f"in **{GENRE_OPTIONS.get(current_genre, current_genre)}**")
        st.markdown(f"Found **{total_elements}** book(s) {' '.join(parts)}")
    else:
        st.markdown(f"Showing all **{total_elements}** books in the catalog")

    if books:
        left_col, right_col = st.columns(2)
        for idx, book in enumerate(books):
            with (left_col if idx % 2 == 0 else right_col):
                render_book_card(book)

        if total_pages > 1:
            st.divider()
            render_pagination(total_pages, current_page)
    else:
        st.info(
            "No books found matching your search. "
            "Try different keywords or clear the genre filter."
        )
