-- ============================================================
-- 防超卖并发回归 · 数据一致性校验脚本
-- 适用场景：对同一房源同一入住/退租日期段做 JMeter 高并发下单压测后执行
-- 期望结果：
--   ① 重叠订单查询 → 0 行
--   ② 该场景成功落库订单数 → 1 单（并发 N 中只有 1 人能抢到该时段）
--   ③ Redis 无残留锁（redis-cli 执行：KEYS "lock:order:room:*"）
-- 使用前请把下面三个变量替换为本次压测的真实参数
-- ============================================================

-- 场景参数（压测时使用的房源与日期段）
SET @roomId    = 101;                      -- 压测房源 ID
SET @checkIn   = '2026-10-01';             -- 压测入住日期
SET @checkOut  = '2026-10-15';             -- 压测退租日期

-- ① 检查：是否存在“同一房源、时间段互相重叠”的有效订单（期望：空结果集）
SELECT a.id AS order_a, b.id AS order_b,
       a.user_id AS user_a, b.user_id AS user_b,
       a.check_in_date, a.check_out_date
FROM room_order a
JOIN room_order b
  ON a.room_id = b.room_id
 AND a.id < b.id
 AND a.is_deleted = 0 AND b.is_deleted = 0
 AND a.status IN (0,1,2,3) AND b.status IN (0,1,2,3)
 AND a.check_in_date < b.check_out_date
 AND a.check_out_date > b.check_in_date
WHERE a.room_id = @roomId;
-- 期望输出：Empty set（0 行）＝ 未出现重叠/超卖订单

-- ② 检查：并发压测场景下实际成功落库的订单数（期望：1）
SELECT COUNT(*) AS success_orders
FROM room_order
WHERE room_id = @roomId
  AND is_deleted = 0
  AND status IN (0,1,2,3)
  AND check_in_date = @checkIn
  AND check_out_date = @checkOut;
-- 期望输出：1
-- 若 >1 说明仍存在并发漏洞；若 =0 说明压测参数/请求体可能有误，需复查 JMeter 配置

-- ③ 检查（在命令行执行，不在本脚本内）：Redis 中是否残留锁
-- redis-cli
-- KEYS "lock:order:room:*"
-- 期望输出：(empty array)  ← 压测结束后锁应全部被释放，无死锁残留
-- 若仍有残留，可用：DEL lock:order:room:<roomId> 手动清理后复查原因
