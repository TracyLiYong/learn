--- 库存key
local seckillStockKey = KEYS[1];
--- 秒杀key
local seckillLockKey = KEYS[2];
--- 获取秒杀商品信息
local successKillInfo = ARGV[1];
--- 获取秒杀商品超时时间
local expire = ARGV[2];

--- 获取指定的库存
local resultStock = redis.call("get", seckillStockKey);
--- 查询是否秒杀过
local resultKilled = redis.call("get", seckillLockKey);

--- 是否库存存在
if not resultStock then
    return -4;
end
if tonumber(resultStock) <= 0 then
    return -4;
end

--- 是否秒杀过
if not resultKilled then
    redis.call("set", seckillLockKey, successKillInfo);
    redis.call('decr', seckillStockKey);
    --- 由于lua脚本接收到参数都会转为String，所以要转成数字类型才能比较
    if tonumber(expire) > 0 then
        --- 设置过期时间
        redis.call("expire", seckillLockKey, expire);
    end
    return 1;
end
return -1;
