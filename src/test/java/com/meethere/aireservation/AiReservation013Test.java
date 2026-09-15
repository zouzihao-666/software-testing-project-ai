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
class AiReservation013Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-013 修改不存在的订单编号")
    void shouldReturnControlledResponseForMissingOrder() throws Exception {
        User user = ensureUser("user01");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 15, 1);
        int missingOrderId = 999999;
        while (orderDao.findByOrderID(missingOrderId) != null) missingOrderId++;
        Set<Integer> before = orderIds();

        WebDriver driver = openChrome();
        try {
            login(driver, user);
            driver.get(BASE_URL + "/order_manage");
            HttpResult response = post(driver, "/modifyOrder",
                    params("venueName", venue.getVenueName(), "date", day.toString(),
                            "startTime", day + " 15:00", "hours", "1",
                            "orderID", String.valueOf(missingOrderId)));
            List<Order> created = newOrders(before);
            boolean passed = created.isEmpty() && response.status >= 400 && response.status < 500;
            showEvidence(driver, "RES-AI-013 不存在的订单编号", passed,
                    "输入orderID：" + missingOrderId,
                    "HTTP状态：" + response.status,
                    "新增订单数：" + created.size(),
                    passed ? "通过：返回可识别的客户端错误" : "未通过：不存在的编号触发服务器异常");
            assertTrue(created.isEmpty(), "修改不存在的订单不能新增数据");
            assertTrue(response.status >= 400 && response.status < 500,
                    "不存在的订单应返回4xx提示，不能返回500服务器错误");
        } finally {
            driver.quit();
        }
    }
}
