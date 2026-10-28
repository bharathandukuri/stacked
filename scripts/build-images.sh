#!/usr/bin/env bash

set -euo pipefail

export DOCKER_BUILDKIT=1

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
DOCKER_DIR="${ROOT_DIR}/server/src/main/resources/docker"

# Parse CLI arguments
FORCE="${FORCE_REBUILD:-false}"
FILTER=""

for ARG in "$@"; do
    case "$ARG" in
        --force|-f)
            FORCE="true"
            ;;
        *)
            FILTER="$ARG"
            ;;
    esac
done

echo "======================================================================"
echo "Compilr: Building Execution Sandbox Docker Images"
echo "  • Docker contexts: ${DOCKER_DIR}"
echo "  • Force rebuild:   ${FORCE}"
if [ -n "${FILTER}" ]; then
    echo "  • Filter:          ${FILTER}"
fi
echo "======================================================================"

# Strict topological build order ensuring base images exist before child images
BUILD_ORDER=(
    "execution/isolate:1.0|isolate-1_0"
    "execution/java:21|java-21"
    "execution/javascript:node-20|javascript-node-20"
    "execution/c:17|c-17"
    "execution/cpp:23|cpp-23"
    "execution/python:3.12|python-3_12"
    # "execution/go:1.22|go-1_22"
    # "execution/rust:1.75|rust-1_75"
    "execution/mysql:8.0|mysql-8_0"
    "execution/postgres:16|postgres-16"
    "execution/sqlite:3|sqlite-3"
    "execution/mongodb:8.0|mongodb-8_0"
    # "execution/kotlin:1.9|kotlin-1_9"
    # "execution/typescript:5.4|typescript-5_4"
)

TOTAL=${#BUILD_ORDER[@]}
INDEX=1
SKIPPED_COUNT=0
BUILT_COUNT=0

for ENTRY in "${BUILD_ORDER[@]}"; do
    TAG="${ENTRY%%|*}"
    SUBDIR="${ENTRY##*|}"

    # If a filter is provided, skip non-matching entries
    if [ -n "${FILTER}" ]; then
        if [[ "${TAG}" != *"${FILTER}"* ]] && [[ "${SUBDIR}" != *"${FILTER}"* ]]; then
            INDEX=$((INDEX + 1))
            continue
        fi
    fi

    # Check if image already exists in local Docker daemon
    if docker image inspect "${TAG}" >/dev/null 2>&1 && [ "${FORCE}" != "true" ]; then
        echo "[${INDEX}/${TOTAL}] [CACHED] ${TAG} already exists (use --force to rebuild)"
        SKIPPED_COUNT=$((SKIPPED_COUNT + 1))
    else
        echo ""
        echo "[${INDEX}/${TOTAL}] [BUILDING] ${TAG} from ${SUBDIR}..."
        BUILD_FLAGS=(--network=host)
        if [ "${FORCE}" = "true" ]; then
            BUILD_FLAGS+=(--no-cache)
        fi
        docker build "${BUILD_FLAGS[@]}" -t "${TAG}" "${DOCKER_DIR}/${SUBDIR}"
        BUILT_COUNT=$((BUILT_COUNT + 1))
    fi

    INDEX=$((INDEX + 1))
done

echo ""
echo "======================================================================"
echo "Execution Images Status:"
echo "  • Built:   ${BUILT_COUNT}"
echo "  • Cached:  ${SKIPPED_COUNT}"
echo "======================================================================"