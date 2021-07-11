local bitmapkey=KEYS[1]
local tinyurlkey=KEYS[2]
local txnlogkey=KEYS[3]
local txnlogendkey=KEYS[4]
local tinyurlrandomvaluekey=KEYS[5]
local rollbackkey=KEYS[6]

local bitmapindex=ARGV[1]
local originalvalue=ARGV[2]
local aliascode=ARGV[3]
local rollbackkeyexpired=tonumber(ARGV[4])

local randomvalue=redis.call("get",tinyurlrandomvaluekey)

local rollback=true

if randomvalue then
    if randomvalue ~= originalvalue then
        rollback=false
    end
end

if rollback then
  redis.call("setbit",bitmapkey,bitmapindex,0)
  redis.call("del",tinyurlkey)
  redis.call("del",txnlogkey)
  redis.call("del",txnlogendkey)
  redis.call("del",tinyurlrandomvaluekey)
  redis.call("set",rollbackkey,aliascode,"ex",rollbackkeyexpired)
end
return "OK"




