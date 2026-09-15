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

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
class AiReservation012Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-012 退出登录后复用原会话提交预约")
    void shouldInvalidateUserSessionAfterLogout() throws Exception {
        User user = ensureUser("user01");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 14, 1);
        Set<Integer> before = orderIds();

        WebDriver driver = openChrome();
        try {
            login(driver, user);
            driver.get(BASE_URL + "/logout.do");
            HttpResult response = post(driver, "/addOrder.do",
                    params("venueName", venue.getVenueName(), "date", day.toString(),
                            "startTime", day + " 14:00", "hours", "1"));
            List<Order> created = newOrders(before);
            boolean passed = created.isEmpty();
            showEvidence(driver, "RES-AI-012 注销后的会话", passed,
                    "用户已执行退出登录",
                    "继续使用同一浏览器会话提交预约",
                    "HTTP状态：" + response.status,
                    "新增订单数：" + created.size(),
                    passed ? "通过：退出后的会话不能再预约" : "未通过：退出后的会话仍然有效");
            assertTrue(created.isEmpty(), "退出登录后原会话不能继续创建订单");
        } finally {
            driver.quit();
        }
    }
}
