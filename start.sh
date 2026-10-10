#!/bin/bash
export MERCURY_BASE_URL=http://localhost:11434/v1
export MERCURY_MODEL=qwen3.8
export MERCURY_SEARXNG_URL=http://localhost:8080
export MERCURY_API_KEY=""

# Smoke-Test (ohne Netzwerk)
mvn -q compile exec:java -Dexec.mainClass=mercury.LoopSmokeTest -Dexec.classpathScope=compile

# Research Agent
mvn -q compile exec:java -Dexec.mainClass=mercury.Main -Dexec.args="research Deine Forschungsfrage"

# Chat-Profil
mvn -q compile exec:java -Dexec.mainClass=mercury.Main
