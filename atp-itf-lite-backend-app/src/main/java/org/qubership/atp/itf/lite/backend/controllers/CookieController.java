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

package org.qubership.atp.itf.lite.backend.controllers;

import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.COOKIES_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.IMPORT_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.PROJECT_ID;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.PROJECT_ID_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.PROJECT_PATH;
import static org.qubership.atp.itf.lite.backend.model.api.ApiPath.SERVICE_API_V1_PATH;

import java.util.List;
import java.util.UUID;

import org.qubership.atp.itf.lite.backend.model.api.dto.CookiesDto;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportFromRamRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;
import org.qubership.atp.itf.lite.backend.service.CookieService;
import org.qubership.atp.itf.lite.backend.utils.CookieUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(SERVICE_API_V1_PATH + PROJECT_PATH + PROJECT_ID_PATH + COOKIES_PATH)
@RequiredArgsConstructor
public class CookieController {

    private final CookieService cookieService;

    /**
     * update cookies for current user.
     * @param projectId project id
     * @param cookiesDto cookies to update
     * @return list of saved cookies
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectId, 'UPDATE')")
    @PostMapping
    @Transactional
    public ResponseEntity<List<CookiesDto>> saveCookies(@PathVariable(name = PROJECT_ID) UUID projectId,
                                                        @RequestBody List<CookiesDto> cookiesDto) {
        // Delete old cookies with same session id
        cookieService.deleteByUserIdAndProjectId(projectId);
        List<Cookie> cookies = CookieUtils.convertToCookieList(cookiesDto);
        cookieService.fillCookieInfo(cookies, projectId);
        return ResponseEntity.ok(CookieUtils.convertToCookiesDtoList(cookieService.save(cookies)));
    }

    /**
     * Get not expired cookies for current user and projectId.
     * @param projectId project id
     * @return list of not expired cookies
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectId, 'READ')")
    @GetMapping
    public ResponseEntity<List<CookiesDto>> getCookies(@PathVariable(name = PROJECT_ID) UUID projectId) {
        List<Cookie> cookies = cookieService.getNotExpiredCookiesByUserIdAndProjectId(projectId);
        return ResponseEntity.ok(CookieUtils.convertToCookiesDtoList(cookies));
    }

    /**
     * Import cookies from ram.
     * @param projectId project id
     * @param importFromRamRequest import request
     * @return list of not expired cookies
     */
    @PreAuthorize("@entityAccess.checkAccess(#projectId, 'UPDATE')")
    @PostMapping(value = IMPORT_PATH)
    public ResponseEntity<List<CookiesDto>> importCookieFromRam(@PathVariable(name = PROJECT_ID) UUID projectId,
                                                                @RequestBody
                                                                ImportFromRamRequest importFromRamRequest) {
        List<Cookie> cookies = cookieService.importCookiesFromRam(projectId, importFromRamRequest);
        return ResponseEntity.ok(CookieUtils.convertToCookiesDtoList(cookies));
    }
}
