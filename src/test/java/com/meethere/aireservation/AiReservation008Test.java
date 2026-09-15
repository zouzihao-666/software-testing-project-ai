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
class AiReservation008Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-008 普通用户删除他人订单")
    void shouldPreventUserFromDeletingAnotherUsersOrder() throws Exception {
        User attacker = ensureUser("user01");
        User owner = ensureUser("user02");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 13, 1);
        Order target = saveOrder(owner, venue, day.atTime(13, 0), 1, 1);

        WebDriver driver = openChrome();
        try {
            login(driver, attacker);
            driver.get(BASE_URL + "/order_manage");
            HttpResult response = post(driver, "/delOrder.do",
                    params("orderID", String.valueOf(target.getOrderID())));
            Order after = orderDao.findByOrderID(target.getOrderID());
            boolean passed = after != null;
            showEvidence(driver, "RES-AI-008 越权删除", passed,
                    "当前用户：" + attacker.getUserID(),
                    "订单所有者：" + owner.getUserID(),
                    "目标订单：" + target.getOrderID(),
                    "HTTP状态：" + response.status,
                    passed ? "通过：他人订单保持不变" : "未通过：普通用户删除了他人订单");
            assertNotNull(after, "系统必须校验订单所有者，不能仅凭orderID删除他人订单");
        } finally {
            driver.quit();
        }
    }
}
