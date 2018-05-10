#!/bin/bash

CP=out/:jars/lwjgl.jar:jars/lwjgl_util.jar
MAIN="Game"

java -Djava.library.path=natives/ -cp $CP $MAIN
