#!/usr/bin/env bash

set -ev

IMAGE=orders
REPO=${GROUP}/${IMAGE}
SCRIPT_DIR=$(dirname "$0")
CODE_DIR=$(cd $SCRIPT_DIR/..; pwd)

echo $CODE_DIR

if [[ -z "$GROUP" ]] ; then
    echo "Cannot find GROUP env var"
    exit 1
fi

if [[ -z "$COMMIT" ]] ; then
    echo "Cannot find COMMIT env var"
    exit 1
fi

if [[ "$(uname)" == "Darwin" ]]; then
    DOCKER_CMD=docker
else
    DOCKER_CMD="sudo docker"
fi

$DOCKER_CMD run --rm -v $HOME/.m2:/root/.m2 -v $CODE_DIR:/usr/src/mymaven -w /usr/src/mymaven maven:3.2-jdk-8 mvn -DskipTests package

cp -rf $CODE_DIR/docker $CODE_DIR/target/docker/
cp -rf $CODE_DIR/target/*.jar $CODE_DIR/target/docker/${IMAGE}

$DOCKER_CMD build -t ${REPO}:${COMMIT} $CODE_DIR/target/docker/${IMAGE};
