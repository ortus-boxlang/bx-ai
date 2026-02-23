#!/usr/bin/env bash
set -euo pipefail

echo "==> Post-create starting..."

echo "==> Java version:"
java -version

echo "==> Waiting for Cassandra at cassandra:9042..."
for i in {1..60}; do
  if (echo > /dev/tcp/cassandra/9042) >/dev/null 2>&1; then
    echo "==> Cassandra port is open."
    break
  fi
  sleep 2
done

BOXLANG_LIB_DIR="src/test/resources/libs"
mkdir -p "${BOXLANG_LIB_DIR}"

BOXLANG_VERSION=""
if [[ -f "gradle.properties" ]]; then
  BOXLANG_VERSION="$(grep -E '^boxlangVersion=' gradle.properties | cut -d'=' -f2 || true)"
fi

if [[ -z "${BOXLANG_VERSION}" ]]; then
  BOXLANG_VERSION="$(grep -E "boxlangVersion\s*=" build.gradle | head -n 1 | sed -E "s/.*boxlangVersion\s*=\s*['\"]([^'\"]+)['\"].*/\1/" || true)"
fi

if [[ -z "${BOXLANG_VERSION}" ]]; then
  echo "ERROR: Could not determine boxlangVersion from gradle.properties or build.gradle."
  exit 1
fi

BOXLANG_JAR="${BOXLANG_LIB_DIR}/boxlang-${BOXLANG_VERSION}.jar"

if [[ -s "${BOXLANG_JAR}" ]]; then
  echo "==> BoxLang jar already present: ${BOXLANG_JAR}"
else
  echo "==> BoxLang jar missing. Attempting download..."

  RELEASE_URL="https://github.com/ortus-boxlang/boxlang/releases/download/v${BOXLANG_VERSION}/boxlang-${BOXLANG_VERSION}.jar"

  curl -fL "${RELEASE_URL}" -o "${BOXLANG_JAR}" || true

  if [[ ! -s "${BOXLANG_JAR}" ]]; then
    echo "ERROR: Failed to download BoxLang jar from:"
    echo "  ${RELEASE_URL}"
    echo "Fix options:"
    echo "  1) Place the correct jar at ${BOXLANG_JAR}"
    echo "  2) Or clone/build boxlang next to this repo so ../../boxlang/build/libs exists"
    exit 1
  fi

  echo "==> Downloaded BoxLang jar: ${BOXLANG_JAR}"
fi

# Ensure integration test harness doesn't crash when secrets aren't present
if [ ! -f "src/test/resources/.env" ]; then
  cat > "src/test/resources/.env" <<'EOF'
# Test placeholder file for CI/Codespaces.
# Integration tests will skip unless required variables are set.
DUMMY=1
EOF
fi

echo "==> Gradle sanity check:"
./gradlew --version

echo "==> Post-create complete."
