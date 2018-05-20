#!/bin/bash

SLUG="TheMrMilchmann/MJLEvents"
JDK="oraclejdk8"
BRANCH="master"

set -e

if [ "$TRAVIS_REPO_SLUG" == "$SLUG" ] && [ "$TRAVIS_JDK_VERSION" == "$JDK" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "$BRANCH" ]; then
    # Upload snapshot artifacts to OSSRH.

    echo -e "[deploy.sh] Publishing snapshots...\n"

    ./gradlew uploadArchives --parallel -Psnapshot

    echo -e "[deploy.sh] Published snapshots to OSSRH.\n"
fi