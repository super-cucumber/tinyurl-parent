echo $(grep 'generate, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "sync db, generate id average cost time is", total/count/1000000, "ms"}')

echo $(grep 'prepare the variables when creating, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "sync db, prepare the variables when creating average cost time is", total/count/1000000, "ms"}')

echo $(grep 'dao add, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "sync db, dao add average cost time is", total/count/1000000, "ms"}')

echo $(grep ', set bit, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $4}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "sync db, set bit average cost time is", total/count/1000000, "ms"}')

echo $(grep 'sync db commit, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "sync db commit average cost time is", total/count/1000000, "ms"}')

echo $(grep 'add raw url to redis, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $2}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}'  | awk '{total=total+$0;count++;} END{print "sync db, add raw url to redis cost time is", total/count/1000000, "ms"}')

echo $(grep 'no alias code, pre create validation, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "pre create validation average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep 'no alias code, do create, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "do create average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep 'create url, cost' /opt/tinyurl/logs/tinyurl-dubbo-provider.log | awk -F '[,]' '{print $3}' | awk -F ' ' '{print $2}' | awk -F 'ns' '{print $1}' |  awk '{total=total+$0;count++;} END{print "sync db, create tiny url average cost time is", total/count/1000000, "ms,", "count is", count}')

echo $(grep '[[]PSYoungGen:' /opt/tinyurl/logs/tinyurl-provider-gc.log | awk -F '[,]' '{print $2}' | awk -F '[]]' '{print $1}' | awk -F ' ' '{print $1}' | awk '{total=total+$0;count++;} END{print "minor gc total cost time is", total, "s,", "count is", count}')

echo $(grep '[[]Full GC' /opt/tinyurl/logs/tinyurl-provider-gc.log | awk -F '[,]' '{print $3}' | awk -F '[]]' '{print $1}' | awk -F ' ' '{print $1}' | awk 'BEGIN{total=0;count=0;}{total=0;total=total+$0;count++;} END{print "full gc total cost time is", total, "s,", "count is", count}')
