package org.qubership.atp.itf.lite.backend.utils;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanCookieDto;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;

@ExtendWith(MockitoExtension.class)
class CookieUtilsTest {

    CookieUtils cookieUtils;

    @Test
    void convertCookieListToPostmanCookieDtoList() {
        List<Cookie> cookies = new ArrayList<>();
        Cookie cookie1 = new Cookie();
        cookie1.setKey("Cookie_1");
        cookie1.setValue("Cookie_1=value; Path=/;");
        cookie1.setDomain("domain1");
        cookies.add(cookie1);

        Cookie cookie2 = new Cookie();
        cookie2.setKey("Cookie_2");
        cookie2.setValue("Cookie_2=value; Path=/;");
        cookie2.setDomain("");
        cookies.add(cookie2);

        Cookie cookie3 = new Cookie();
        cookie3.setKey("Cookie_3");
        cookie3.setValue("Cookie_3=value; Path=/;");
        cookie3.setDomain(null);
        cookies.add(cookie3);

        Cookie cookie4 = new Cookie();
        cookie4.setKey("Cookie_4");
        cookie4.setValue("Cookie_4=value; Path=/; Domain=domain4;");
        cookie4.setDomain("domainDuplicate");
        cookies.add(cookie4);

        List<PostmanCookieDto>  postmanCookieDtos = cookieUtils.convertCookieListToPostmanCookieDtoList(cookies);

        Assertions.assertEquals(cookie1.getValue() + " Domain=domain1;", postmanCookieDtos.get(0).getValue(),
                "Not correctly set value");
        Assertions.assertEquals(cookie4.getValue(), postmanCookieDtos.get(3).getValue(),
                "Not correctly set value");
        Assertions.assertFalse(postmanCookieDtos.get(2).getValue().contains("Domain"), "cookie3 have not domain");
        Assertions.assertFalse(postmanCookieDtos.get(1).getValue().contains("Domain"), "cookie3 have not domain");
        Assertions.assertTrue(postmanCookieDtos.get(0).getValue().contains("Domain"), "cookie1 have domain");

        System.out.println(postmanCookieDtos);
    }
}
