#!/usr/bin/env bash
# Butuh: JDK 25 + Gradle 9.5.1+ (atau jalankan "gradle wrapper" sekali).
# Pemakaian: ./build.sh [mc_version] [fabric_api_version]
set -e
if [ -n "$1" ] && [ -n "$2" ]; then
  gradle build -Pminecraft_version="$1" -Pfabric_api_version="$2"
else
  gradle build
fi
echo "Selesai. Jar ada di build/libs/ (ambil yang TANPA -sources)."
