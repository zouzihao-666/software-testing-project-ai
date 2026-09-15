package com.meethere.aireservation;

import com.meethere.entity.Order;
import com.meethere.entity.User;
import com.meethere.entity.Venue;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
class AiReservation011Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-011 跨站请求伪造提交预约")
    void shouldRejectCrossOriginFormWithoutCsrfToken() throws Exception {
        User user = ensureUser("user01");
        Venue venue = requireBasketballVenue();
        LocalDate day = findFreeDay(venue, 13, 1);
        Set<Integer> before = orderIds();
        String html = "<!doctype html><meta charset='UTF-8'><title>第三方页面</title>"
                + "<h1>第三方页面正在自动提交预约</h1>"
                + "<form id='attack' method='post' action='" + BASE_URL + "/addOrder.do'>"
                + hidden("venueName", venue.getVenueName())
                + hidden("date", day.toString())
                + hidden("startTime", day + " 13:00")
                + hidden("hours", "1") + "</form><script>attack.submit()</script>";

        HttpServer attacker = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        attacker.createContext("/", exchange -> {
            byte[] body = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        attacker.start();

        WebDriver driver = openChrome();
        try {
            login(driver, user);
            driver.get("http://localhost:" + attacker.getAddress().getPort() + "/");
            Thread.sleep(2000);
            List<Order> created = newOrders(before);
            boolean passed = created.isEmpty();
            showEvidence(driver, "RES-AI-011 CSRF预约", passed,
                    "请求来源：第三方网页",
                    "请求中没有防伪令牌",
                    "新增订单数：" + created.size(),
                    passed ? "通过：跨站请求被拒绝" : "未通过：登录状态下跨站表单成功创建订单");
            assertTrue(created.isEmpty(), "缺少防伪令牌的跨站请求不能创建订单；异常订单保留供检查");
        } finally {
            driver.quit();
            attacker.stop(0);
        }
    }

    private String hidden(String name, String value) {
        return "<input type='hidden' name='" + name + "' value='" + value + "'>";
    }
}
