#!/usr/bin/env bash
set -euo pipefail

function jfrog_netrc() {
  echo "[INFO] adding jfrog credentials to ${HOME}/.netrc"
  touch ${HOME}/.netrc
  echo "machine digitalasset.jfrog.io" >> ${HOME}/.netrc
  echo "login ${ARTIFACTORY_USER}" >> ${HOME}/.netrc
  echo "password ${ARTIFACTORY_TOKEN}" >> ${HOME}/.netrc
}

function jfrog_docker_auth() {
  echo "[INFO] docker login for digitalasset-docker.jfrog.io"
  echo "${ARTIFACTORY_TOKEN}" | docker login --password-stdin --username "${ARTIFACTORY_USER}" digitalasset-docker.jfrog.io
}

jfrog_docker_auth
jfrog_netrc