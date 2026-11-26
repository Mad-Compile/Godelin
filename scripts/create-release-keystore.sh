#!/usr/bin/env bash
set -euo pipefail
umask 077

ALIAS="${GODELIN_RELEASE_KEY_ALIAS:-godelin-release}"
OUT_DIR="${1:-$PWD/private-signing}"
KEYSTORE="$OUT_DIR/godelin-release.jks"

if ! command -v keytool >/dev/null 2>&1; then
  echo "keytool was not found. Install a JDK (Java 17 or newer) and try again." >&2
  exit 1
fi

mkdir -p "$OUT_DIR"
if [[ -e "$KEYSTORE" ]]; then
  echo "Refusing to overwrite existing keystore: $KEYSTORE" >&2
  exit 1
fi

echo "Creating the permanent Godelin release signing key."
echo "Store and key passwords are entered interactively; they are not saved in this script."
keytool -genkeypair -v \
  -keystore "$KEYSTORE" \
  -storetype JKS \
  -alias "$ALIAS" \
  -keyalg RSA \
  -keysize 4096 \
  -sigalg SHA256withRSA \
  -validity 10000 \
  -dname "CN=Godelin Release, O=Mad Compile, C=IN"

chmod 600 "$KEYSTORE"
echo
echo "Keystore created at: $KEYSTORE"
echo "Alias: $ALIAS"
echo
echo "Back up this JKS file offline in at least two secure locations."
echo "Never commit it to Git or send it in an issue, pull request, or chat."
echo
echo "Base64 value for GitHub secret GODELIN_RELEASE_KEYSTORE_BASE64:"
base64 < "$KEYSTORE" | tr -d '\n'
echo
