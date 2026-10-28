#!/usr/bin/env bash
# ==============================================================================
# Stacked v1.0 - Simplified One-Command Deployment Script
# ==============================================================================
# Usage:
#   ./deploy.sh             Deploy full stack (builds & starts all containers)
#   ./deploy.sh --seed      Deploy full stack and seed 15 foundational coding problems
#   ./deploy.sh --down      Stop and remove all Stacked containers and networks
#   ./deploy.sh --restart   Restart all Stacked containers
#   ./deploy.sh --logs      Follow live container logs (e.g. ./deploy.sh --logs server)
#   ./deploy.sh --status    Check health and container statuses
#   ./deploy.sh --help      Display usage information
# ==============================================================================

set -euo pipefail

# Colors & Formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${ROOT_DIR}"

# ------------------------------------------------------------------------------
# Helper Log Functions
# ------------------------------------------------------------------------------
log_info() {
    echo -e "${CYAN}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1" >&2
}

banner() {
    echo -e "${BOLD}${BLUE}"
    echo "  ███████╗████████╗ █████╗  ██████╗██╗  ██╗███████╗██████╗ "
    echo "  ██╔════╝╚══██╔══╝██╔══██╗██╔════╝██║ ██╔╝██╔════╝██╔══██╗"
    echo "  ███████╗   ██║   ███████║██║     █████╔╝ █████╗  ██║  ██║"
    echo "  ╚════██║   ██║   ██╔══██║██║     ██╔═██╗ ██╔══╝  ██║  ██║"
    echo "  ███████║   ██║   ██║  ██║╚██████╗██║  ██╗███████╗██████╔╝"
    echo "  ╚══════╝   ╚═╝   ╚═╝  ╚═╝ ╚═════╝╚═╝  ╚═╝╚══════╝╚═════╝   v1.0"
    echo -e "${NC}"
}

# ------------------------------------------------------------------------------
# Dependency & Environment Checks
# ------------------------------------------------------------------------------
detect_docker() {
    if ! command -v docker >/dev/null 2>&1; then
        log_error "Docker is not installed or not in PATH. Please install Docker first."
        exit 1
    fi

    if ! docker info >/dev/null 2>&1; then
        log_error "Docker daemon is not running. Please start the Docker service."
        exit 1
    fi

    # Detect compose command
    if docker compose version >/dev/null 2>&1; then
        COMPOSE_CMD="docker compose"
    elif command -v docker-compose >/dev/null 2>&1; then
        COMPOSE_CMD="docker-compose"
    else
        log_error "Neither 'docker compose' nor 'docker-compose' was found."
        exit 1
    fi
}

check_env() {
    if [ ! -f "${ROOT_DIR}/.env" ]; then
        log_warn ".env file not found. Initializing from .env.example..."
        if [ -f "${ROOT_DIR}/.env.example" ]; then
            cp "${ROOT_DIR}/.env.example" "${ROOT_DIR}/.env"
        else
            log_error ".env.example file missing. Cannot initialize environment."
            exit 1
        fi

        # Generate a secure 256-bit JWT secret if not configured
        if grep -q "JWT_SECRET=$" "${ROOT_DIR}/.env" || grep -q "JWT_SECRET=\"\"" "${ROOT_DIR}/.env" || ! grep -q "JWT_SECRET=" "${ROOT_DIR}/.env"; then
            local gen_secret
            if command -v openssl >/dev/null 2>&1; then
                gen_secret=$(openssl rand -hex 32)
            else
                gen_secret=$(python3 -c "import secrets; print(secrets.token_hex(32))")
            fi
            sed -i "s|^JWT_SECRET=.*|JWT_SECRET=${gen_secret}|" "${ROOT_DIR}/.env"
            log_success "Generated random 256-bit JWT_SECRET for security."
        fi
    fi
}

# ------------------------------------------------------------------------------
# Sandbox Execution Images
# ------------------------------------------------------------------------------
ensure_execution_images() {
    local force_flag=""
    if [ "${1:-}" = "--force" ]; then
        force_flag="--force"
    fi

    log_info "Verifying sandboxed execution Docker images (Isolate, Python, Java, JS, C++, SQL)..."
    if [ -f "${ROOT_DIR}/scripts/build-images.sh" ]; then
        chmod +x "${ROOT_DIR}/scripts/build-images.sh"
        "${ROOT_DIR}/scripts/build-images.sh" ${force_flag}
    else
        log_warn "scripts/build-images.sh not found. Skipping execution sandbox images check."
    fi
}

# ------------------------------------------------------------------------------
# Health Check Wait Loop
# ------------------------------------------------------------------------------
wait_for_healthy() {
    local max_wait=90
    local elapsed=0
    local server_ready=false

    log_info "Waiting for application services to become healthy (timeout: ${max_wait}s)..."

    while [ ${elapsed} -lt ${max_wait} ]; do
        # Check HTTP response from server endpoint
        if curl -s -f -o /dev/null "http://localhost:8080/api/auth/me" 2>/dev/null || \
           curl -s -f -o /dev/null "http://localhost:8080/actuator/health" 2>/dev/null || \
           curl -s "http://localhost:8080/api" 2>/dev/null | grep -q -E "(timestamp|status|error)"; then
            server_ready=true
            break
        fi

        sleep 2
        elapsed=$((elapsed + 2))
        echo -ne "  • Waiting for backend initialization... (${elapsed}s / ${max_wait}s)\r"
    done
    echo ""

    if [ "${server_ready}" = "true" ]; then
        log_success "Backend API is up and healthy!"
    else
        log_warn "Backend did not respond within ${max_wait}s. Checking container status..."
        ${COMPOSE_CMD} ps
    fi
}

# ------------------------------------------------------------------------------
# Seed Database
# ------------------------------------------------------------------------------
seed_database() {
    log_info "Seeding 15 foundational coding problems (DSA + SQL)..."
    if [ -f "${ROOT_DIR}/scripts/seed_coding_problems.py" ]; then
        if command -v python3 >/dev/null 2>&1; then
            # Ensure requests is installed or inform user
            if python3 -c "import requests" >/dev/null 2>&1; then
                python3 "${ROOT_DIR}/scripts/seed_coding_problems.py"
                log_success "Database seeded successfully!"
            else
                log_warn "'requests' module not found in Python. Installing temporarily..."
                python3 -m pip install requests --quiet || pip install requests --quiet || true
                python3 "${ROOT_DIR}/scripts/seed_coding_problems.py"
                log_success "Database seeded successfully!"
            fi
        else
            log_error "Python 3 is required to run the seed script."
        fi
    else
        log_error "scripts/seed_coding_problems.py not found."
    fi
}

# ------------------------------------------------------------------------------
# Print Deployment Summary
# ------------------------------------------------------------------------------
print_summary() {
    echo ""
    echo -e "${BOLD}${GREEN}======================================================================${NC}"
    echo -e "${BOLD}${GREEN}         🎉 STACKED v1.0 APPLICATION DEPLOYED SUCCESSFULLY!          ${NC}"
    echo -e "${BOLD}${GREEN}======================================================================${NC}"
    echo ""
    echo -e "  ${BOLD}Web Application:${NC}   ${CYAN}http://localhost:3000${NC}"
    echo -e "  ${BOLD}Backend API:${NC}       ${CYAN}http://localhost:8080/api${NC}"
    echo -e "  ${BOLD}MongoDB:${NC}           localhost:27017 (db: stacked_db)"
    echo -e "  ${BOLD}Redis:${NC}             localhost:6379"
    echo ""
    echo -e "  ${BOLD}Default Admin Credentials:${NC}"
    echo -e "  • Email:    ${YELLOW}admin@stacked.com${NC}"
    echo -e "  • Password: ${YELLOW}Admin@123456${NC}"
    echo ""
    echo -e "  ${BOLD}Helpful Commands:${NC}"
    echo -e "  • Seed problems:   ${CYAN}./deploy.sh --seed${NC}"
    echo -e "  • View logs:       ${CYAN}./deploy.sh --logs${NC}"
    echo -e "  • Stop all:        ${CYAN}./deploy.sh --down${NC}"
    echo -e "${BOLD}${GREEN}======================================================================${NC}"
    echo ""
}

# ------------------------------------------------------------------------------
# Action Handlers
# ------------------------------------------------------------------------------
show_help() {
    banner
    echo "Usage: ./deploy.sh [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  (none)            Build and deploy full application stack"
    echo "  --seed            Build, deploy, and seed 15 foundational coding problems"
    echo "  --build-images    Force rebuild execution sandbox images (Isolate/Python/Java/etc.)"
    echo "  --restart         Restart application containers"
    echo "  --down            Stop and tear down application containers and networks"
    echo "  --logs [service]  Follow logs for all containers or a specific service (e.g. server, client)"
    echo "  --status          View container status and health check state"
    echo "  --help, -h        Show this help message"
    echo ""
}

do_deploy() {
    local seed_after="${1:-false}"
    local force_images="${2:-false}"

    banner
    detect_docker
    check_env

    # 1. Start DinD execution service first
    log_info "Starting DinD execution daemon on backend-network..."
    ${COMPOSE_CMD} up -d dind

    # Wait for DinD daemon readiness
    log_info "Waiting for DinD daemon readiness on tcp://127.0.0.1:2375..."
    local dind_ready=false
    for i in {1..30}; do
        if docker -H tcp://127.0.0.1:2375 version >/dev/null 2>&1; then
            dind_ready=true
            break
        fi
        sleep 1
    done

    if [ "${dind_ready}" = "true" ]; then
        log_success "DinD daemon is ready."
        export DOCKER_HOST="tcp://127.0.0.1:2375"
    else
        log_warn "DinD not responding on tcp://127.0.0.1:2375 yet; continuing with environment config."
    fi

    # 2. Ensure execution sandbox images exist in DinD
    if [ "${force_images}" = "true" ]; then
        ensure_execution_images --force
    else
        ensure_execution_images
    fi

    # Unset DOCKER_HOST so subsequent compose commands target the host Docker engine
    unset DOCKER_HOST

    # 3. Build & launch application stack
    echo ""
    log_info "Building and starting Stacked containers via ${COMPOSE_CMD}..."
    ${COMPOSE_CMD} up -d --build

    # 3. Wait for services to be ready
    wait_for_healthy

    # 4. Optional seed
    if [ "${seed_after}" = "true" ]; then
        echo ""
        seed_database
    fi

    # 5. Display summary
    print_summary
}

# ------------------------------------------------------------------------------
# Entrypoint
# ------------------------------------------------------------------------------
main() {
    detect_docker

    case "${1:-}" in
        --seed)
            do_deploy true false
            ;;
        --build-images|-b)
            do_deploy false true
            ;;
        --down)
            log_info "Stopping and removing Stacked containers..."
            ${COMPOSE_CMD} down
            log_success "All Stacked containers stopped."
            ;;
        --restart)
            log_info "Restarting Stacked containers..."
            ${COMPOSE_CMD} restart
            wait_for_healthy
            log_success "Restart complete."
            ;;
        --logs)
            shift
            ${COMPOSE_CMD} logs -f "$@"
            ;;
        --status)
            ${COMPOSE_CMD} ps
            ;;
        --help|-h)
            show_help
            ;;
        "")
            do_deploy false false
            ;;
        *)
            log_error "Unknown option: $1"
            echo "Run './deploy.sh --help' for usage."
            exit 1
            ;;
    esac
}

main "$@"

