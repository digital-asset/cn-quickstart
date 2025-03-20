# Use DinD base image for Docker-in-Docker capability
FROM docker:28-dind

# Install required packages: Node.js, NPM, Docker Compose, wget, curl, git, and OpenJDK (for gradle)
RUN apk add --no-cache \
  nodejs \
  npm \
  docker-compose \
  bash \
  curl \
  git \
  wget \
  openjdk17 \
  make \
  && rm -rf /var/cache/apk/*

# Set working directory
WORKDIR /app

# Ensure execute permissions for scripts (if applicable)
RUN chmod +x ./docker/**/*.sh || true

# Copy and prepare entrypoint script
COPY integration-test/docker/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

VOLUME /var/lib/docker

ENTRYPOINT ["docker-entrypoint.sh"]

HEALTHCHECK --interval=5s --timeout=5s --retries=12 CMD curl --fail --unix-socket /var/run/docker.sock http://localhost/version || exit 1

CMD ["tail", "-f", "/dev/null"]
