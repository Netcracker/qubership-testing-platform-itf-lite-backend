package org.qubership.atp.itf.lite.backend.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.CookiesRepository;
import org.qubership.atp.itf.lite.backend.exceptions.cookie.IllegalCookieException;
import org.qubership.atp.itf.lite.backend.feign.service.RamService;
import org.qubership.atp.itf.lite.backend.model.api.request.ImportFromRamRequest;
import org.qubership.atp.itf.lite.backend.model.entities.Cookie;

@ExtendWith(MockitoExtension.class)
public class CookieServiceTest {

    @Mock
    private CookiesRepository cookiesRepository;
    @Mock
    private Provider<UserInfo> userInfoProvider;
    @Mock
    private RamService ramService;
    @InjectMocks
    private CookieService cookieService;

    @Test
    public void getCookieByUserIdAndProjectId_allCookieNotExpire_allReturned() {
        // given
        UUID projectId = UUID.randomUUID();
        List<Cookie> cookies = new ArrayList<>();
        Cookie cookie1 = new Cookie();
        cookie1.setKey("Cookie_1");
        cookie1.setValue("Cookie_1=value; Path=/;");
        cookies.add(cookie1);
        Cookie cookie2 = new Cookie();
        cookie2.setKey("Cookie_2");
        cookie2.setValue("Cookie_2=value; Path=/;");
        cookies.add(cookie2);
        Cookie cookie3 = new Cookie();
        cookie3.setKey("Cookie_3");
        cookie3.setValue("Cookie_3=value; Path=/;");
        cookies.add(cookie3);

        // when
        when(userInfoProvider.get()).thenReturn(new UserInfo());
        when(cookiesRepository.findAllByUserIdAndProjectId(eq(null), eq(projectId))).thenReturn(cookies);
        List<Cookie> returnedCookies = cookieService.getNotExpiredCookiesByUserIdAndProjectId(projectId);

        // then
        Assertions.assertEquals(3, returnedCookies.size());
    }

    @Test
    public void getCookieByUserIdAndProjectId_someCookieExpire_wasRemoved() {
        // given
        UUID projectId = UUID.randomUUID();
        List<Cookie> cookies = new ArrayList<>();
        Cookie cookie1 = new Cookie();
        cookie1.setId(UUID.randomUUID());
        cookie1.setKey("Cookie_1");
        cookie1.setValue("Cookie_1=value; Path=/;");
        cookies.add(cookie1);

        Cookie cookie2 = new Cookie();
        UUID cookie2Id = UUID.randomUUID();
        cookie2.setId(cookie2Id);
        cookie2.setKey("Cookie_2");
        cookie2.setValue("Cookie_2=value; Path=/; Expires=Wen, 01 Mar 2023 13:35:27 GMT;");
        cookies.add(cookie2);

        Cookie cookie3 = new Cookie();
        cookie3.setId(UUID.randomUUID());
        cookie3.setKey("Cookie_3");
        cookie3.setValue("Cookie_3=value; Path=/;");
        cookies.add(cookie3);

        // when
        when(userInfoProvider.get()).thenReturn(new UserInfo());
        when(cookiesRepository.findAllByUserIdAndProjectId(eq(null), eq(projectId))).thenReturn(cookies);
        List<Cookie> returnedCookies = cookieService.getNotExpiredCookiesByUserIdAndProjectId(projectId);

        // then
        verify(cookiesRepository, times(1))
                .removeAllByIdIn(new HashSet<UUID>(){{add(cookie2Id);}});
        Assertions.assertEquals(2, returnedCookies.size());
    }

    @Test
    public void save_allCookieValidAndNotExpire_allSaved() {
        // given
        List<Cookie> cookies = new ArrayList<>();
        Cookie cookie1 = new Cookie();
        cookie1.setKey("Cookie_1");
        cookie1.setValue("Cookie_1=value; Path=/;");
        cookies.add(cookie1);
        Cookie cookie2 = new Cookie();
        cookie2.setKey("Cookie_2");
        cookie2.setValue("Cookie_2=value; Path=/;");
        cookies.add(cookie2);
        Cookie cookie3 = new Cookie();
        cookie3.setKey("Cookie_3");
        cookie3.setValue("Cookie_3=value; Path=/;");
        cookies.add(cookie3);

        // when
        when(cookiesRepository.saveAll(eq(cookies))).thenAnswer(args -> args.getArguments()[0]);
        List<Cookie> savedCookies = cookieService.save(cookies);

        // then
        Assertions.assertEquals(3, savedCookies.size());
    }

    @Test
    public void save_CookieInValid_allNotSaved() {
        // given
        List<Cookie> cookies = new ArrayList<>();
        Cookie cookie1 = new Cookie();
        cookie1.setKey("Cookie_1");
        cookie1.setValue("Cookie_1=value; Path=/;");
        cookies.add(cookie1);
        Cookie cookie2 = new Cookie();
        cookie2.setKey("Cookie_2");
        cookie2.setValue("Cookie_2"); // invalid
        cookies.add(cookie2);
        Cookie cookie3 = new Cookie();
        cookie3.setKey("Cookie_3");
        cookie3.setValue("Cookie_3=value; Path=/;");
        cookies.add(cookie3);

        // when
        Assertions.assertThrows(IllegalCookieException.class, () -> cookieService.save(cookies));

        // then
        verify(cookiesRepository, never()).saveAll(any());
    }

    @Test
    public void save_CookieIsExpired_notSaved() {
        // given
        List<Cookie> cookies = new ArrayList<>();
        Cookie cookie1 = new Cookie();
        cookie1.setKey("Cookie_1");
        cookie1.setValue("Cookie_1=value; Path=/;");
        cookies.add(cookie1);
        Cookie cookie2 = new Cookie();
        cookie2.setKey("Cookie_2");
        cookie2.setValue("Cookie_2=value; Path=/; Expires=Wen, 01 Mar 2023 13:35:27 GMT;"); // expired
        cookies.add(cookie2);
        Cookie cookie3 = new Cookie();
        cookie3.setKey("Cookie_3");
        cookie3.setValue("Cookie_3=value; Path=/;");
        cookies.add(cookie3);

        // when
        cookieService.save(cookies);
        ArgumentCaptor<List<Cookie>> cookiesCaptor = ArgumentCaptor.forClass(List.class);
        verify(cookiesRepository).saveAll(cookiesCaptor.capture());

        // then
        List<Cookie> savedCookies = cookiesCaptor.getValue();
        Assertions.assertEquals(2, savedCookies.size());
    }

    @Test
    public void importCookiesFromRam_noAlreadySavedCookies() {
        // given
        UUID projectId = UUID.randomUUID();
        ImportFromRamRequest importRequest = new ImportFromRamRequest();
        List<Cookie> importedCookies = new ArrayList<>();

        Cookie c1 = new Cookie();
        c1.setKey("Cookie_1");
        c1.setValue("Cookie_1=value");
        c1.setDomain("test");
        importedCookies.add(c1);

        Cookie c2 = new Cookie();
        c2.setKey("Cookie_2");
        c2.setValue("Cookie_2=value");
        c2.setDomain("test");
        importedCookies.add(c2);

        // when
        when(userInfoProvider.get()).thenReturn(new UserInfo());
        when(ramService.importCookies(eq(importRequest))).thenReturn(importedCookies);
        when(cookiesRepository.findAllByUserIdAndProjectId(eq(null), eq(projectId)))
                .thenReturn(new ArrayList<>());

        List<Cookie> savedCookies = cookieService.importCookiesFromRam(projectId, importRequest);
        Assertions.assertEquals(2, savedCookies.size());
    }

    @Test
    public void importCookiesFromRam_nameAndDomainNotUniq_shouldBeReplaced() {
        // given
        UUID projectId = UUID.randomUUID();
        ImportFromRamRequest importRequest = new ImportFromRamRequest();
        List<Cookie> importedCookies = new ArrayList<>();

        Cookie c1 = new Cookie();
        c1.setKey("Cookie_1");
        c1.setValue("Cookie_1=value");
        c1.setDomain("test");
        importedCookies.add(c1);

        Cookie c2 = new Cookie();
        c2.setKey("Cookie_2");
        c2.setValue("Cookie_2=value");
        c2.setDomain("test");
        importedCookies.add(c2);

        List<Cookie> savedCookie = new ArrayList<>();

        Cookie c3 = new Cookie();
        c3.setKey("Cookie_1");
        c3.setValue("Cookie_1=value2");
        c3.setDomain("test");
        savedCookie.add(c3);

        Cookie c4 = new Cookie();
        c4.setKey("Cookie_2");
        c4.setValue("Cookie_2=value2");
        c4.setDomain("another_domain");
        savedCookie.add(c4);

        // when
        when(userInfoProvider.get()).thenReturn(new UserInfo());
        when(ramService.importCookies(eq(importRequest))).thenReturn(importedCookies);
        when(cookiesRepository.findAllByUserIdAndProjectId(eq(null), eq(projectId))).thenReturn(savedCookie);

        List<Cookie> savedCookies = cookieService.importCookiesFromRam(projectId, importRequest);
        Assertions.assertEquals(3, savedCookies.size());
    }

    @Test
    public void testGetAllByExecutionRequestIdAndTestRunId_MethodCallCheck_ShouldFindCookiesByErIdAndTrId() {
        UUID executionRequestId = UUID.randomUUID();
        UUID testRunId = UUID.randomUUID();

        cookieService.getAllByExecutionRequestIdAndTestRunId(executionRequestId, testRunId);

        verify(cookiesRepository)
                .findAllByExecutionRequestIdAndTestRunIdOrTestRunIdIsNull(eq(executionRequestId), eq(testRunId));
    }
 }
