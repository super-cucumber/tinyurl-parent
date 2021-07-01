echo $(grep 'generate, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, sync log, generate id average cost time is", total/count/1000000, "ms"}')

echo $(grep 'prepare the variables when creating, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, sync log, prepare the variables when creating average cost time is", total/count/1000000, "ms"}')

echo $(grep 'models, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, sync log, serialize log average cost time is", total/count/1000000, "ms"}')

echo $(grep 'batch flush prepare log, flush log number' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, sync log, pure flush log exclude lock average cost time is", total/count/1000000, "ms"}')

echo $(grep 'models to log file, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, sync log, flush log include lock average cost time is", total/count/1000000, "ms"}')

echo $(grep 'sync log, commit P log, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, sync log, commit P log average cost time is", total/count/1000000, "ms"}')

echo $(grep 'sync log, add tiny url to cache, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, sync log, set values to redis average cost time is", total/count/1000000, "ms"}')

echo $(grep 'sync log, commit C log, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db, sync log, commit C log average cost time is", total/count/1000000, "ms"}')

echo $(grep 'async db commit, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "async db commit average cost time is", total/count/1000000, "ms"}')

echo $(grep 'add raw url to redis, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "add raw url to redis cost time is", total/count/1000000, "ms"}')

echo $(grep 'no alias code, pre create validation, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "pre create validation average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep 'no alias code, do create, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "do create average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep 'create url, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "create tiny url average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep 'batch flush prepare log, flush log number' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F 'batch flush prepare log, flush log number' '{print $2}'  | awk -F '[,]' '{print $1}' |  awk '{total=total+$0;count++;} END{print "total logs has been flushed is", total, ", count is", count}')

echo $(grep 'have been flushed to db, maxXid' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F 'records' '{print $1}'  | awk -F ' ' '{print $8}' |  awk '{total=total+$0;count++;} END{print "total records has been inserted to db is", total, ", count is", count}')

echo $(grep 'log commit tasks run end,' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | grep 'records commit' |  awk -F 'records' '{print $1}' | awk -F ' ' '{print $NF}' |  awk '{total=total+$0;count++;} END{print "total C logs have been committed is", total, ", count is", count}')

echo $(grep '[[]PSYoungGen:' /opt/tinyurl/logs/tinyurl-provider-gc.log | awk -F '[,]' '{print $2}' | awk -F '[]]' '{print $1}' | awk -F ' ' '{print $1}' | awk '{total=total+$0;count++;} END{print "minor gc total cost time is", total, "s,", "count is", count}')

echo $(grep '[[]Full GC' /opt/tinyurl/logs/tinyurl-provider-gc.log | awk -F '[,]' '{print $3}' | awk -F '[]]' '{print $1}' | awk -F ' ' '{print $1}' | awk 'BEGIN{total=0;count=0;}{total=0;total=total+$0;count++;} END{print "full gc total cost time is", total, "s,", "count is", count}')
