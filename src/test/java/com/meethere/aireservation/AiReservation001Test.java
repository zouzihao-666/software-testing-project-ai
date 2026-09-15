package com.meethere.aireservation;

import com.meethere.entity.Order;
import com.meethere.entity.User;
import com.meethere.entity.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
class AiReservation001Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-001 两名用户同时预约同一时段")
    void shouldAllowOnlyOneConcurrentReservation() throws Exception {
        User user01 = ensureUser("user01");
        User user02 = ensureUser("user02");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 10, 1);
        Set<Integer> before = orderIds();

        WebDriver first = openChrome();
        WebDriver second = openChrome();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            login(first, user01);
            login(second, user02);
            first.get(BASE_URL + "/order_place.do?venueID=" + venue.getVenueID());
            second.get(BASE_URL + "/order_place.do?venueID=" + venue.getVenueID());

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);
            Future<HttpResult> request1 = executor.submit(() -> {
                ready.countDown(); start.await();
                return post(first, "/addOrder.do", orderParams(venue, day));
            });
            Future<HttpResult> request2 = executor.submit(() -> {
                ready.countDown(); start.await();
                return post(second, "/addOrder.do", orderParams(venue, day));
            });
            ready.await();
            start.countDown();
            HttpResult result1 = request1.get();
            HttpResult result2 = request2.get();

            List<Order> created = newOrders(before);
            boolean passed = created.size() == 1;
            showEvidence(first, "RES-AI-001 并发预约", passed,
                    "请求1 HTTP状态：" + result1.status,
                    "请求2 HTTP状态：" + result2.status,
                    "同一时段新增订单数：" + created.size(),
                    passed ? "通过：系统只保存一笔订单" : "未通过：系统产生了重复占用订单");
            assertEquals(1, created.size(), "两个并发请求只能有一个成功；异常订单保留供检查");
        } finally {
            executor.shutdownNow();
            first.quit();
            second.quit();
        }
    }

    private java.util.Map<String, String> orderParams(Venue venue, LocalDate day) {
        return params("venueName", venue.getVenueName(), "date", day.toString(),
                "startTime", day + " 10:00", "hours", "1");
    }
}
