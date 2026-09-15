package com.meethere.aireservation;

import com.meethere.dao.OrderDao;
import com.meethere.dao.UserDao;
import com.meethere.dao.VenueDao;
import com.meethere.entity.Order;
import com.meethere.entity.User;
import com.meethere.entity.Venue;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URLEncoder;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

abstract class AiReservationTestSupport {
    static final String BASE_URL = "http://localhost:8888";

    @Autowired protected UserDao userDao;
    @Autowired protected VenueDao venueDao;
    @Autowired protected OrderDao orderDao;

    protected User ensureUser(String userId) {
        User existing = userDao.findByUserID(userId);
        if (existing != null) return existing;
        User user = new User();
        user.setUserID(userId);
        user.setUserName("AI测试用户" + userId);
        user.setPassword("Test-" + userId + "!");
        user.setEmail(userId + "@meethere.local");
        user.setPhone("");
        user.setPicture("");
        user.setIsadmin(0);
        return userDao.saveAndFlush(user);
    }

    protected User ensureAdmin() {
        User existing = userDao.findAll().stream()
                .filter(user -> user.getIsadmin() == 1).findFirst().orElse(null);
        if (existing != null) return existing;
        User admin = new User();
        admin.setUserID("ai_test_admin");
        admin.setUserName("AI测试管理员");
        admin.setPassword("TestAdmin01!");
        admin.setEmail("ai_test_admin@meethere.local");
        admin.setPhone("");
        admin.setPicture("");
        admin.setIsadmin(1);
        return userDao.saveAndFlush(admin);
    }

    protected Venue requireBasketballVenue() {
        Venue venue = venueDao.findByVenueName("篮球馆");
        if (venue == null) {
            throw new AssertionError("请先在数据库中准备名称为“篮球馆”的场馆");
        }
        return venue;
    }

    protected WebDriver openChrome() throws Exception {
        WebDriver driver = ChromeDriverSupport.openBrowser();
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(20));
        driver.get(BASE_URL + "/index");
        return driver;
    }

    protected void login(WebDriver driver, User user) {
        driver.get(BASE_URL + "/login");
        driver.findElement(By.id("userID")).sendKeys(user.getUserID());
        driver.findElement(By.id("password")).sendKeys(user.getPassword());
        driver.findElement(By.id("submit")).click();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        Alert alert = wait.until(ExpectedConditions.alertIsPresent());
        assertEquals("登录成功！", alert.getText());
        alert.accept();
        wait.until(ExpectedConditions.urlMatches(".*/(index|admin_index)$"));
    }

    protected LocalDate findFreeDay(Venue venue, int startHour, int hours) {
        List<Order> orders = orderDao.findAll();
        for (int offset = 1; offset <= 60; offset++) {
            LocalDate day = LocalDate.now().plusDays(offset);
            LocalDateTime start = day.atTime(startHour, 0);
            LocalDateTime end = start.plusHours(hours);
            boolean occupied = orders.stream().anyMatch(order -> order.getStartTime() != null
                    && order.getVenueID() == venue.getVenueID()
                    && order.getStartTime().isBefore(end)
                    && order.getStartTime().plusHours(order.getHours()).isAfter(start));
            if (!occupied) return day;
        }
        throw new AssertionError("未来60天没有可用测试日期");
    }

    protected Order saveOrder(User user, Venue venue, LocalDateTime start, int hours, int state) {
        Order order = new Order();
        order.setUserID(user.getUserID());
        order.setVenueID(venue.getVenueID());
        order.setOrderTime(LocalDateTime.now());
        order.setStartTime(start);
        order.setHours(hours);
        order.setTotal(hours * venue.getPrice());
        order.setState(state);
        return orderDao.saveAndFlush(order);
    }

    protected Set<Integer> orderIds() {
        return orderDao.findAll().stream().map(Order::getOrderID).collect(Collectors.toSet());
    }

    protected List<Order> newOrders(Set<Integer> before) {
        return orderDao.findAll().stream()
                .filter(order -> !before.contains(order.getOrderID()))
                .collect(Collectors.toList());
    }

    protected Map<String, String> params(String... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put(values[i], values[i + 1]);
        return result;
    }

    protected HttpResult post(WebDriver driver, String path, Map<String, String> parameters) {
        String body = parameters.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        String script = "const done=arguments[arguments.length-1];"
                + "fetch(arguments[0],{method:'POST',credentials:'same-origin',redirect:'follow',"
                + "headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},body:arguments[1]})"
                + ".then(async r=>done([r.status,r.url,(await r.text()).slice(0,240)]))"
                + ".catch(e=>done([0,'',String(e)]));";
        Object raw = ((JavascriptExecutor) driver).executeAsyncScript(script, path, body);
        List<?> values = raw instanceof List ? (List<?>) raw : new ArrayList<>();
        int status = values.isEmpty() ? 0 : ((Number) values.get(0)).intValue();
        String url = values.size() > 1 ? String.valueOf(values.get(1)) : "";
        String text = values.size() > 2 ? String.valueOf(values.get(2)) : "";
        return new HttpResult(status, url, text);
    }

    protected HttpResult get(WebDriver driver, String path) {
        String script = "const done=arguments[arguments.length-1];"
                + "fetch(arguments[0],{credentials:'same-origin',redirect:'follow'})"
                + ".then(async r=>done([r.status,r.url,(await r.text()).slice(0,240)]))"
                + ".catch(e=>done([0,'',String(e)]));";
        Object raw = ((JavascriptExecutor) driver).executeAsyncScript(script, path);
        List<?> values = (List<?>) raw;
        return new HttpResult(((Number) values.get(0)).intValue(),
                String.valueOf(values.get(1)), String.valueOf(values.get(2)));
    }

    protected void showEvidence(WebDriver driver, String caseId, boolean passed, String... lines) {
        String text = caseId + "\n" + String.join("\n", lines);
        String color = passed ? "#087f5b" : "#c92a2a";
        ((JavascriptExecutor) driver).executeScript(
                "let box=document.getElementById('ai-test-evidence');"
                        + "if(!box){box=document.createElement('pre');box.id='ai-test-evidence';document.body.appendChild(box);}"
                        + "box.textContent=arguments[0];"
                        + "box.style='position:fixed;z-index:2147483647;left:4%;top:8%;width:92%;max-height:84%;"
                        + "overflow:auto;padding:28px;box-sizing:border-box;background:white;border:8px solid '+arguments[1]+';"
                        + "color:#202124;font:700 24px/1.6 Microsoft YaHei, sans-serif;white-space:pre-wrap;box-shadow:0 8px 40px #0008';",
                text, color);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static final class HttpResult {
        final int status;
        final String url;
        final String text;

        HttpResult(int status, String url, String text) {
            this.status = status;
            this.url = url;
            this.text = text;
        }
    }
}
