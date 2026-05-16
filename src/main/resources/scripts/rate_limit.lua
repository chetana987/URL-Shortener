local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local member = ARGV[4]

redis.call('ZREMRANGEBYSCORE', key, 0, now - window)
local count = redis.call('ZCARD', key)

if count >= limit then
    local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
    local retryAfter = window - (now - tonumber(oldest[2]))
    if retryAfter < 1 then retryAfter = 1 end
    return {0, math.ceil(retryAfter / 1000)}
end

redis.call('ZADD', key, now, member)
redis.call('EXPIRE', key, math.ceil(window / 500))
return {1, 0}
