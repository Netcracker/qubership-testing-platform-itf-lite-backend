/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.itf.lite.backend.utils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.net.HttpCookie;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.cookie.ClientCookie;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.exceptions.cookie.IllegalCookieException;
import org.qubership.atp.itf.lite.backend.feign.dto.PostmanCookieDto;
import org.qubership.atp.itf.lite.backend.model.api.dto.CookieDto;
import org.qubership.atp.itf.lite.backend.model.api.dto.CookiesDto;
import org.qubership.atp.itf.lite.backend.model.api.dto.ResponseCookie;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CookieUtils {

    private static final ModelMapper modelMapper = new ModelMapper();

    /**
     * Converts list of {@link CookiesDto} to list of {@link Cookie}.
     */
    public static List<Cookie> convertToCookieList(List<CookiesDto> cookiesDto) {
        List<Cookie> cookies = new ArrayList<>();
        cookiesDto.forEach(cookieDto -> {
            String domain = cookieDto.getDomain();
            cookieDto.getCookies().forEach(c -> {
                Cookie newCookie = new Cookie();
                newCookie.setDomain(domain);
                newCookie.setKey(c.getKey());
                newCookie.setValue(c.getValue());
                newCookie.setDisabled(c.isDisabled());
                cookies.add(newCookie);
            });
        });
        return cookies;
    }


    /**
     * Converts list of {@link Cookie} to list of {@link CookiesDto}.
     */
    public static List<CookiesDto> convertToCookiesDtoList(List<Cookie> cookies) {
        Map<String, List<Cookie>> mappedCookies = StreamUtils.toEntityListMap(cookies, Cookie::getDomain);
        List<CookiesDto> cookiesDto = new ArrayList<>();
        mappedCookies.forEach((domain, cookie) -> {
            List<CookieDto> listCookies = new ArrayList<>();
            cookie.forEach(c -> listCookies.add(modelMapper.map(c, CookieDto.class)));
            cookiesDto.add(new CookiesDto(domain, listCookies));
        });
        return cookiesDto;
    }

    /**
     * Converts list of {@link PostmanCookieDto} to list of {@link Cookie}.
     */
    public static List<Cookie> convertPostmanCookieDtoListToCookieList(List<PostmanCookieDto> cookiesDto) {
        List<Cookie> cookies = new ArrayList<>();
        cookiesDto.forEach(cookieDto -> {
            Cookie newCookie = new Cookie();
            List<HttpCookie> parsedCookies = HttpCookie.parse(cookieDto.getValue());
            newCookie.setKey(cookieDto.getKey());
            newCookie.setValue(cookieDto.getValue());
            newCookie.setDomain(parsedCookies.get(0).getDomain());
            cookies.add(newCookie);
        });
        return cookies;
    }

    /**
     * Converts list of {@link Cookie} to list of {@link PostmanCookieDto}.
     */
    public static List<PostmanCookieDto> convertCookieListToPostmanCookieDtoList(List<Cookie> cookies) {
        List<PostmanCookieDto> postmanCookieDtos = new ArrayList<>();
        cookies.forEach(cookie -> {
            if (!cookie.isDisabled()) {
                PostmanCookieDto postmanCookieDto = new PostmanCookieDto();
                postmanCookieDto.setKey(cookie.getKey());
                postmanCookieDto.setValue(collectToString(cookie));
                postmanCookieDtos.add(postmanCookieDto);
            }
        });
        return postmanCookieDtos;
    }

    private static String collectToString(Cookie cookie) {
        String valueCookie = cookie.getValue();
        if (cookie.getValue().toLowerCase().contains("domain")) {
            return valueCookie;
        } else if (!StringUtils.isEmpty(cookie.getDomain())) {
            return valueCookie + " Domain=" + cookie.getDomain() + ";";
        }
        return valueCookie;
    }

    /**
     * Converts list of {@link ResponseCookie} to list of {@link Cookie}.
     */
    public static List<Cookie> convertResponseCookieListToCookieList(List<ResponseCookie> responseCookieList) {
        List<Cookie> cookies = new ArrayList<>();
        responseCookieList.forEach(responseCookie -> {
            cookies.add(convertResponseCookieToCookie(responseCookie));
        });
        return cookies;
    }


    /**
     * Converts {@link ResponseCookie} to {@link Cookie}.
     */
    public static Cookie convertResponseCookieToCookie(ResponseCookie respCookie) {
        Cookie newCookie = new Cookie();
        newCookie.setKey(respCookie.getName());
        newCookie.setValue(getValueByResponseCookie(respCookie));
        newCookie.setDomain(respCookie.getDomain());
        return newCookie;
    }

    /**
     * Parses header value and extracts all cookies.
     * @param domain request domain
     * @param headerValue header value
     * @return list of parsed cookies
     */
    public static List<Cookie> parseCookieHeader(String domain, String headerValue) {
        if (StringUtils.isEmpty(domain)) {
            log.error("Domain is null");
            throw new IllegalCookieException("Domain can not be null or empty");
        }

        List<Cookie> cookies = new ArrayList<>();
        List<HttpCookie> parsedCookies = HttpCookie.parse(headerValue);
        parsedCookies.forEach(parsedCookie -> {
            Cookie newCookie = new Cookie();
            newCookie.setKey(parsedCookie.getName());
            newCookie.setValue(httpCookieToString(parsedCookie));
            newCookie.setDomain(StringUtils.isEmpty(parsedCookie.getDomain()) ? domain : parsedCookie.getDomain());
            cookies.add(newCookie);
        });
        return cookies;
    }

    /**
     * Converts list of {@link Cookie} to list of {@link ResponseCookie}.
     */
    public static List<ResponseCookie> convertCookieListToResponseCookieList(List<Cookie> cookies) {
        if (isNull(cookies)) {
            return null;
        }
        List<ResponseCookie> responseCookies = new ArrayList<>();
        cookies.forEach(cookie -> {
            ResponseCookie newCookie = new ResponseCookie();
            newCookie.setName(cookie.getKey());
            newCookie.setDomain(cookie.getDomain());

            List<HttpCookie> parsedCookies = HttpCookie.parse(cookie.getValue());

            newCookie.setValue(parsedCookies.get(0).getValue());
            newCookie.setPath(parsedCookies.get(0).getPath());
            newCookie.setExpires(
                    timestampToCookieDate(System.currentTimeMillis() + parsedCookies.get(0).getMaxAge() * 1000));
            newCookie.setHttpOnly(parsedCookies.get(0).isHttpOnly());
            newCookie.setSecure(parsedCookies.get(0).getSecure());
            responseCookies.add(newCookie);
        });
        return responseCookies;
    }

    /**
     * Collect value for Cookie by ResponseCookie.
     */
    public static String getValueByResponseCookie(ResponseCookie responseCookie) {
        StringBuilder sb = new StringBuilder();
        sb.append(responseCookie.getName()).append('=').append(responseCookie.getValue());

        if (!StringUtils.isEmpty(responseCookie.getDomain())) {
            sb.append("; Domain=").append(responseCookie.getDomain());
        }
        if (!StringUtils.isEmpty(responseCookie.getPath())) {
            sb.append("; Path=").append(responseCookie.getPath());
        }
        if (!StringUtils.isEmpty(responseCookie.getExpires())) {
            sb.append("; Expires=").append(responseCookie.getExpires());
        }
        if (responseCookie.isHttpOnly()) {
            sb.append("; HttpOnly");
        }
        if (responseCookie.isSecure()) {
            sb.append("; Secure");
        }

        return sb.toString();
    }

    /**
     * Convert timestamp to string in cookie format.
     *
     * @param timestamp timestamp
     * @return date string in cookie format
     */
    public static String timestampToCookieDate(long timestamp) {
        Date expdate = new Date();
        expdate.setTime(timestamp);
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(expdate);
    }


    /**
     * Convert date to string in cookie format.
     *
     * @param date date
     * @return date string in cookie format
     */
    public static String dateToCookieDate(Date date) {
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df.format(date);
    }

    private static String httpCookieToString(HttpCookie httpCookie) {
        StringJoiner sj = new StringJoiner("; ");
        if (StringUtils.isEmpty(httpCookie.getName())) {
            log.error("Cookie name is empty");
            throw new IllegalCookieException("Cookie name can not be empty");
        }
        String value = httpCookie.getValue();
        sj.add(String.format("%s=%s", httpCookie.getName(), isNull(value) ? "" : value));

        if (!StringUtils.isEmpty(httpCookie.getDomain())) {
            sj.add(String.format("%s=%s", Constants.DOMAIN_KEY, httpCookie.getDomain()));
        }
        if (!StringUtils.isEmpty(httpCookie.getPath())) {
            sj.add(String.format("%s=%s", Constants.PATH_KEY, httpCookie.getPath()));
        }
        if (httpCookie.getMaxAge() > -1) {
            sj.add(String.format("%s=%s", Constants.EXPIRES_KEY, timestampToCookieDate(
                    System.currentTimeMillis() + httpCookie.getMaxAge() * 1000)));
        }
        if (httpCookie.getSecure()) {
            sj.add(String.format("%s", Constants.SECURE_KEY));
        }
        if (httpCookie.isHttpOnly()) {
            sj.add(String.format("%s", Constants.HTTP_ONLY_KEY));
        }
        return sj.toString();
    }

    /**
     * Parses cookie from http client to response cookie.
     * @param requestDomain request domain
     * @param responseCookies cookies from http client
     * @return list of response cookies
     */
    public static List<ResponseCookie> parseResponseCookie(String requestDomain,
                                                           List<org.apache.http.cookie.Cookie> responseCookies) {
        List<ResponseCookie> respCookies = new ArrayList<>();
        responseCookies.forEach(respCookie -> {
            ResponseCookie c = new ResponseCookie();
            c.setName(respCookie.getName());
            c.setValue(respCookie.getValue());
            if (StringUtils.isEmpty(respCookie.getDomain())) {
                c.setDomain(requestDomain);
            } else {
                c.setDomain(respCookie.getDomain());
            }
            c.setPath(respCookie.getPath());
            if (respCookie.isPersistent()) {
                c.setExpires(dateToCookieDate(respCookie.getExpiryDate()));
            }
            c.setSecure(respCookie.isSecure());
            if (respCookie instanceof ClientCookie) {
                ClientCookie cc = (ClientCookie) respCookie;
                String httpOnly = cc.getAttribute("httponly");
                if (nonNull(httpOnly)) {
                    c.setHttpOnly(Boolean.parseBoolean(httpOnly));
                }
            }
            respCookies.add(c);
        });
        return respCookies;
    }

    /**
     * Add cookies from response to request cookies.
     * Overwrites cookies with same name and domain
     * @param requestCookies request cookies
     * @param responseCookies response cookies
     * @return merged list
     */
    public static List<Cookie> addResponseCookie(List<Cookie> requestCookies, List<ResponseCookie> responseCookies) {
        Map<String, Cookie> cookieMap = requestCookies
                .stream()
                .collect(Collectors.toMap(CookieUtils::getNameWithDomain, Function.identity()));
        responseCookies.forEach(respCookie -> {
            cookieMap.put(CookieUtils.getNameWithDomain(respCookie),
                    CookieUtils.convertResponseCookieToCookie(respCookie));
        });
        return new ArrayList<>(cookieMap.values());
    }

    private static String getNameWithDomain(Cookie cookie) {
        return cookie.getKey() + "-" + cookie.getDomain();
    }

    private static String getNameWithDomain(ResponseCookie cookie) {
        return cookie.getName() + "-" + cookie.getDomain();
    }

}
