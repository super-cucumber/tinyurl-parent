local bitmapkey=KEYS[1]
local tinyurlkey=KEYS[2]
local txnlogkey=KEYS[3]
local txnlogendkey=KEYS[4]
local tinyurlupdatetimekey=KEYS[5]

local bitmapindex=ARGV[1]
local messagecreatetime=tonumber(ARGV[2])

local updatetime=redis.call("get",tinyurlupdatetimekey)

local rollback=true

if updatetime then
    if updatetime > messagecreatetime then
        rollback=false
    end
end

if rollback then
  redis.call("setbit",bitmapkey,bitmapindex,0)
  redis.call("del",tinyurlkey)
  redis.call("del",txnlogkey)
  redis.call("del",txnlogendkey)
  redis.call("del",tinyurlupdatetimekey)
end
return "OK"




