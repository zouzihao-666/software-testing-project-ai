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
class AiReservation004Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-004 非整点时间预约")
    void shouldRejectUnsupportedHalfHourStart() throws Exception {
        User user = ensureUser("user01");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 8, 2);
        Set<Integer> before = orderIds();

        WebDriver driver = openChrome();
        try {
            login(driver, user);
            driver.get(BASE_URL + "/order_place.do?venueID=" + venue.getVenueID());
            HttpResult response = post(driver, "/addOrder.do",
                    params("venueName", venue.getVenueName(), "date", day.toString(),
                            "startTime", day + " 08:30", "hours", "1"));
            List<Order> created = newOrders(before);
            boolean passed = created.isEmpty();
            showEvidence(driver, "RES-AI-004 非整点时间", passed,
                    "输入时间：" + day + " 08:30",
                    "HTTP状态：" + response.status,
                    "新增订单数：" + created.size(),
                    passed ? "通过：非整点时间被拒绝" : "未通过：页面没有该选项，但接口仍接受08:30");
            assertTrue(created.isEmpty(), "系统仅提供整点时段，接口不能静默接受08:30；异常订单保留供检查");
        } finally {
            driver.quit();
        }
    }
}
