-- KEYS[1] = challenge key, ARGV[1] = 요청 코드의 HMAC(호출자가 미리 계산해서 넘긴다)
-- 반환값: 성공 시 challenge의 원본 JSON 문자열(삭제 전 상태 — 호출자가 결속된 userId 등 부가
--        정보를 읽을 수 있도록), 실패(불일치/만료/이미 사용됨/5회 초과)는 빈 문자열("")
-- 코드 확인과 실패 횟수 증가를 하나의 스크립트로 묶어 원자적으로 처리한다(SIGNUP-2 정책,
-- RECOVERY-1/2에서도 동일 스크립트를 재사용).
local raw = redis.call('GET', KEYS[1])
if raw == false then
  return ''
end

local ok, decoded = pcall(cjson.decode, raw)
if not ok then
  redis.call('DEL', KEYS[1])
  return ''
end

if decoded.codeHmac == ARGV[1] then
  redis.call('DEL', KEYS[1])
  return raw
end

local attempts = (decoded.attempts or 0) + 1
if attempts >= 5 then
  redis.call('DEL', KEYS[1])
  return ''
end

decoded.attempts = attempts
redis.call('SET', KEYS[1], cjson.encode(decoded), 'KEEPTTL')
return ''
