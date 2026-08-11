package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class RoomDataInitializerTest {

    // TODO: 这里注入你真实写好的房源/订单 Service
    // @Autowired
    // private RoomService roomService;

    @Test
    public void testConcurrentBookRoom() throws InterruptedException {
        System.out.println("========== 开始并发抢房测试 ==========");

        int threadCount = 100; // 模拟 100 个人同时抢房
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        // latch 就像运动场上的发令枪，初始值为 1
        CountDownLatch latch = new CountDownLatch(1);
        // endLatch 用来等所有线程跑完，我们再结束测试
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final Long mockUserId = (long) (i + 1); // 模拟不同的用户 ID
            final Long roomId = 101L; // 假设大家都在抢 101 号房间

            executorService.submit(() -> {
                try {
                    // 1. 所有线程跑到这里都会被卡住，等待发令枪响
                    latch.await();

                    // 2. 发令枪响后，100 个线程瞬间同时执行下面的抢房逻辑
                    // roomService.bookRoom(roomId, mockUserId); // 调用你的真实下单逻辑

                    System.out.println("用户 " + mockUserId + " 发起了抢房请求");
                } catch (Exception e) {
                    System.err.println("用户 " + mockUserId + " 抢房失败：" + e.getMessage());
                } finally {
                    // 3. 当前线程执行完毕，endLatch 减 1
                    endLatch.countDown();
                }
            });
        }

        // 倒数 3 秒准备发令
        Thread.sleep(3000);
        System.out.println("3, 2, 1... 嘭！开始抢房！");

        // 发令枪响！latch 变为 0，刚才被卡住的 100 个线程瞬间同时释放
        latch.countDown();

        // 主线程等待所有子线程执行完毕
        endLatch.await();
        System.out.println("========== 并发抢房测试结束 ==========");
    }
}