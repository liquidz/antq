#!/usr/bin/env bash
set -Euxo pipefail

cd test/resources/integration-testing/red1 || exit 1

if lein with-profile -user antq; then
  echo "Should have failed!"
  exit 1
fi

cd ../red2 || exit 1

if lein with-profile -user antq; then
  echo "Should have failed!"
  exit 1
fi

cd ../red3 || exit 1

if lein with-profile -user antq; then
  echo "Should have failed!"
  exit 1
fi

cd ../green || exit 1

if ! lein with-profile -user antq; then
  echo "Should have passed!"
  exit 1
fi

exit 0
