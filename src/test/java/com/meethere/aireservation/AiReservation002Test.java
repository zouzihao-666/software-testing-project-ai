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

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
class AiReservation002Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-002 快速双击造成重复下单")
    void shouldTreatRapidDoubleSubmitAsOneOrder() throws Exception {
        User user = ensureUser("user01");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 11, 1);
        Set<Integer> before = orderIds();

        WebDriver driver = openChrome();
        try {
            login(driver, user);
            driver.get(BASE_URL + "/order_place.do?venueID=" + venue.getVenueID());
            List<HttpResult> responses = postTwice(driver, "/addOrder.do",
                    params("venueName", venue.getVenueName(), "date", day.toString(),
                            "startTime", day + " 11:00", "hours", "1"));
            List<Order> created = newOrders(before);
            boolean passed = created.size() == 1;
            showEvidence(driver, "RES-AI-002 快速双击重复提交", passed,
                    "两次请求状态：" + responses.get(0).status + " / " + responses.get(1).status,
                    "新增订单数：" + created.size(),
                    passed ? "通过：重复请求只生成一笔订单" : "未通过：双击产生了重复订单");
            assertEquals(1, created.size(), "快速双击只能生成一笔订单；异常订单保留供检查");
        } finally {
            driver.quit();
        }
    }
}
