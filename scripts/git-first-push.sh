#!/usr/bin/env bash
# First-time git setup and push to remote
# Usage: ./scripts/git-first-push.sh <remote-url>
# Example: ./scripts/git-first-push.sh https://github.com/yourusername/library-app.git

set -e
REMOTE_URL="${1:?Usage: $0 <remote-url>}"

cd "$(dirname "$0")/.."

if [ ! -d .git ]; then
  git init
  echo "Initialized git repository."
fi

git add -A
git status

echo ""
read -p "Commit message [Initial commit: Library catalog app with book search]: " MSG
MSG="${MSG:-Initial commit: Library catalog app with book search}"
git commit -m "$MSG"

git branch -M main
git remote add origin "$REMOTE_URL" 2>/dev/null || git remote set-url origin "$REMOTE_URL"
git push -u origin main

echo "Done. Pushed to $REMOTE_URL"
