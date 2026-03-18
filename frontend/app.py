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
API_RESEND_VERIFICATION_ENDPOINT = f"{BACKEND_URL}/api/v1/auth/resend-verification"
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


def register_user(email: str, password: str, confirm_password: str) -> Optional[dict]:
    """
    Register a new user via the backend API.

    Returns the unwrapped 'data' dict (user with id, email, emailVerified) on success.
    Returns None and calls st.error() on failure.
    """
    if password != confirm_password:
        st.error("Password and confirm password do not match.")
        return None

    logger.info("Registering user email='%s'", email)

    try:
        response = requests.post(
            API_REGISTER_ENDPOINT,
            json={
                "email": email,
                "password": password,
                "confirmPassword": confirm_password,
            },
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        envelope = response.json()
        logger.info("Registration successful email='%s'", email)
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


def login_user(email: str, password: str) -> Optional[dict]:
    """
    Log in via the backend API.

    Returns the unwrapped 'data' dict (userId, email, token) on success.
    Sets st_email_not_verified=True if login fails with 403 (unverified email).
    Returns None and calls st.error() on failure.
    """
    logger.info("Logging in email='%s'", email)

    try:
        response = requests.post(
            API_LOGIN_ENDPOINT,
            json={"email": email, "password": password},
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        envelope = response.json()
        data = envelope.get("data")
        logger.info("Login successful email='%s'", email)
        return data
    except requests.exceptions.ConnectionError:
        logger.error("Connection refused url=%s", BACKEND_URL)
        st.error(
            f"Cannot connect to the backend at `{BACKEND_URL}`. "
            "Is it running? Try: `./scripts/start.sh`"
        )
        return None
    except requests.exceptions.HTTPError as exc:
        if exc.response.status_code == 403:
            st.session_state.st_email_not_verified = True
            st.session_state.st_pending_verification_email = email
            logger.warning("Login blocked: email not verified email='%s'", email)
            return None
        try:
            envelope = exc.response.json()
            error_message = envelope.get("error", "Invalid email or password")
        except Exception:
            error_message = "Invalid email or password"
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


def resend_verification(email: str) -> bool:
    """
    Resend verification email via the backend API.

    Returns True on success (200), False on failure.
    """
    logger.info("Resending verification email='%s'", email)

    try:
        response = requests.post(
            API_RESEND_VERIFICATION_ENDPOINT,
            json={"email": email},
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        logger.info("Resend verification requested email='%s'", email)
        return True
    except requests.exceptions.ConnectionError:
        logger.error("Connection refused url=%s", BACKEND_URL)
        st.error(
            f"Cannot connect to the backend at `{BACKEND_URL}`. "
            "Is it running? Try: `./scripts/start.sh`"
        )
        return False
    except requests.exceptions.HTTPError as exc:
        try:
            envelope = exc.response.json()
            error_message = envelope.get("error", str(exc))
        except Exception:
            error_message = str(exc)
        logger.warning(
            "Resend verification HTTP error status=%d message=%s",
            exc.response.status_code, error_message
        )
        st.error(error_message)
        return False
    except Exception as exc:
        logger.exception("Unexpected error during resend verification")
        st.error(f"An unexpected error occurred: {exc}")
        return False


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


def render_landing_page() -> None:
    """Render the landing page for logged-out users: hero section + auth forms."""
    st.write("")

    _, hero_col, _ = st.columns([1, 2, 1])
    with hero_col:
        st.markdown(
            "<h1 style='text-align: center; margin-bottom: 0;'>📚 Library Catalog</h1>",
            unsafe_allow_html=True,
        )
        st.markdown(
            "<p style='text-align: center;'>Discover, search, and explore our curated collection "
            "of books. Sign in to browse the full catalog, filter by genre, and find your next "
            "great read.</p>",
            unsafe_allow_html=True,
        )

    _, form_col, _ = st.columns([1, 2, 1])
    with form_col:
        login_tab, register_tab = st.tabs(["Log In", "Create Account"])

        with login_tab:
            st.markdown("##### Welcome back")

            if st.session_state.st_email_not_verified:
                pending_email = st.session_state.st_pending_verification_email or ""
                st.warning(
                    "Your email is not yet verified. "
                    "Please check your inbox for the verification link."
                )
                if st.button(
                    "Resend Verification Email",
                    key="btn_resend_from_login",
                    use_container_width=True,
                ):
                    if pending_email and resend_verification(pending_email):
                        st.success("Verification email resent! Check your inbox.")
                        st.session_state.st_email_not_verified = False
                    st.rerun()

            with st.form(key="login_form"):
                login_email = st.text_input(
                    "Email",
                    key="login_email",
                )
                login_password = st.text_input(
                    "Password",
                    type="password",
                    key="login_password",
                )
                if st.form_submit_button("Log In", use_container_width=True):
                    st.session_state.st_email_not_verified = False
                    if login_email and login_password:
                        data = login_user(login_email, login_password)
                        if data:
                            st.session_state.st_user = {
                                "id": data.get("userId"),
                                "email": data.get("email"),
                            }
                            st.session_state.st_token = data.get("token")
                            st.rerun()

        with register_tab:
            if st.session_state.st_pending_verification_email and \
                    not st.session_state.st_email_not_verified:
                pending_email = st.session_state.st_pending_verification_email
                st.success("Account Created!")
                st.markdown(
                    f"A verification email has been sent to **{pending_email}**. "
                    "Please click the link in the email to verify your account before logging in."
                )
                if st.button(
                    "Resend Verification Email",
                    key="btn_resend_after_register",
                    use_container_width=True,
                ):
                    resend_verification(pending_email)
                    st.success("Verification email resent!")
                if st.button("Go to Log In", use_container_width=True):
                    st.session_state.st_pending_verification_email = None
                    st.rerun()
            else:
                st.markdown("##### Create your account")
                with st.form(key="register_form"):
                    reg_email = st.text_input(
                        "Email",
                        placeholder="you@example.com",
                        key="reg_email",
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
                    if st.form_submit_button("Create Account", use_container_width=True):
                        if reg_email and reg_password and reg_confirm:
                            data = register_user(reg_email, reg_password, reg_confirm)
                            if data:
                                st.session_state.st_pending_verification_email = reg_email
                                st.rerun()

    _, footer_col, _ = st.columns([1, 2, 1])
    with footer_col:
        st.caption("Free to use. No credit card required.")


def render_catalog() -> None:
    """Render the full catalog experience for logged-in users."""
    # ── Header ─────────────────────────────────────────────────────────────────
    header_col_title, header_col_auth = st.columns([3, 1])
    with header_col_title:
        st.title("📚 Library Catalog")
    with header_col_auth:
        user = st.session_state.st_user
        st.markdown(f"**Welcome, {user.get('email', 'User')}**")
        if st.button("Logout", key="btn_logout"):
            st.session_state.st_user = None
            st.session_state.st_token = None
            st.rerun()

    # ── Search form ─────────────────────────────────────────────────────────────
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

    # ── Search results ──────────────────────────────────────────────────────────
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
if "st_pending_verification_email" not in st.session_state:
    st.session_state.st_pending_verification_email = None
if "st_email_not_verified" not in st.session_state:
    st.session_state.st_email_not_verified = False

# ── Main rendering: gate on authentication ─────────────────────────────────────
if st.session_state.st_user is None:
    render_landing_page()
else:
    render_catalog()
