local bitmapkey=KEYS[1]
local tinyurlkey=KEYS[2]
local txnlogkey=KEYS[3]
local txnlogendkey=KEYS[4]
local tinyurlupdatetimekey=KEYS[5]

local bitmapindex=ARGV[1]
local rawurl=ARGV[2]
local xid=ARGV[3]
local rawurlexpired=tonumber(ARGV[4])
local xidexpired=tonumber(ARGV[5])
local updatetime=tonumber(ARGV[6])

redis.call("set",txnlogkey,xid,"ex",xidexpired)
redis.call("setbit",bitmapkey,bitmapindex,1)
redis.call("set",tinyurlkey,rawurl,"ex",rawurlexpired)
redis.call("set",tinyurlupdatetimekey,updatetime,"ex",xidexpired)
redis.call("set",txnlogendkey,xid,"ex",xidexpired)


return "OK"




