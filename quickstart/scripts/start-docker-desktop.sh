#!/usr/bin/env bash

set -eou pipefail

is_docker_running() {
  if ! timeout 5 docker info > /dev/null 2>&1; then
    echo "Docker is not responding within the timeout period."
    return 1
  fi
  return 0
}

echo "Checking if Docker is running..."

# Check if Docker is running
if ! is_docker_running; then
  OS_TYPE="$(uname)"
  echo "Docker is not running. Starting Docker Desktop..."
  case "$OS_TYPE" in
  
  Darwin)
    # macOS
    open -a Docker;;

  Linux)
    # Linux
    sudo systemctl start docker;;

  MINGW*|CYGWIN*|MSYS*)
    # Windows
    powershell.exe -Command "Start-Service docker";;
  
  *)
    echo "Unsupported operating system detected: $OS_TYPE"
    echo "Please start Docker manually on your system."
    exit 1;;
  esac

  # Wait until Docker is running or stop after 5 iterations
  max_attempts=5
  attempt=1
  while ! is_docker_running; do
    echo "Waiting for Docker to start (attempt $attempt/$max_attempts)..."
    sleep 2
    attempt=$((attempt + 1))
    if [ $attempt -ge $max_attempts ]; then
      echo "Error: Docker did not start within the expected time after $max_attempts attempts."
      echo "Please start Docker manually on your system."
      exit 1
    fi
  done
fi

echo "Docker is running."