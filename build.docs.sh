#!/bin/sh

set -e

# Setup the version as the first arg or default to "latest"
VERSION=${1:-"latest"}

# Remove any existing documentation
rm -rf build/apidocs

# Create the default output directory: build/apidocs/$VERSION
mkdir -p build/apidocs/$VERSION

# Generate the documentation
boxlang module:docbox \
	--source="./src/main/bx" \
	--mapping="bxModules.bxai" \
	--output-dir="build/apidocs/$VERSION" \
	--project-title="BoxLang AI v$VERSION" \
	--project-description="BoxLang AI Documentation" \
