package com.meethere.aireservation;

import com.meethere.entity.Order;
import com.meethere.entity.User;
import com.meethere.entity.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
class AiReservation007Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-007 未登录直接调用删除接口")
    void shouldRequireLoginBeforeDeletingOrder() throws Exception {
        User owner = ensureUser("user01");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 12, 1);
        Order original = saveOrder(owner, venue, day.atTime(12, 0), 1, 1);

        WebDriver driver = openChrome();
        try {
            HttpResult response = post(driver, "/delOrder.do",
                    params("orderID", String.valueOf(original.getOrderID())));
            Order after = orderDao.findByOrderID(original.getOrderID());
            boolean passed = after != null;
            showEvidence(driver, "RES-AI-007 未登录删除订单", passed,
                    "未携带登录会话",
                    "目标订单：" + original.getOrderID(),
                    "HTTP状态：" + response.status,
                    passed ? "通过：请求被拒绝，订单仍存在" : "未通过：未登录也能删除订单");
            assertNotNull(after, "删除接口必须校验登录状态；被删除订单可由测试记录确认");
        } finally {
            driver.quit();
        }
    }
}
