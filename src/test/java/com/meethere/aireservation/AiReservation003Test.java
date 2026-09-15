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
class AiReservation003Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-003 date与startTime日期不一致")
    void shouldRejectInconsistentDateParameters() throws Exception {
        User user = ensureUser("user01");
        Venue venue = requireBasketballVenue();
        LocalDate startDay = findFreeDay(venue, 10, 1);
        LocalDate declaredDay = startDay.plusDays(1);
        Set<Integer> before = orderIds();

        WebDriver driver = openChrome();
        try {
            login(driver, user);
            driver.get(BASE_URL + "/order_place.do?venueID=" + venue.getVenueID());
            HttpResult response = post(driver, "/addOrder.do",
                    params("venueName", venue.getVenueName(), "date", declaredDay.toString(),
                            "startTime", startDay + " 10:00", "hours", "1"));
            List<Order> created = newOrders(before);
            boolean passed = created.isEmpty();
            showEvidence(driver, "RES-AI-003 日期参数一致性", passed,
                    "date参数：" + declaredDay,
                    "startTime日期：" + startDay,
                    "HTTP状态：" + response.status,
                    "新增订单数：" + created.size(),
                    passed ? "通过：不一致参数被拒绝" : "未通过：后台忽略date并生成订单");
            assertTrue(created.isEmpty(), "date与startTime日期不一致时不能生成订单；异常订单保留供检查");
        } finally {
            driver.quit();
        }
    }
}
