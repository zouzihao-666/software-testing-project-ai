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
class AiReservation006Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-006 超大时长导致总价溢出")
    void shouldRejectDurationThatOverflowsTotalPrice() throws Exception {
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
                            "startTime", day + " 10:00", "hours", "2147483647"));
            List<Order> created = newOrders(before);
            boolean invalidSaved = created.stream().anyMatch(order ->
                    order.getHours() == Integer.MAX_VALUE || order.getTotal() <= 0);
            boolean passed = created.isEmpty() && !invalidSaved;
            String saved = created.isEmpty() ? "无" :
                    "hours=" + created.get(0).getHours() + "，total=" + created.get(0).getTotal();
            showEvidence(driver, "RES-AI-006 超大时长与金额溢出", passed,
                    "输入：hours=2147483647",
                    "HTTP状态：" + response.status,
                    "数据库结果：" + saved,
                    passed ? "通过：超大数值被拒绝" : "未通过：系统保存了溢出金额订单");
            assertTrue(created.isEmpty(), "超大时长不能生成订单；异常订单保留供检查");
        } finally {
            driver.quit();
        }
    }
}
