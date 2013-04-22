#!/bin/bash
if [ -z "$1" ]; then
  echo "Missing app parameter"
  exit 1
fi

DIR=$(mktemp -d /tmp/wsXXXXXXXX) || { echo "Failed to create temp directory"; exit 1; }
trap "rm -Rf $DIR" EXIT
echo "Working in ${DIR}"

cp -R play2 $DIR
cp dotcloud.yml $DIR

mkdir $DIR/application
cp -R ../../target $DIR/application/
cp -R ../../public $DIR/application/

# connect app and deploy
cd $DIR
dotcloud connect $1 || { echo "Failed to connect to dotcloud app $1"; exit 1; }
dotcloud push || { echo "Failed to push to dotcloud"; exit 1; }

