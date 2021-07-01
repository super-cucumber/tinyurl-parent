local expectedvalue=ARGV[1]
local actualvalue=redis.call("get", KEYS[1])
if expectedvalue == actualvalue then
    redis.call("del",KEYS[1])
    return "OK"
end

return "null"




