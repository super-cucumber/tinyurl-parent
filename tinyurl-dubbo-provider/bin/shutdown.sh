#! /bin/bash

APP_NAME=tinyurl-dubbo-provider

PID=$(ps -ef | grep ${APP_NAME}.jar | grep -v grep | awk '{ print $2 }')
if [ -z "$PID" ]
then
    echo "${APP_NAME} has stopped!"
else
    echo "shutdown ${APP_NAME} $PID"
    kill $PID
fi

