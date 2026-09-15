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
class AiReservation015Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-015 已驳回订单再次审核通过")
    void shouldRejectApprovalOfAlreadyRejectedOrder() throws Exception {
        User owner = ensureUser("user01");
        User admin = ensureAdmin();
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 18, 1);
        Order rejected = saveOrder(owner, venue, day.atTime(18, 0), 1, 4);

        WebDriver driver = openChrome();
        try {
            login(driver, admin);
            driver.get(BASE_URL + "/reservation_manage");
            HttpResult response = post(driver, "/passOrder.do",
                    params("orderID", String.valueOf(rejected.getOrderID())));
            clearPersistenceContext();
            Order after = orderDao.findByOrderID(rejected.getOrderID());
            boolean passed = after != null && after.getState() == 4;
            showEvidence(driver, "RES-AI-015 非法状态回退", passed,
                    "订单初始状态：4（已驳回）",
                    "再次调用审核通过接口",
                    "HTTP状态：" + response.status,
                    "操作后状态：" + (after == null ? "订单不存在" : after.getState()),
                    passed ? "通过：已驳回状态保持不变" : "未通过：已驳回订单被重新改为已审核");
            assertEquals(4, after.getState(), "已驳回订单不能再次审核通过");
        } finally {
            driver.quit();
        }
    }
}
