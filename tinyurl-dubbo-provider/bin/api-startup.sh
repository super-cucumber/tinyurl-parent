#!/bin/bash

nohup java -jar -XX:+PrintHeapAtGC -XX:+PrintTenuringDistribution  -XX:+PrintGCDetails -Xloggc:/opt/tinyurl/tinyurl-api-gc.log  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/tinyurl/tinyurl-api-dump.log -javaagent:/opt/skywalking/apiagent/skywalking-agent.jar  tinyurl-api.jar --spring.profiles.active=prod  > /opt/tinyurl/tinyurl-api.log 2>&1 &

echo "start successfully!"
