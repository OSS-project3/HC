-- KEYS[1] = challenge key, ARGV[1] = 요청 코드의 HMAC(호출자가 미리 계산해서 넘긴다)
-- 반환값: 1=성공(challenge 삭제됨), 0=불일치(재시도 가능, attempts 증가),
--        -1=challenge 없음(만료/이미 사용/손상), -2=5회 초과로 폐기(이번 호출에서 삭제됨)
-- 코드 확인과 실패 횟수 증가를 하나의 스크립트로 묶어 원자적으로 처리한다(SIGNUP-2 정책).
local raw = redis.call('GET', KEYS[1])
if raw == false then
  return -1
end

local ok, decoded = pcall(cjson.decode, raw)
if not ok then
  redis.call('DEL', KEYS[1])
  return -1
end

if decoded.codeHmac == ARGV[1] then
  redis.call('DEL', KEYS[1])
  return 1
end

local attempts = (decoded.attempts or 0) + 1
if attempts >= 5 then
  redis.call('DEL', KEYS[1])
  return -2
end

decoded.attempts = attempts
redis.call('SET', KEYS[1], cjson.encode(decoded), 'KEEPTTL')
return 0
