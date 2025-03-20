#!/usr/bin/env node
// Copyright (c) 2025, Digital Asset (Switzerland) GmbH and/or its affiliates. All rights reserved.
// SPDX-License-Identifier: 0BSD

/**
 * Name: setup-and-run-tests.js
 *
 * Description:
 *   The purpose of this script is to efficiently run the end-to-end test suite for the Quickstart project in an isolated environment.
 *
 *   The script manages a docker-in-docker container that starts the application with all its dependencies (LocalNet)
 *   and then runs the integration tests inside a nested Docker container.
 *
 *   1. Builds the local project artifacts (via Makefile).
 *   2. Builds (or reuses) a Docker-in-Docker image from the provided Dockerfile.
 *   3. Runs that image in a container (named "quickstart-end2end-runner").
 *   4. Collects required images from the docker-compose configuration, checks if they exist in the running container, and if not, transfers them via a volume.
 *      This resolves authentication issues and speeds up the process for images already present on the host.
 *   5. Starts the application inside the container, creates four app-install requests, and executes an integration test via nested Docker.
 *
 * Usage:
 *   ./setup-and-run-tests.js [--force-rebuild]
 *
 *   --force-rebuild: Optional flag to force a rebuild of the Docker image.
 */

const { spawn } = require('child_process');
const { join } = require('path');
const process = require('process');

function runCommand(command, args, options = {}) {
    return new Promise((resolve, reject) => {
        console.log(`[RUN] ${command} ${args.join(' ')}`);

        const child = spawn(command, args, {
            stdio: 'inherit',
            ...options,
        });

        child.on('error', (error) => {
            console.error(`[ERROR] Failed to start command: ${command}`, error);
            reject(error);
        });

        child.on('close', (code) => {
            if (code !== 0) {
                reject(
                    new Error(`Command "${command} ${args.join(' ')}" exited with code ${code}`)
                );
            } else {
                resolve();
            }
        });
    });
}

function runCommandCaptureStdout(command, args = [], options = {}) {
    return new Promise((resolve, reject) => {
        console.log(`[RUN CAPTURE] ${command} ${args.join(' ')}`);

        const child = spawn(command, args, {
            shell: false,
            stdio: ['ignore', 'pipe', 'inherit'],
            ...options,
        });

        let stdoutData = '';
        child.stdout.on('data', (data) => {
            stdoutData += data;
        });

        child.on('error', (error) => {
            console.error(`[ERROR] Failed to start command: ${command}`, error);
            reject(error);
        });

        child.on('close', (code) => {
            if (code !== 0) {
                reject(
                    new Error(`Command "${command} ${args.join(' ')}" exited with code ${code}`)
                );
            } else {
                resolve(stdoutData.trim());
            }
        });
    });
}

function pipeCommands(producerCmd, consumerCmd) {
    return new Promise((resolve, reject) => {
        const [prod, ...prodArgs] = producerCmd;
        const [cons, ...consArgs] = consumerCmd;

        console.log(`[PIPE] ${producerCmd.join(' ')} | ${consumerCmd.join(' ')}`);

        const producer = spawn(prod, prodArgs, { stdio: ['ignore', 'pipe', 'inherit'] });
        const consumer = spawn(cons, consArgs, { stdio: ['pipe', 'inherit', 'inherit'] });

        producer.stdout.pipe(consumer.stdin);

        producer.on('error', (error) => {
            console.error('[ERROR] Producer process failed to start:', error);
            reject(error);
        });

        consumer.on('error', (error) => {
            console.error('[ERROR] Consumer process failed to start:', error);
            reject(error);
        });

        consumer.on('close', (code) => {
            if (code !== 0) {
                reject(
                    new Error(`Consumer command "${cons} ${consArgs.join(' ')}" exited with code ${code}`)
                );
            } else {
                resolve();
            }
        });
    });
}

async function imageExistsLocally(imageTag) {
    try {
        await runCommandCaptureStdout('docker', ['image', 'inspect', imageTag]);
        return true;
    } catch {
        return false;
    }
}

async function containerHasImage(containerName, image) {
    try {
        await runCommandCaptureStdout('docker', [
            'exec',
            containerName,
            'docker',
            'image',
            'inspect',
            image,
        ]);
        return true;
    } catch {
        return false;
    }
}

(async () => {
    try {
        const rawArgs = process.argv.slice(2);
        const flags = rawArgs.filter((arg) => arg.startsWith('--'));
        const forceRebuild = flags.includes('--force-rebuild');

        const containerName = 'quickstart-end2end-runner';

        const scriptDir = __dirname;
        const projectRoot = join(scriptDir, '../');
        const dockerfilePath = join(scriptDir, 'docker/Dockerfile');
        const imageTag = `${containerName}:latest`;

        console.log('[INFO] Checking if the Docker image already exists...');
        const imageAlreadyExists = await imageExistsLocally(imageTag);

        if (forceRebuild) {
            console.log('[INFO] Removing existing volume "quickstart-e2e-dind-data" if it exists...');
            await runCommand('docker', ['volume', 'rm', 'quickstart-e2e-dind-data'], { shell: true })
                .catch(() => null);
        }

        if (forceRebuild || !imageAlreadyExists) {
            console.log(`[INFO] Building Docker image "${imageTag}"...`);
            await runCommand('docker', [
                'build',
                '-t',
                imageTag,
                projectRoot,
                '-f',
                dockerfilePath,
            ]);
        } else {
            console.log(`[INFO] Using existing image "${imageTag}".`);
        }

        console.log(`[INFO] Removing old container "${containerName}" if it exists...`);
        await runCommand('docker', ['rm', '-f', containerName], { shell: true }).catch(() => {});

        console.log(`[INFO] Running container "${containerName}" in detached mode...`);
        await runCommand('docker', [
            'run',
            '--rm',
            '-d',
            '--name',
            containerName,
            '--privileged',
            '-v',
            'quickstart-e2e-dind-data:/var/lib/docker',
            '-v',
            `${projectRoot}:/app:ro`,
            '-v',
            `${projectRoot}/integration-test/docker/env.local:/app/.env.local:ro`,
            imageTag,
        ]);

        console.log('[INFO] Waiting for container to be ready...');
        for (let i = 0; i < 30; i++) {
            try {
                await runCommandCaptureStdout('docker', [
                    'exec',
                    containerName,
                    'curl',
                    '--fail',
                    '--unix-socket',
                    '/var/run/docker.sock',
                    'http://localhost/version',
                ]);
                break;
            } catch {
                if (i === 29) {
                    throw new Error('[ERROR] Container did not become ready in time.');
                }
                await new Promise((resolve) => setTimeout(resolve, 2000));
            }
        }

        console.log('[INFO] Fetching list of images from docker-compose config (JSON)...');
        const composeConfigArgs = [
            'compose',
            '-f',
            'compose.yaml',
            '--env-file',
            '.env.local',
            '--env-file',
            '.env',
            '--profile',
            'localnet',
            '--env-file',
            'docker/localnet.env',
            '--profile',
            'observability',
            '-f',
            'docker/o11y/cadvisor-linux.yaml',
            '-f',
            'docker/o11y/compose.yaml',
            'config',
            '--format',
            'json',
        ];
        const rawConfig = await runCommandCaptureStdout('docker', composeConfigArgs, {
            cwd: projectRoot,
        });
        let parsed;
        try {
            parsed = JSON.parse(rawConfig);
        } catch {
            throw new Error('[ERROR] Failed to parse docker-compose config as JSON.');
        }
        if (!parsed?.services) {
            throw new Error('[ERROR] No services found in docker-compose configuration.');
        }
        let images = Object.values(parsed.services)
            .map((service) => service.image)
            .filter(Boolean);
        images = Array.from(new Set(images)).sort();
        if (!images.length) {
            throw new Error('[ERROR] No images found in docker-compose configuration.');
        }

        console.log('[INFO] Checking and transferring required images into the container...');
        for (const image of images) {
            const hasImage = await containerHasImage(containerName, image);
            if (hasImage) {
                console.log(`[INFO] Container already has image "${image}". Skipping transfer.`);
            } else {
                console.log(`\n[INFO] Pulling image "${image}"...`);
                await runCommand('docker', ['pull', image]);

                console.log(`[INFO] Transferring image "${image}" into container "${containerName}"...`);
                await pipeCommands(
                    ['docker', 'save', image],
                    ['docker', 'exec', '-i', containerName, 'docker', 'load']
                );
            }
        }

        console.log('[INFO] Ensuring fresh slate...');
        await runCommand('docker', [
            'exec',
            containerName,
            'bash',
            '-c',
            'docker rm -f $(docker ps -aq) || true'
        ]);
        await runCommand('docker', [
            'exec',
            containerName,
            'docker',
            'network',
            'prune',
            '-f'
        ]);
        await runCommand('docker', [
            'exec',
            containerName,
            'bash',
            '-c',
            'docker volume rm $(docker volume ls -q) || true'
        ]);

        console.log('[INFO] Starting the application inside the container...');
        await runCommand('docker', [
            'exec',
            containerName,
            'make',
            '-o',
            'build',
            'start',
        ]);

        console.log('[INFO] Creating four app-install requests (expected by end2end test)...');
        for (let i = 1; i <= 4; i++) {
            await runCommand('docker', [
                'exec',
                containerName,
                'make',
                'create-app-install-request',
            ]);
        }

        console.log('[INFO] Running integration tests via nested Docker container...');
        await runCommand('docker', [
            'exec',
            containerName,
            'bash',
            '-c',
            'docker run --rm --net host --user "$(id -u):$(id -g)" ' +
            '-v "$PWD/integration-test/":/work -w /work ' +
            'mcr.microsoft.com/playwright:v1.51.0-jammy ' +
            'npx playwright test --output /tmp/ --reporter line',
        ]);

        console.log('[SUCCESS] All tasks completed successfully.');
    } catch (error) {
        console.error(`[FATAL] Script failed: ${error.message}`);
        process.exit(1);
    } finally {
        console.log(`[INFO] Removing container "${containerName}"...`);
        await runCommand('docker', ['rm', '-f', containerName]);
    }
})();
