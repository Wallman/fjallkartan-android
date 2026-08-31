#!/usr/bin/env bash
#
# Bumps versionCode (and optionally versionName) in app/build.gradle.kts and
# uploads the resulting build to Google Play with fastlane.
#
# Usage:
#   tools/release.sh [--version-name VERSION_NAME] [--lane LANE] [--no-commit]
#
# Examples:
#   tools/release.sh                                   # bump versionCode only, keep versionName
#   tools/release.sh --version-name 1.1
#   tools/release.sh --version-name 1.1 --lane internal
#   tools/release.sh --lane promote -- from:internal to:production
#
# Requires the same environment variables as fastlane itself
# (GOOGLE_PLAY_JSON_KEY and, for lanes that build, the FJALLKARTAN_* signing
# variables) -- see AGENTS.md for details.

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: tools/release.sh [--version-name VERSION_NAME] [--lane LANE] [--no-commit] [-- LANE_ARGS...]

Options:
  --version-name NAME   New versionName (e.g. 1.1). Optional; versionCode is
                         always bumped, versionName is left untouched if omitted.
  --lane LANE           Fastlane lane to run after bumping (default: closed).
  --no-commit           Skip creating a git commit for the version bump.
  -h, --help            Show this help.

Anything after a literal "--" is passed through to the fastlane lane, e.g.:
  tools/release.sh --lane promote -- from:internal to:production
EOF
}

version_name=""
lane="closed"
do_commit=1
lane_args=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --version-name)
      version_name="${2:?--version-name requires a value}"
      shift 2
      ;;
    --lane)
      lane="${2:?--lane requires a value}"
      shift 2
      ;;
    --no-commit)
      do_commit=0
      shift
      ;;
    -h | --help)
      usage
      exit 0
      ;;
    --)
      shift
      lane_args=("$@")
      break
      ;;
    *)
      echo "error: unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
build_file="$root_dir/app/build.gradle.kts"

if [[ ! -f "$build_file" ]]; then
  echo "error: $build_file not found" >&2
  exit 1
fi

if [[ -n "$(git -C "$root_dir" status --porcelain -- "$build_file")" ]]; then
  echo "error: $build_file has uncommitted changes; commit or stash before running" >&2
  exit 1
fi

current_version_code="$(grep -oE 'versionCode = [0-9]+' "$build_file" | grep -oE '[0-9]+' || true)"
if [[ -z "$current_version_code" ]]; then
  echo "error: could not find versionCode in $build_file" >&2
  exit 1
fi
new_version_code=$((current_version_code + 1))

echo "Bumping versionCode: $current_version_code -> $new_version_code"
perl -pi -e "s/versionCode = [0-9]+/versionCode = ${new_version_code}/" "$build_file"
if ! grep -q "versionCode = ${new_version_code}" "$build_file"; then
  echo "error: failed to update versionCode in $build_file" >&2
  exit 1
fi

if [[ -n "$version_name" ]]; then
  echo "Setting versionName: $version_name"
  perl -pi -e "s/versionName = \"[^\"]*\"/versionName = \"${version_name}\"/" "$build_file"
  if ! grep -qF "versionName = \"${version_name}\"" "$build_file"; then
    echo "error: failed to update versionName in $build_file" >&2
    exit 1
  fi
fi

if [[ "$do_commit" -eq 1 ]]; then
  git -C "$root_dir" add "$build_file"
  commit_message="Bump versionCode to ${new_version_code}"
  [[ -n "$version_name" ]] && commit_message="Bump version to ${version_name} (${new_version_code})"
  git -C "$root_dir" commit -m "$commit_message"
fi

echo "Running: bundle exec fastlane android ${lane} ${lane_args[*]}"
(cd "$root_dir" && bundle exec fastlane android "$lane" "${lane_args[@]}")

