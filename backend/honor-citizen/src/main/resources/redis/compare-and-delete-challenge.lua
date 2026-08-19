-- KEYS[1] = challenge key, ARGV[1] = expected challengeId
-- 메일 발송 실패 시 "지금 저장된 challenge가 이 요청이 방금 저장한 것과 같을 때만" 지운다.
-- 그 사이 다른(재전송) 요청이 새 코드를 이미 저장했다면 그건 건드리지 않는다.
local raw = redis.call('GET', KEYS[1])
if raw == false then
  return 0
end
local ok, decoded = pcall(cjson.decode, raw)
if not ok or decoded.challengeId ~= ARGV[1] then
  return 0
end
redis.call('DEL', KEYS[1])
return 1
