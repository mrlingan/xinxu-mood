#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -z "${JAVA_HOME:-}" ]; then
  for candidate in "$PWD"/.tools/jdk-17*/Contents/Home; do
    if [ -x "$candidate/bin/java" ]; then export JAVA_HOME="$candidate"; break; fi
  done
fi
if [ -z "${JAVA_HOME:-}" ]; then
  JAVA_HOME=$(/usr/libexec/java_home -v 17 2>/dev/null || true)
  export JAVA_HOME
fi
if [ -z "${JAVA_HOME:-}" ]; then
  echo '请安装 JDK 17 并设置 JAVA_HOME，再执行本脚本。'
  exit 1
fi
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-parallel --max-workers=1 "$@"
