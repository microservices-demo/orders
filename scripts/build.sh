#!/usr/bin/env bash

IMAGE=orders

set -ev

SCRIPT_DIR=$(dirname "$0")

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
CODE_DIR=$(cd $SCRIPT_DIR/..; pwd)
echo $CODE_DIR
$DOCKER_CMD run --rm -v $HOME/.m2:/root/.m2 -v $CODE_DIR:/usr/src/mymaven -w /usr/src/mymaven maven:3.2-jdk-8 mvn -DskipTests package

cp -r $CODE_DIR/docker $CODE_DIR/target/docker/
cp -r $CODE_DIR/target/*.jar $CODE_DIR/target/docker/${IMAGE}

REPO=${GROUP}/${IMAGE}
    $DOCKER_CMD build -t ${REPO}:${COMMIT} $CODE_DIR/target/docker/${IMAGE};
