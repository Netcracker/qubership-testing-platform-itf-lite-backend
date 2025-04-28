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

package org.qubership.atp.itf.lite.backend.service;

import static org.qubership.atp.itf.lite.backend.utils.Constants.COOKIE_HEADER_KEY;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CookiesRepository;
import org.qubership.atp.itf.lite.backend.exceptions.cookie.IllegalCookieException;
import org.qubership.atp.itf.lite.backend.exceptions.requests.ItfLiteUrlSyntaxException;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportFromRamRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.http.HttpHeaderSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import clover.org.apache.commons.lang.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CookieService {

    private final CookiesRepository cookiesRepository;
    private final Provider<UserInfo> userInfoProvider;
    private final RamService ramService;

    /**
     * Gets a list of all not expired cookies by sessionId.
     * Deletes all expired cookies from the result and from the database
     *
     * @return list of filtered cookies
     */
    @Transactional
    public List<Cookie> getNotExpiredCookiesByUserIdAndProjectId(UUID projectId) {
        List<Cookie> cookies = cookiesRepository.findAllByUserIdAndProjectId(userInfoProvider.get().getId(), projectId);
        List<Cookie> filteredCookies = filterExpired(cookies);
        cookies.removeAll(filteredCookies);
        cookiesRepository.removeAllByIdIn(StreamUtils.extractIds(cookies));
        return filteredCookies;
    }

    /**
     * Only saves cookies that haven't expired.
     *
     * @param cookies list of cookies to save
     * @return list of saved cookies
     */
    @Transactional
    public List<Cookie> save(List<Cookie> cookies) {
        cookies = filterExpired(cookies);
        return cookiesRepository.saveAll(cookies);
    }

    @Transactional
    public void deleteByUserIdAndProjectId(UUID projectId) {
        cookiesRepository.removeAllByUserIdAndProjectId(userInfoProvider.get().getId(), projectId);
    }

    @Transactional
    public void deleteByExecutionRequestIdAndTestRunId(UUID executionRequestId, UUID testRunId) {
        cookiesRepository.removeAllByExecutionRequestIdAndTestRunId(executionRequestId, testRunId);
    }

    /**
     * Validates cookies and deletes non-valid or expired cookies.
     *
     * @param cookies list of cookies to filter
     * @return filtered list
     */
    private List<Cookie> filterExpired(List<Cookie> cookies) {
        try {
            Map<String, HttpCookie> parsedCookies = cookies.stream()
                    .flatMap(cookie -> {
                        try {
                            return HttpCookie.parse(cookie.getValue()).stream()
                                    .peek(c -> c.setDomain(cookie.getDomain()));
                        } catch (IllegalArgumentException ex) {
                            log.error("Failed to parse cookie: {}", cookie.getKey());
                            throw new IllegalCookieException(ex.getMessage());
                        }
                    })
                    .filter(cookie -> !cookie.hasExpired())
                    .collect(Collectors.toMap(this::getNameWithDomain, Function.identity()));
            return cookies.stream()
                    .filter(cookie -> parsedCookies.containsKey(getNameWithDomain(cookie)))
                    .collect(Collectors.toList());
        } catch (IllegalStateException ex) {
            log.error("Cookie name is duplicated", ex);
            throw new IllegalCookieException("The cookie name must be unique within the same domain");
        }
    }

    private String getNameWithDomain(HttpCookie cookie) {
        return cookie.getName() + "-" + cookie.getDomain();
    }

    private String getNameWithDomain(Cookie cookie) {
        return cookie.getKey() + "-" + cookie.getDomain();
    }

    /**
     * Fills in the technical information of the cookie.
     * Adds the project, session and user IDs
     *
     * @param cookies   list of cookies that should be filled
     * @param projectId project id
     */
    public void fillCookieInfo(List<Cookie> cookies, UUID projectId) {
        cookies.forEach(cookie -> {
            cookie.setProjectId(projectId);
            cookie.setUserId(userInfoProvider.get().getId());
        });
    }

    /**
     *  Fill cookie info.
     */
    public void fillCookieInfoWithExecutionRequestInfo(List<Cookie> cookies, UUID executionRequestId, UUID testRunId) {
        cookies.forEach(cookie -> {
            cookie.setExecutionRequestId(executionRequestId);
            cookie.setTestRunId(testRunId);
        });
    }

    /**
     * Get all cookies by Execution id and Test Run id including Test Run id is null.
     * @param executionRequestId Execution Request id.
     * @param testRunId Test Run id.
     * @return List o Cookies.
     */
    public List<Cookie> getAllByExecutionRequestIdAndTestRunId(UUID executionRequestId, UUID testRunId) {
        return filterExpired(
                cookiesRepository
                        .findAllByExecutionRequestIdAndTestRunIdOrTestRunIdIsNull(
                            executionRequestId,
                            testRunId));
    }

    /**
     * Creates cookie header for specified url.
     */
    public HttpHeaderSaveRequest cookieListToRequestHeader(String url, List<Cookie> cookies) {
        try {
            return cookieListToRequestHeader(new URI(url), cookies);
        } catch (URISyntaxException ex) {
            log.error("Failed to parse url", ex);
            throw new ItfLiteUrlSyntaxException(ex.getMessage());
        }
    }

    /**
     * Creates cookie header for specified url.
     */
    public HttpHeaderSaveRequest cookieListToRequestHeader(URI uri, List<Cookie> cookies) {
        return new HttpHeaderSaveRequest(COOKIE_HEADER_KEY, cookiesToString(uri, cookies));
    }

    /**
     * Filter cookie list for current url.
     * Filter by domain, path, httpOnly and secure
     *
     * @param url request url
     * @param cookies cookies to filter
     * @return return filtered cookie list
     */
    public List<Cookie> filterCookie(String url, List<Cookie> cookies) {
        try {
            URI uri = new URI(url);
            return filterCookie(uri, cookies);
        } catch (URISyntaxException ex) {
            log.error("Failed to parse url", ex);
            throw new ItfLiteUrlSyntaxException(ex.getMessage());
        }
    }


    /**
     * Filter cookie list for current uri.
     * Filter by domain, path, httpOnly and secure
     *
     * @param uri request uri
     * @param cookies cookies to filter
     * @return return filtered cookie list
     */
    public List<Cookie> filterCookie(URI uri, List<Cookie> cookies) {
        return cookies.stream()
                .filter(cookie -> {
                    if (!cookie.isDisabled()) {
                        List<HttpCookie> parsedCookies = HttpCookie.parse(cookie.getValue());
                        if (!CollectionUtils.isEmpty(parsedCookies)) {
                            for (HttpCookie parsedCookie : parsedCookies) {
                                if (StringUtils.isEmpty(parsedCookie.getDomain())) {
                                    parsedCookie.setDomain(cookie.getDomain());
                                }
                                if (!filterCookieForUri(uri, parsedCookie)) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toList());
    }

    /**
     * Converts the cookie to a header string.
     * Not contains all info about cookies!!!
     * Disabled or expired cookies are not included in the result
     */
    public String cookiesToString(URI uri, List<Cookie> cookies) {
        CookieManager cookieManager = new CookieManager();
        cookies.forEach(cookie -> {
            List<HttpCookie> parsedCookies = HttpCookie.parse(cookie.getValue());
            if (!cookie.isDisabled() && !CollectionUtils.isEmpty(parsedCookies)) {
                parsedCookies.forEach(parsedCookie -> {
                    if (StringUtils.isEmpty(parsedCookie.getDomain())) {
                        parsedCookie.setDomain(cookie.getDomain());
                    }
                    if (filterCookieForUri(uri, parsedCookie)) {
                        cookieManager.getCookieStore().add(null, parsedCookie);
                    }
                });
            }
        });
        return StringUtils.join(cookieManager.getCookieStore().getCookies(), ";");
    }

    private boolean filterCookieForUri(URI uri, HttpCookie cookie) {
        // expired check
        if (cookie.hasExpired()) {
            return false;
        }
        // domain check
        if (!StringUtils.isEmpty(cookie.getDomain()) && !HttpCookie.domainMatches(cookie.getDomain(), uri.getHost())) {
            return false;
        }
        // path check
        if (!pathMatches(cookie.getPath(), uri.getRawPath())) {
            return false;
        }
        // httpOnlyCheck
        String scheme = uri.getScheme();
        if (cookie.isHttpOnly()
                && !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
            return false;
        }
        // Secure check
        return !cookie.getSecure() || "https".equalsIgnoreCase(uri.getScheme());
    }

    private boolean pathMatches(String cookiePath, String requestPath) {
        if (StringUtils.isEmpty(cookiePath) || "/".equals(cookiePath)) {
            return true;
        }
        cookiePath = cookiePath.toLowerCase();
        requestPath = requestPath.toLowerCase();
        if (cookiePath.endsWith("/")) {
            return requestPath.startsWith(cookiePath);
        }
        return requestPath.equals(cookiePath);
    }

    @Transactional
    public void deleteIfErIdOrTrIdSpecified() {
        cookiesRepository.removeAllByExecutionRequestIdIsNotNullAndTestRunIdIsNotNull();
    }

    @Transactional
    public void deleteAllByIdIn(Collection<UUID> ids) {
        cookiesRepository.removeAllByIdIn(ids);
    }

    public List<Cookie> getAll() {
        return cookiesRepository.findAll();
    }

    /**
     * Import cookie from ram for current user.
     * @param projectId project id
     * @param importRequest import request
     * @return list of all not expired cookies
     */
    @Transactional
    public List<Cookie> importCookiesFromRam(UUID projectId, ImportFromRamRequest importRequest) {
        List<Cookie> importedCookies = ramService.importCookies(importRequest);
        List<Cookie> savedCookie = getNotExpiredCookiesByUserIdAndProjectId(projectId);
        if (CollectionUtils.isEmpty(importedCookies)) {
            // nothing to do, just return already saved cookies
            return savedCookie;
        }
        Map<String, List<Cookie>> mappedImportedCookies = importedCookies.stream()
                .collect(Collectors.groupingBy(this::getNameWithDomain));

        Map<String, Cookie> mappedSavedCookies = savedCookie.stream()
                .collect(Collectors.toMap(this::getNameWithDomain, Function.identity()));

        mappedImportedCookies.forEach((k,v) -> {
            int lastIndex = v.size() - 1;
            mappedSavedCookies.put(k, v.get(lastIndex));
        });
        return filterExpired(new ArrayList<>(mappedSavedCookies.values()));
    }
}
