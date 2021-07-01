echo $(grep 'generate, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "generate id average cost time is", total/count/1000000, "ms"}')

echo $(grep 'prepare the variables when creating, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "prepare the variables when creating average cost time is", total/count/1000000, "ms"}')

echo $(grep 'models, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, delay log, serialize log average cost time is", total/count/1000000, "ms"}')

echo $(grep 'batch flush prepare log, flush log number' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, delay log, pure flush log exclude lock average cost time is", total/count/1000000, "ms"}')

echo $(grep 'models to log file, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, delay log, flush log include lock average cost time is", total/count/1000000, "ms"}')

echo $(grep 'await finish, arrive index' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' | awk '{total=total+$0;count++;} END{print "cyclic barrier average await time is", total/count/1000000, "ms"}')

echo $(grep 'await commit prepare log, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, delay log, commit P log average cost time is", total/count/1000000, "ms"}')

echo $(grep 'delay log, add tiny url to cache, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, delay log, set values to redis average cost time is", total/count/1000000, "ms"}')

echo $(grep 'delay log, commit C log, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, delay log, commit C log average cost time is", total/count/1000000, "ms"}')

echo $(grep 'async db commit, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db commit average cost time is", total/count/1000000, "ms"}')

echo $(grep 'add raw url to redis, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "add raw url to redis cost time is", total/count/1000000, "ms"}')

echo $(grep 'no alias code, pre create validation, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "pre create validation average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep 'no alias code, do create, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "do create average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep 'create url, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "create tiny url average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep '[[]PSYoungGen:' /opt/tinyurl/logs/tinyurl-provider-gc.log | awk -F '[,]' '{print $2}' | awk -F '[]]' '{print $1}' | awk -F ' ' '{print $1}' | awk '{total=total+$0;count++;} END{print "minor gc total cost time is", total, "s,", "count is", count}')

echo $(grep '[[]Full GC' /opt/tinyurl/logs/tinyurl-provider-gc.log | awk -F '[,]' '{print $3}' | awk -F '[]]' '{print $1}' | awk -F ' ' '{print $1}' | awk 'BEGIN{total=0;count=0;}{total=0;total=total+$0;count++;} END{print "full gc total cost time is", total, "s,", "count is", count}')
