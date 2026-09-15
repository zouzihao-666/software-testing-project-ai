package com.meethere.aireservation;

import com.meethere.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.hibernate.ddl-auto=none")
class AiReservation014Test extends AiReservationTestSupport {

    @Test
    @DisplayName("RES-AI-014 预约列表页码为0")
    void shouldHandleZeroPageWithoutServerError() throws Exception {
        User user = ensureUser("user01");
        WebDriver driver = openChrome();
        try {
            login(driver, user);
            driver.get(BASE_URL + "/order_manage");
            HttpResult response = get(driver, "/getOrderList.do?page=0");
            boolean passed = response.status > 0 && response.status < 500;
            showEvidence(driver, "RES-AI-014 页码边界", passed,
                    "输入：page=0",
                    "HTTP状态：" + response.status,
                    passed ? "通过：系统返回参数提示或按第一页处理" : "未通过：page=0触发HTTP 500");
            assertTrue(passed, "page=0不能引发服务器内部错误");
        } finally {
            driver.quit();
        }
    }
}
