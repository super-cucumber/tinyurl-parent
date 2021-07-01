#! /bin/bash

echo $(grep 'batch flush prepare log, flush log' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "exclude lock pure flush P logs average cost time is", total/count/1000000, "ms"}')

echo $(grep 'models to log file, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print " include lock flush P logs average cost time is", total/count/1000000, "ms"}')

echo $(grep 'models, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "serialize P logs average cost time is", total/count/1000000, "ms"}')

echo $(grep 'node queue polled, list size' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F  ' ' '{print $2}' | awk -F 'ns' '{print $1}' | awk '{total=total+$0;count++;} END{print "queue poll average await time is", total/count/1000000, "ms"}')

echo $(grep 'unpark threads, node size' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F  ' ' '{print $2}' | awk -F 'ns' '{print $1}' | awk '{total=total+$0;count++;} END{print "threads unpark average await time is", total/count/1000000, "ms"}')

echo $(grep 'park end, from park to unpark,' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F  ' ' '{print $2}' | awk -F 'ns' '{print $1}' | awk '{total=total+$0;count++;} END{print "timed barrier average await time is", total/count/1000000, "ms"}')

echo $(grep 'await commit prepare log, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "commit P log average cost time is", total/count/1000000, "ms"}')

echo $(grep 'add tiny url to cache, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "add to redis average cost time is", total/count/1000000, "ms"}')

echo $(grep 'commit log, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "commit C log average cost time is", total/count/1000000, "ms"}')

echo $(grep 'no alias code, do create, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "do create average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep 'o alias code, pre create validation, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "pre create validation average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep 'create url, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "create tiny url average cost time is", total/count/1000000, "ms,", "count is", count}')

