-- KEYS[1] = cooldown key, KEYS[2] = target(email/phone) 시간당 카운터 키, KEYS[3] = IP 시간당 카운터 키
-- ARGV[1] = cooldown TTL(초), ARGV[2] = target 한도, ARGV[3] = target 윈도(초),
-- ARGV[4] = IP 한도, ARGV[5] = IP 윈도(초)
-- 반환값: 1=허용(쿨다운 선점 + 카운터 증가까지 완료), 0=차단(쿨다운 중이거나 한도 초과 — 아무 것도
--        증가·설정하지 않고 그대로 반환)
-- 계정 존재 여부와 무관하게 매 요청이 이 게이트를 먼저 통과해야 한다(RECOVERY-1/2 정책) — 동시
-- 요청이 이 원자 스크립트 밖에서 한도를 우회하거나 쿨다운을 중복 선점하지 못하게 한다.
if redis.call('EXISTS', KEYS[1]) == 1 then
  return 0
end

local targetCount = redis.call('INCR', KEYS[2])
if targetCount == 1 then
  redis.call('EXPIRE', KEYS[2], ARGV[3])
end
if targetCount > tonumber(ARGV[2]) then
  return 0
end

local ipCount = redis.call('INCR', KEYS[3])
if ipCount == 1 then
  redis.call('EXPIRE', KEYS[3], ARGV[5])
end
if ipCount > tonumber(ARGV[4]) then
  return 0
end

redis.call('SET', KEYS[1], '1', 'EX', ARGV[1])
return 1
