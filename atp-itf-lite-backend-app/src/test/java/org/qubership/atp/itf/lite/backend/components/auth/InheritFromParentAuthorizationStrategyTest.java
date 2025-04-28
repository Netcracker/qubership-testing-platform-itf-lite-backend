package org.qubership.atp.itf.lite.backend.components.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateFolder;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateRandomHttpRequestEntitySaveRequest;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.crypt.exception.AtpDecryptException;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.enums.auth.OAuth2GrantType;
import org.qubership.atp.itf.lite.backend.enums.auth.RequestAuthorizationType;
import org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyRequest;
import org.qubership.atp.itf.lite.backend.model.AuthorizationStrategyResponse;
import org.qubership.atp.itf.lite.backend.model.api.request.HttpRequestEntitySaveRequest;
import org.qubership.atp.itf.lite.backend.model.api.request.auth.InheritFromParentAuthorizationSaveRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BasicRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.BearerRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.InheritFromParentRequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.auth.OAuth2RequestAuthorization;
import org.qubership.atp.itf.lite.backend.model.entities.http.RequestHeader;
import org.qubership.atp.itf.lite.backend.service.EncryptionService;
import org.qubership.atp.itf.lite.backend.service.FolderService;
import org.qubership.atp.itf.lite.backend.service.TemplateResolverService;
import org.qubership.atp.itf.lite.backend.service.rest.RestTemplateService;
import org.qubership.atp.itf.lite.backend.utils.AuthorizationUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.atp.macros.core.processor.Evaluator;

public class InheritFromParentAuthorizationStrategyTest {

    private final ThreadLocal<EncryptionService> encryptionService = new ThreadLocal<>();
    private final ThreadLocal<FolderService> folderService = new ThreadLocal<>();
    private final ThreadLocal<RequestAuthorizationRegistry> registry = new ThreadLocal<>();
    private final ThreadLocal<InheritFromParentAuthorizationStrategy> inheritStrategy = new ThreadLocal<>();

    @BeforeEach
    public void setUp() {
        EncryptionService encryptionServiceMock = mock(EncryptionService.class);
        FolderService folderServiceMock = mock(FolderService.class);
        InheritFromParentAuthorizationStrategy inherit = new InheritFromParentAuthorizationStrategy(encryptionServiceMock);
        inherit.setFolderService(folderServiceMock);
        OAuth2RequestAuthorizationStrategy oAuth2AuthorizationStrategy =
                new OAuth2RequestAuthorizationStrategy(mock(RestTemplateService.class), encryptionServiceMock,
                        new MapperConfiguration().modelMapper());
        BearerAuthorizationStrategy bearerAuthorizationStrategy = new BearerAuthorizationStrategy(encryptionServiceMock);
        BasicRequestAuthorizationStrategy basicAuthorizationStrategy = new BasicRequestAuthorizationStrategy(encryptionServiceMock);
        RequestAuthorizationRegistry requestAuthorizationRegistry = new RequestAuthorizationRegistry(Arrays.asList(oAuth2AuthorizationStrategy,
                bearerAuthorizationStrategy, basicAuthorizationStrategy, inherit));
        requestAuthorizationRegistry.lookupStrategies();
        inherit.setRequestAuthorizationRegistry(requestAuthorizationRegistry);
        encryptionService.set(encryptionServiceMock);
        folderService.set(folderServiceMock);
        registry.set(requestAuthorizationRegistry);
        inheritStrategy.set(inherit);

        TemplateResolverService restTemplateService = mock(TemplateResolverService.class);
        AuthorizationUtils.setObjectMapper(new ObjectMapper());
        AuthorizationUtils.setTemplateResolverService(restTemplateService);
        AuthorizationUtils.setModelMapper(new MapperConfiguration().modelMapper());
    }

    @Test
    public void getAuthorizationToken_noAuthorizationFolderId_noAuth() throws AtpDecryptException {
        // given
        InheritFromParentAuthorizationSaveRequest inheritAuth = new InheritFromParentAuthorizationSaveRequest();
        inheritAuth.setType(RequestAuthorizationType.INHERIT_FROM_PARENT);
        Evaluator evaluator = mock(Evaluator.class);
        AuthorizationStrategyRequest request = new AuthorizationStrategyRequest(inheritAuth, inheritAuth,
                null, UUID.randomUUID(), UUID.randomUUID(), evaluator, null);

        // when
        AuthorizationStrategyResponse auth = inheritStrategy.get().getAuthorizationToken(request);

        // then
        assertNull(auth);
    }

    @Test
    public void getAuthorizationToken_authorizationFolderIdSpecified() throws AtpDecryptException {
        // given
        InheritFromParentAuthorizationSaveRequest inheritAuth = new InheritFromParentAuthorizationSaveRequest();
        inheritAuth.setType(RequestAuthorizationType.INHERIT_FROM_PARENT);
        UUID authFolderId = UUID.randomUUID();
        inheritAuth.setAuthorizationFolderId(authFolderId);
        HttpRequestEntitySaveRequest requestWithInheritAuth = generateRandomHttpRequestEntitySaveRequest();
        requestWithInheritAuth.setAuthorization(inheritAuth);
        Folder authFolder = generateFolder("test", UUID.randomUUID());
        BearerRequestAuthorization bearerAuth = new BearerRequestAuthorization();
        bearerAuth.setToken("token");
        bearerAuth.setType(RequestAuthorizationType.BEARER);
        authFolder.setAuthorization(bearerAuth);

        Evaluator evaluator = mock(Evaluator.class);
        AuthorizationStrategyRequest request = new AuthorizationStrategyRequest(inheritAuth, inheritAuth,
                null, UUID.randomUUID(), UUID.randomUUID(), evaluator, null);

        // when
        when(folderService.get().getFolder(authFolderId)).thenReturn(authFolder);
        AuthorizationStrategyResponse auth = inheritStrategy.get().getAuthorizationToken(request);

        // then
        assertEquals("Bearer token", auth.getUnsafeAuthorizationToken());
    }

    @Test
    public void generateAuthHeader_parentBasicAuthorization() {
        // given
        BasicRequestAuthorization basicAuth = EntitiesGenerator.generateBasicRequestAuthorization();
        Folder parentFolder = EntitiesGenerator.generateFolder("parent", UUID.randomUUID(), null);
        parentFolder.setAuthorization(basicAuth);
        UUID parentFolderId = parentFolder.getId();
        InheritFromParentRequestAuthorization inheritAuth =
                EntitiesGenerator.generateInheritFromParentRequestAuth(parentFolderId);

        // when
        when(folderService.get().getFolder(parentFolderId)).thenReturn(parentFolder);
        RequestHeader authHeader = inheritStrategy.get().generateAuthorizationHeader(inheritAuth);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("Basic dXNlcm5hbWU6cGFzc3dvcmQ=", authHeader.getValue());
    }

    @Test
    public void generateAuthHeader_parentBearerAuthorization() {
        // given
        BearerRequestAuthorization bearerAuth = EntitiesGenerator.generateBearerRequestAuthorization("token");
        Folder parentFolder = EntitiesGenerator.generateFolder("parent", UUID.randomUUID(), null);
        parentFolder.setAuthorization(bearerAuth);
        UUID parentFolderId = parentFolder.getId();
        InheritFromParentRequestAuthorization inheritAuth =
                EntitiesGenerator.generateInheritFromParentRequestAuth(parentFolderId);

        // when
        when(folderService.get().getFolder(parentFolderId)).thenReturn(parentFolder);
        RequestHeader authHeader = inheritStrategy.get().generateAuthorizationHeader(inheritAuth);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("Bearer token", authHeader.getValue());
    }

    @Test
    public void generateAuthHeader_parentOAuth2Authorization_passwordCredentialsGrantType() {
        // given
        OAuth2RequestAuthorization OAuth2WithPasswordCredentials =
                EntitiesGenerator.generateOAuth2RequestAuthorization(OAuth2GrantType.PASSWORD_CREDENTIALS);
        Folder parentFolder = EntitiesGenerator.generateFolder("parent", UUID.randomUUID(), null);
        parentFolder.setAuthorization(OAuth2WithPasswordCredentials);
        UUID parentFolderId = parentFolder.getId();
        InheritFromParentRequestAuthorization inheritAuth =
                EntitiesGenerator.generateInheritFromParentRequestAuth(parentFolderId);

        // when
        when(folderService.get().getFolder(parentFolderId)).thenReturn(parentFolder);
        RequestHeader authHeader = inheritStrategy.get().generateAuthorizationHeader(inheritAuth);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("<calculated when request is sent>", authHeader.getValue());
    }

    @Test
    public void generateAuthHeader_parentOAuth2Authorization_clientCredentialsGrantType() {
        // given
        OAuth2RequestAuthorization OAuth2WithClientCredentials =
                EntitiesGenerator.generateOAuth2RequestAuthorization(OAuth2GrantType.CLIENT_CREDENTIALS);
        Folder parentFolder = EntitiesGenerator.generateFolder("parent", UUID.randomUUID(), null);
        parentFolder.setAuthorization(OAuth2WithClientCredentials);
        UUID parentFolderId = parentFolder.getId();
        InheritFromParentRequestAuthorization inheritAuth =
                EntitiesGenerator.generateInheritFromParentRequestAuth(parentFolderId);

        // when
        when(folderService.get().getFolder(parentFolderId)).thenReturn(parentFolder);
        RequestHeader authHeader = inheritStrategy.get().generateAuthorizationHeader(inheritAuth);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("<calculated when request is sent>", authHeader.getValue());
    }

    @Test
    public void generateAuthHeader_parentOAuth2Authorization_AuthorizationCodeGrantType() {
        // given
        OAuth2RequestAuthorization OAuth2WithAuthorizationCode =
                EntitiesGenerator.generateOAuth2RequestAuthorization(OAuth2GrantType.AUTHORIZATION_CODE);
        Folder parentFolder = EntitiesGenerator.generateFolder("parent", UUID.randomUUID(), null);
        parentFolder.setAuthorization(OAuth2WithAuthorizationCode);
        UUID parentFolderId = parentFolder.getId();
        InheritFromParentRequestAuthorization inheritAuth =
                EntitiesGenerator.generateInheritFromParentRequestAuth(parentFolderId);

        // when
        when(folderService.get().getFolder(parentFolderId)).thenReturn(parentFolder);
        RequestHeader authHeader = inheritStrategy.get().generateAuthorizationHeader(inheritAuth);

        // then
        Assertions.assertNotNull(authHeader);
        Assertions.assertEquals("token", authHeader.getValue());
    }

    @Test
    public void generateAuthHeader_parentHasNotAuthType_authHeaderIsNull() {
        // given
        Folder parentFolder = EntitiesGenerator.generateFolder("parent", UUID.randomUUID(), null);
        UUID parentFolderId = parentFolder.getId();
        InheritFromParentRequestAuthorization inheritAuth =
                EntitiesGenerator.generateInheritFromParentRequestAuth(parentFolderId);

        // when
        when(folderService.get().getFolder(parentFolderId)).thenReturn(parentFolder);
        RequestHeader authHeader = inheritStrategy.get().generateAuthorizationHeader(inheritAuth);

        // then
        Assertions.assertNull(authHeader);
    }

    @Test
    public void generateAuthHeader_parentNotSet_authHeaderIsNull() {
        // given
        InheritFromParentRequestAuthorization inheritAuth =
                EntitiesGenerator.generateInheritFromParentRequestAuth(null);

        // when
        RequestHeader authHeader = inheritStrategy.get().generateAuthorizationHeader(inheritAuth);

        // then
        Assertions.assertNull(authHeader);
    }
}
