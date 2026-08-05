#!/usr/bin/env bash

# Kept POSIX-compatible so it also runs under `sh`/dash, which has no `pipefail`. There are no
# pipelines here, so `set -eu` is sufficient. The Gradle build does not invoke this script; run it
# yourself after cloning without --recursive.
set -eu

# Fetch all submodule content.
git submodule sync --recursive
git submodule update --force --recursive --init
