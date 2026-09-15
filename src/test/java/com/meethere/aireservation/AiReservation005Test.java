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
class AiReservation005Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-005 预约时长传入非数字")
    void shouldRejectNonNumericHoursWithoutCreatingOrder() throws Exception {
        User user = ensureUser("user01");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 10, 1);
        Set<Integer> before = orderIds();

        WebDriver driver = openChrome();
        try {
            login(driver, user);
            driver.get(BASE_URL + "/order_place.do?venueID=" + venue.getVenueID());
            HttpResult response = post(driver, "/addOrder.do",
                    params("venueName", venue.getVenueName(), "date", day.toString(),
                            "startTime", day + " 10:00", "hours", "abc"));
            List<Order> created = newOrders(before);
            boolean passed = created.isEmpty() && response.status >= 400;
            showEvidence(driver, "RES-AI-005 非数字时长", passed,
                    "输入：hours=abc",
                    "HTTP状态：" + response.status,
                    "新增订单数：" + created.size(),
                    passed ? "通过：参数转换失败且未生成订单" : "未通过：异常参数未被正确拦截");
            assertTrue(created.isEmpty(), "hours=abc时不能生成订单");
            assertTrue(response.status >= 400 && response.status < 500,
                    "无效参数应返回4xx响应，而不是成功或服务器崩溃");
        } finally {
            driver.quit();
        }
    }
}
