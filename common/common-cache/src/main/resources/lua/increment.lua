--字符串分隔
local function split(str , reps)
    local result = {}
    string.gsub(str , '[^' .. reps .. ']+' , function(w)
        table.insert(result , w)
        end)
    return result
end

--计算借出的秒数账单
local function getBorrowSecond(newTimestamp ,borrow ,incr ,finIncr, borrowKey)
    local borrowSecond = 0
    local oldTimestamp = -1
    local oldBorrow = 0
    if redis.call('EXISTS' , borrowKey) == 1 then
        --格式：timestamp_borrow
        local oldLedger = redis.call('GET' , borrowKey)
        local oldLedgerTable =  split(oldLedger , '_')
        oldTimestamp = tonumber(oldLedgerTable[1])
        oldBorrow = tonumber(oldLedgerTable[2])
--        redis.log(redis.LOG_WARNING,'账单 timestamp:' .. oldTimestamp .. ' borrow:' .. oldBorrow)
    end

    if borrow > 0 then
        --没有账单记录新账单 , 或者账单过期
        if oldTimestamp < 0 or (newTimestamp - oldTimestamp > oldBorrow) then
            oldTimestamp = newTimestamp
            oldBorrow = borrow
        --borrow>0时，正常borrow会>=oldBorrow , 小于是跳到了下一分钟的自增键
        elseif borrow < oldBorrow then
            if finIncr == 0 then
                oldBorrow = oldBorrow + 1
            end
        else
            oldBorrow = borrow
        end
    elseif oldTimestamp > 0 then
        local interval = newTimestamp - oldTimestamp
        --账单已过期
        if interval > oldBorrow then
            oldTimestamp = -1
            oldBorrow = 0
            redis.call('DEL' , borrowKey)
        --这种情况是自增ID跳到下一分钟后，防止冲突，多加1S
        elseif incr <=1 then
            oldBorrow = oldBorrow + 1
        end
    end


    borrowSecond = oldTimestamp > 0 and (oldBorrow - (newTimestamp - oldTimestamp)) or 0
    --有借秒数账单，更新值
    if oldTimestamp > 0 then
        redis.call('SET' ,borrowKey , tostring(oldTimestamp) .. '_' .. tostring(oldBorrow))
    end


    return borrowSecond
end

-- 主函数
local function get_uniquid_seq()
    -- countBits
    local lsheft = tonumber(ARGV[3])
    -- 时间戳
    local timestamp = tonumber(ARGV[1])
    --获取自增值
    local incr = redis.call('INCRBY' , KEYS[1] , 1)
    --设置过期时间
    redis.call('expire' , KEYS[1] , ARGV[2])
    local borrow = math.floor(incr / (2 ^ lsheft))
    -- 自增值
    local finIncr = incr % (2 ^ lsheft)
    --计算出借出的秒数
    local b = getBorrowSecond(timestamp , borrow , incr , finIncr , KEYS[2])

    return finIncr .. '_' .. b
end


return get_uniquid_seq()