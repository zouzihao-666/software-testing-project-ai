package com.meethere.aireservation;

import com.meethere.entity.Order;
import com.meethere.entity.User;
import com.meethere.entity.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
class AiReservation009Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-009 普通用户修改他人订单")
    void shouldPreventUserFromModifyingAnotherUsersOrder() throws Exception {
        User attacker = ensureUser("user01");
        User owner = ensureUser("user02");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 14, 3);
        LocalDateTime originalTime = day.atTime(14, 0);
        Order target = saveOrder(owner, venue, originalTime, 1, 1);

        WebDriver driver = openChrome();
        try {
            login(driver, attacker);
            driver.get(BASE_URL + "/order_manage");
            HttpResult response = post(driver, "/modifyOrder",
                    params("venueName", venue.getVenueName(), "date", day.toString(),
                            "startTime", day + " 16:00", "hours", "1",
                            "orderID", String.valueOf(target.getOrderID())));
            clearPersistenceContext();
            Order after = orderDao.findByOrderID(target.getOrderID());
            boolean passed = after != null && owner.getUserID().equals(after.getUserID())
                    && originalTime.equals(after.getStartTime());
            showEvidence(driver, "RES-AI-009 越权修改", passed,
                    "当前用户：" + attacker.getUserID(),
                    "订单所有者：" + owner.getUserID(),
                    "HTTP状态：" + response.status,
                    "修改后所有者：" + (after == null ? "订单不存在" : after.getUserID()),
                    "修改后时间：" + (after == null ? "无" : after.getStartTime()),
                    passed ? "通过：他人订单未改变" : "未通过：攻击者修改并接管了他人订单");
            assertNotNull(after);
            assertEquals(owner.getUserID(), after.getUserID(), "订单所有者不能被修改请求替换");
            assertEquals(originalTime, after.getStartTime(), "越权请求不能改变订单时间");
        } finally {
            driver.quit();
        }
    }
}
