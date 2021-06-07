#!/usr/bin/env bash
set -Eeuxo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" || exit 1; pwd)"
VERSION=$(grep '<version>' "${SCRIPT_DIR}"/../pom.xml | head -n1 | sed 's/ *<[^>]*>//g')

if ! grep "== ${VERSION}" "${SCRIPT_DIR}"/../CHANGELOG.adoc; then
    echo 'version_check: There is no corresponding section in CHANGELOG'
    exit 1
fi
