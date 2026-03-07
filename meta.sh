#!/bin/bash

set -e

clean() {
    echo "Cleaning build artifacts..."
    find . -type d \( -name "build" -o -name ".gradle" -o -name ".kotlin" \) -exec rm -rf {} +
}

check_copyright() {
    exit=0
    for file in $(find . -type f -name "*.kt" ! -path "*/build/*"); do
        if ! grep -E -q "Copyright \(c\) 20[0-9]{2}(-20[0-9]{2})? Bastiaan van der Plaat" "$file"; then
            echo "Bad copyright header in: $file"
            exit=1
        fi
    done
    if [ "$exit" -ne 0 ]; then
        exit 1
    fi
}

build() {
    echo "Building application..."
    ./gradlew assembleDebug
}

check() {
    check_copyright
    build
}

case "${1:-check}" in
    clean)
        clean
        ;;
    check)
        check
        ;;
    *)
        echo "Usage: $0 {clean|check}"
        exit 1
        ;;
esac
