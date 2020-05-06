#!/bin/bash

SCRIPT_DIR=$(cd $(dirname $0); pwd)
VERSION=$(grep '<version>' ${SCRIPT_DIR}/../pom.xml | head -n1 | sed 's/ *<[^>]*>//g')

grep "== ${VERSION}" ${SCRIPT_DIR}/../CHANGELOG.adoc
if [ $? -ne 0 ]; then
    echo 'version_check: There is no corresponding section in CHANGELOG'
    exit 1
fi
