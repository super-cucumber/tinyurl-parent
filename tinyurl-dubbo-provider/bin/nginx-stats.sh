echo $(grep 'upstream_response_time' /opt/ng_logs/tinyurl_logs/api.access.log | awk -F 'upstream_response_time' '{print $2}' | awk -F ' ' '{print $1}'  | awk '{total=total+$0;count++;} END{print "upstream response time average cost time is", (total/count)*1000, "ms"}')

echo $(grep 'request_time' /opt/ng_logs/tinyurl_logs/api.access.log | awk -F 'request_time' '{print $2}' | awk -F ' ' '{print $1}'  | awk '{total=total+$0;count++;} END{print "request time average cost time is", (total/count)*1000, "ms"}')
