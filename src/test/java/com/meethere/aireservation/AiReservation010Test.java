package com.meethere.aireservation;

import com.meethere.entity.Order;
import com.meethere.entity.User;
import com.meethere.entity.Venue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
class AiReservation010Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-010 普通用户直接调用审核通过接口")
    void shouldRejectApprovalRequestFromNormalUser() throws Exception {
        User user = ensureUser("user01");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 17, 1);
        Order pending = saveOrder(user, venue, day.atTime(17, 0), 1, 1);

        WebDriver driver = openChrome();
        try {
            login(driver, user);
            driver.get(BASE_URL + "/order_manage");
            HttpResult response = post(driver, "/passOrder.do",
                    params("orderID", String.valueOf(pending.getOrderID())));
            clearPersistenceContext();
            Order after = orderDao.findByOrderID(pending.getOrderID());
            boolean passed = after != null && after.getState() == 1;
            showEvidence(driver, "RES-AI-010 普通用户越权审核", passed,
                    "当前用户：" + user.getUserID() + "（普通用户）",
                    "目标订单：" + pending.getOrderID(),
                    "HTTP状态：" + response.status,
                    "审核后状态：" + (after == null ? "订单不存在" : after.getState()),
                    passed ? "通过：审核请求被拒绝" : "未通过：普通用户把订单改为已审核");
            assertEquals(1, after.getState(), "普通用户无权审核订单，状态必须保持未审核");
        } finally {
            driver.quit();
        }
    }
}
