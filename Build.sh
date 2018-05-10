#!/bin/bash

set -eux

mkdir -p out
CP=jars/lwjgl.jar:jars/lwjgl_util.jar
SOURCE="src/*.java"

javac -d out -cp $CP $SOURCE
