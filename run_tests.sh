#!/usr/bin/env bash
# kizashi — local actor-boundary suite, bb/clj.
set -euo pipefail
cd "$(dirname "$0")"
exec bb test
