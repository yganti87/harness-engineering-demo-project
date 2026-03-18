#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
# codespace-exec.sh — Run Claude Code headlessly inside a GitHub Codespace
#
# Usage:
#   ./scripts/codespace-exec.sh "Execute the plan in .ai/plans/my-feature.md"
#   ./scripts/codespace-exec.sh --interactive   # SSH in and run claude manually
#   ./scripts/codespace-exec.sh --status        # Show codespace status
#
# Configuration:
#   Set CODESPACE_NAME in .codespace.env (gitignored) or pass via environment:
#   CODESPACE_NAME=my-codespace ./scripts/codespace-exec.sh "prompt here"
#
# Prerequisites:
#   - gh CLI authenticated (gh auth login)
#   - ANTHROPIC_API_KEY set as a Codespaces secret
#   - Codespace created and running
# ──────────────────────────────────────────────────────────────────────────────
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_NAME="harness-engineering-demo-project"
WORKDIR="/workspaces/${REPO_NAME}"

# ─── Colors ──────────────────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ─── Load GH_TOKEN from .env if not already set ──────────────────────────────
load_gh_token() {
  if [[ -z "${GH_TOKEN:-}" ]]; then
    local env_file="${PROJECT_ROOT}/.env"
    if [[ -f "$env_file" ]]; then
      local token
      token=$(grep -E '^GH_TOKEN=' "$env_file" 2>/dev/null | head -1 | cut -d= -f2-)
      if [[ -n "$token" ]]; then
        export GH_TOKEN="$token"
      fi
    fi
  fi
  if [[ -z "${GH_TOKEN:-}" ]]; then
    echo -e "${RED}✗ GH_TOKEN not set. Add it to .env or export it.${NC}"
    exit 1
  fi
}

# ─── Load codespace name ────────────────────────────────────────────────────
load_codespace_name() {
  load_gh_token
  # Priority: env var > .codespace.env file
  if [[ -n "${CODESPACE_NAME:-}" ]]; then
    return
  fi

  local env_file="${PROJECT_ROOT}/.codespace.env"
  if [[ -f "$env_file" ]]; then
    # shellcheck disable=SC1090
    source "$env_file"
  fi

  if [[ -z "${CODESPACE_NAME:-}" ]]; then
    echo -e "${RED}✗ CODESPACE_NAME not set.${NC}"
    echo ""
    echo "  Set it in one of these ways:"
    echo "    1. Edit .codespace.env and set CODESPACE_NAME=<name>"
    echo "    2. Export: CODESPACE_NAME=<name> ./scripts/codespace-exec.sh ..."
    echo ""
    echo "  Find your codespace name with: gh codespace list"
    exit 1
  fi
}

# ─── Verify codespace is running ────────────────────────────────────────────
verify_codespace() {
  echo -e "${BLUE}⟳ Checking codespace: ${CODESPACE_NAME}${NC}"

  local status
  status=$(gh codespace view --codespace "$CODESPACE_NAME" --json state -q '.state' 2>/dev/null || echo "NOT_FOUND")

  case "$status" in
    Available)
      echo -e "${GREEN}✓ Codespace is running${NC}"
      ;;
    Shutdown)
      echo -e "${YELLOW}⟳ Codespace is stopped, starting it...${NC}"
      gh codespace start --codespace "$CODESPACE_NAME"
      echo -e "${GREEN}✓ Codespace started${NC}"
      ;;
    NOT_FOUND)
      echo -e "${RED}✗ Codespace '${CODESPACE_NAME}' not found${NC}"
      echo ""
      echo "  Available codespaces:"
      gh codespace list 2>/dev/null || echo "  (none found — is gh authenticated?)"
      exit 1
      ;;
    *)
      echo -e "${YELLOW}⚠ Codespace state: ${status}. Attempting to proceed...${NC}"
      ;;
  esac
}

# ─── Sync local branch to codespace ─────────────────────────────────────────
sync_branch() {
  local current_branch
  current_branch=$(git -C "$PROJECT_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "main")

  echo -e "${BLUE}⟳ Syncing branch '${current_branch}' to codespace...${NC}"

  # Push local branch to remote so codespace can access it
  git -C "$PROJECT_ROOT" push origin "$current_branch" 2>/dev/null || true

  # Checkout the same branch in the codespace
  gh codespace ssh --codespace "$CODESPACE_NAME" -- \
    "cd ${WORKDIR} && git fetch origin && git checkout ${current_branch} && git pull origin ${current_branch}" \
    2>/dev/null || true

  echo -e "${GREEN}✓ Branch synced${NC}"
}

# ─── Show status ─────────────────────────────────────────────────────────────
show_status() {
  load_codespace_name
  echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
  echo -e "${CYAN}║     Codespace Status                     ║${NC}"
  echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
  echo ""
  echo -e "  Name:  ${GREEN}${CODESPACE_NAME}${NC}"
  echo ""
  gh codespace view --codespace "$CODESPACE_NAME" --json state,gitStatus,machineName,repository \
    2>/dev/null || echo -e "  ${RED}Could not retrieve status${NC}"
  echo ""

  echo -e "${BLUE}Checking Claude Code installation...${NC}"
  gh codespace ssh --codespace "$CODESPACE_NAME" -- \
    "which claude 2>/dev/null && claude --version 2>/dev/null || echo 'Claude Code not installed'" \
    2>/dev/null || echo -e "  ${RED}Could not connect to codespace${NC}"
}

# ─── Interactive mode ────────────────────────────────────────────────────────
run_interactive() {
  load_codespace_name
  verify_codespace
  sync_branch

  echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
  echo -e "${CYAN}║     Entering Codespace (interactive)     ║${NC}"
  echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
  echo ""
  echo -e "  Run ${GREEN}claude${NC} to start Claude Code inside the codespace."
  echo -e "  Run ${GREEN}exit${NC} to return to your local machine."
  echo ""

  gh codespace ssh --codespace "$CODESPACE_NAME" -- -t "cd ${WORKDIR} && exec bash -l"
}

# ─── Headless execution ─────────────────────────────────────────────────────
run_headless() {
  local prompt="$1"

  load_codespace_name
  verify_codespace
  sync_branch

  echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
  echo -e "${CYAN}║     Claude Code — Headless Execution     ║${NC}"
  echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
  echo ""
  echo -e "  Codespace:  ${GREEN}${CODESPACE_NAME}${NC}"
  echo -e "  Prompt:     ${YELLOW}${prompt}${NC}"
  echo ""

  # Run Claude in headless (print) mode inside the codespace
  gh codespace ssh --codespace "$CODESPACE_NAME" -- \
    "cd ${WORKDIR} && claude --print --dangerously-skip-permissions '${prompt}'"
}

# ─── Main ────────────────────────────────────────────────────────────────────
main() {
  if [[ $# -eq 0 ]]; then
    echo "Usage:"
    echo "  ./scripts/codespace-exec.sh \"<prompt>\"       # Headless Claude execution"
    echo "  ./scripts/codespace-exec.sh --interactive     # SSH into codespace"
    echo "  ./scripts/codespace-exec.sh --status          # Show codespace info"
    echo ""
    echo "Configuration:"
    echo "  Set CODESPACE_NAME in .codespace.env or export it."
    echo "  Find your codespace: gh codespace list"
    exit 0
  fi

  case "$1" in
    --interactive|-i)
      run_interactive
      ;;
    --status|-s)
      show_status
      ;;
    --help|-h)
      main  # re-call with no args to show usage
      ;;
    *)
      run_headless "$1"
      ;;
  esac
}

main "$@"
