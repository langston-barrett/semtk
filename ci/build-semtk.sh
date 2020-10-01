#!/usr/bin/env bash

set -eo pipefail

# DIR = directory containing this script
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd -P)"
. "${DIR}/../.env"

# TODO: The tests currently fail in Github Actions.
mvn --quiet --batch-mode clean install -DskipTests
