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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.UserSettingsRepository;
import org.qubership.atp.itf.lite.backend.model.entities.user.UserSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService extends CrudService<UserSettings> {

    public final String sessionState = "session_state";
    public final String sub = "sub";

    private String issuer;

    @Value("${keycloak.auth-server-url}")
    private String baseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Qualifier("m2mRestTemplate")
    private RestTemplate m2mRestTemplate;

    private final UserSettingsRepository userSettingsRepository;

    private final ModelMapper modelMapper;

    /**
     * Constructor.
     */
    @Autowired
    public UserService(RestTemplate m2mRestTemplate, UserSettingsRepository userSettingsRepository,
                       ModelMapper modelMapper) {
        this.m2mRestTemplate = m2mRestTemplate;
        this.userSettingsRepository = userSettingsRepository;
        this.modelMapper = modelMapper;
    }

    @PostConstruct
    public void init() {
        this.issuer = this.baseUrl + "/admin/realms/" + this.realm;
    }

    @Override
    protected JpaRepository<UserSettings, UUID> repository() {
        return userSettingsRepository;
    }

    /**
     * Gets user info by id.
     *
     * @param token user token
     * @return user info.
     */
    public UserInfo getUserInfoByToken(String token) {
        final UUID userId = getUserIdFromToken(token);
        UserInfo userInfo;
        try {
            userInfo = m2mRestTemplate.getForObject(issuer + "/users/" + userId.toString(), UserInfo.class);
        } catch (Exception e) {
            log.error(String.format("Could not find user: %s ", userId.toString()));
            return null;
        }

        return userInfo;
    }

    /**
     * Gets user info by id.
     *
     * @param token user token
     * @return user info
     */
    public UUID getUserIdFromToken(String token) {
        UUID userId = null;
        if (StringUtils.isNotBlank(token)) {
            try {
                token = token.split(" ")[1];
                JsonParser parser = JsonParserFactory.getJsonParser();
                Map<String, ?> tokenData = parser.parseMap(JwtHelper.decode(token).getClaims());
                userId = UUID.fromString(tokenData.get(sub).toString());
            } catch (Exception e) {
                log.warn("Cannot parse token with error: ", e);
            }
        }
        return userId;
    }

    /**
     * Save user settings.
     *
     * @param userSettings settings entity
     * @param token   user token
     * @return saved entity object
     */
    public UserSettings saveUserSettings(UserSettings userSettings, String token) {
        log.debug("Get userId by token");
        UUID userId = getUserIdFromToken(token);
        log.debug("Check if settings with name = {} already exists", userSettings.getName());
        UserSettings existedUserSettings = userSettingsRepository.findByUserIdAndName(userId, userSettings.getName());
        if (existedUserSettings == null) {
            log.info("Create user settings by request: {}", userSettings);
            existedUserSettings = modelMapper.map(userSettings, UserSettings.class);
            existedUserSettings.setUserId(userId);
        } else {
            log.debug("Update existed user settings");
            existedUserSettings.setVisibleColumns(userSettings.getVisibleColumns());
        }

        log.info("Saved user settings: {}", existedUserSettings);
        return save(existedUserSettings);
    }

    /**
     * Get user settings via search criteria.
     *
     * @param token user token
     * @return found settings entity
     */
    public List<UserSettings> getSettingsByUser(String token) {
        UUID userId = getUserIdFromToken(token);
        return userSettingsRepository.findByUserId(userId);
    }
}