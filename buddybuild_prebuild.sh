#!/usr/bin/env bash
set -e # Exit (and fail) immediately if any command in this scriptfails

# Pre-build script for buddybuild
# http://docs.buddybuild.com/docs/custom-prebuild-and-postbuild-steps#section-pre-build

python tools/l10n/check_translations.py
./gradlew checkstyle