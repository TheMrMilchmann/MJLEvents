#!/bin/bash

SLUG="TheMrMilchmann/MJLEvents"
JDK="oraclejdk8"
JDOC_JDK="openjdk12"
BRANCH="master"

set -e
./gradlew check --info -S --parallel -Psnapshot

if [ "$TRAVIS_REPO_SLUG" == "$SLUG" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    if [ "$TRAVIS_JDK_VERSION" == "$JDK" ] && [ "$TRAVIS_BRANCH" == "$BRANCH" ]; then
        # Upload snapshot artifacts to OSSRH.
        echo -e "[deploy.sh] Publishing snapshots...\n"
        ./gradlew publish --parallel -Psnapshot
        echo -e "[deploy.sh] Published snapshots to OSSRH.\n"
    fi

    if [ "$TRAVIS_JDK_VERSION" == "$JDOC_JDK" ]; then
        if [ "$TRAVIS_TAG" == '^v(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(\.(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*)?(\+[0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*)?$' ]; then
            # Upload JavaDoc to GH-Pages.
            echo -e "[deploy.sh] Publishing documentation...\n"
            mkdir out

            ./gradlew javadoc --parallel -Prelease
            cd out
            git init
            git config user.name "Deployment Bot"
            git config user.email "deploy@travis-ci.org"
            git pull -q "https://github.com/$SLUG.git" gh-pages
            mkdir -p ./docs/${TRAVIS_TAG}
            cp -r ../build/docs/javadoc/* ./docs/${TRAVIS_TAG}
            sed -i -e "s/<!--INSERT_RELEASE-->/<!--INSERT_RELEASE-->\n            <li><a href=\"./docs/${TRAVIS_TAG}/index.html\">${TRAVIS_TAG}<\/a><br><\/li>/g" index.html
            COMMIT_MSG="feat(ci): $TRAVIS_TAG release documentation"

            git add .
            git commit -m "$COMMIT_MSG"
            git push -q "https://${GH_TOKEN}@github.com/$SLUG" master:gh-pages
            echo -e "[deploy.sh] Published documentation.\n"
        elif [ "$TRAVIS_BRANCH" == "$BRANCH" ]; then
            # Upload JavaDoc to GH-Pages.
            echo -e "[deploy.sh] Publishing documentation...\n"
            mkdir out

            ./gradlew javadoc --parallel -Psnapshot
            cd out
            git init
            git config user.name "Deployment Bot"
            git config user.email "deploy@travis-ci.org"
            git pull -q "https://github.com/$SLUG.git" gh-pages
            rm -rf ./docs/snapshot/
            mkdir -p ./docs/snapshot/
            cp -r ../build/docs/javadoc/* ./docs/snapshot/
            COMMIT_MSG="feat(ci): snapshot documentation for build $TRAVIS_BUILD_NUMBER ($TRAVIS_COMMIT)"

            git add .
            git commit -m "$COMMIT_MSG"
            git push -q "https://${GH_TOKEN}@github.com/$SLUG" master:gh-pages
            echo -e "[deploy.sh] Published documentation.\n"
        fi
    fi
fi