ARG PLAYWRIGHT_VERSION=v1.54.1-jammy
FROM mcr.microsoft.com/playwright:${PLAYWRIGHT_VERSION}
RUN apt-get update && apt-get install -y jq && rm -rf /var/lib/apt/lists/*