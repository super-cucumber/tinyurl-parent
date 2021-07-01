#!/bin/bash

nohup java -jar -Xmx2048m -Xms2048m -Xmn1024m -XX:SurvivorRatio=3 -XX:MetaspaceSize=256m -XX:-UseBiasedLocking -XX:+UnlockDiagnosticVMOptions  -XX:+LogVMOutput -XX:LogFile=/opt/tinyurl/logs/vm.log  -XX:+PrintHeapAtGC -XX:+PrintTenuringDistribution  -XX:+PrintGCDetails -XX:+PrintGCDateStamps  -XX:+PrintGCApplicationStoppedTime  -Xloggc:/opt/tinyurl/logs/tinyurl-provider-gc.log  -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/opt/tinyurl/logs/tinyurl-provider-dump.log  -javaagent:/opt/skywalking/provideragent/skywalking-agent.jar -Djava.net.preferIPv4Stack=true -Dcsp.sentinel.api.port=8085 -Dproject.name=tinyurl-dubbo-provider  -Dcsp.sentinel.dashboard.server=127.0.0.1:4899  tinyurl-dubbo-provider.jar --spring.profiles.active=prod   > /opt/tinyurl/logs/tinyurl-provider.log 2>&1 &

echo "start successfully!"
