#!/usr/bin/env bash
# Builds a native installer with jpackage: a jlink'ed runtime with only the modules the app needs
# (no JDK required on the user's machine) plus the application, which opens on the bundled
# champion (--demo) when launched without arguments.
#
# Usage: packaging/jpackage.sh TYPE      TYPE: deb, rpm, msi, exe, dmg, pkg or app-image
#
# Run from the repository root after `./mvnw package`. Output goes to target/installer/.
# Uses the JDK on PATH (21+); on Windows, msi and exe also need WiX Toolset 3 on PATH.
set -euo pipefail

TYPE="${1:?usage: packaging/jpackage.sh deb|rpm|msi|exe|dmg|pkg|app-image}"
NAME="FlappyBirdNEAT"
MODULE="com.neat.flappybirdneat"
MAIN_CLASS="com.neat.flappybirdneat.Main"

if [ "${OS:-}" = "Windows_NT" ]; then MVNW=./mvnw.cmd; else MVNW=./mvnw; fi
VERSION="$("$MVNW" -q -B help:evaluate -Dexpression=project.version -DforceStdout)"
case "$VERSION" in
  *[!0-9.]*|"") echo "jpackage needs a numeric version (X.Y.Z), the pom says '$VERSION'" >&2; exit 1 ;;
esac

# Module path: the plain (unshaded) application jar and its runtime dependencies, with JavaFX's
# jars for this platform (Maven picks the classifier of the OS it runs on)
MODULES=target/jpackage/modules
rm -rf target/jpackage target/installer
mkdir -p "$MODULES"
"$MVNW" -q -B dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory="$MODULES"
cp "target/original-$NAME.jar" "$MODULES/$NAME.jar"

ARGS=(
  --type "$TYPE"
  --name "$NAME"
  --app-version "$VERSION"
  --vendor "guillesanper"
  --description "Neuroevolution simulator: NEAT and a genetic algorithm learn to play Flappy Bird"
  --copyright "MIT License"
  --module-path "$MODULES"
  --module "$MODULE/$MAIN_CLASS"
  # slf4j-simple is only found through ServiceLoader, so jlink does not pull it in by itself
  --add-modules org.slf4j.simple
  --jlink-options "--strip-debug --no-header-files --no-man-pages --strip-native-commands"
  # Default arguments, used only when the launcher gets none: open on the bundled champion
  --arguments --demo
  --dest target/installer
)

case "$TYPE" in
  deb|rpm)
    ARGS+=(--linux-package-name flappybirdneat --linux-shortcut --linux-menu-group Education
           --linux-app-category education)
    ;;
  msi|exe)
    # A fixed upgrade code lets a newer installer replace an older version
    ARGS+=(--win-menu --win-menu-group "$NAME" --win-shortcut --win-dir-chooser
           --win-upgrade-uuid 6f1e2a3b-4c5d-4e6f-8a9b-0c1d2e3f4a5b)
    ;;
  dmg|pkg)
    ARGS+=(--mac-package-name "$NAME")
    ;;
esac

jpackage "${ARGS[@]}"
ls -l target/installer
