package org.qubership.atp.itf.lite.backend.converters.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.qubership.atp.itf.lite.backend.feign.dto.history.FolderHistoryChangeDto;
import org.qubership.atp.itf.lite.backend.feign.dto.history.PermissionDto;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.PermissionEntityConverter;

class FolderVersioningMapperTest {

    @Test
    void mapSpecificFields() {
        FolderVersioningMapper folderVersioningMapper =
                new FolderVersioningMapper(new PermissionEntityConverter(), new ListConverter(), mock(ModelMapper.class));
        UUID permissionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String userName = "user";
        String childFolder = "child folder";
        String childRequest = "child request";
        Folder source = new Folder();
        source.setAutoCookieDisabled(true);
        source.setDisableFollowingRedirect(true);
        source.setDisableSslClientCertificate(true);
        source.setDisableSslCertificateVerification(true);
        source.setChildFolders("[\"" + childFolder + "\"]");
        source.setChildRequests("[\"" + childRequest + "\"]");
        source.setPermissionFolderId(permissionId);
        source.setPermission("{\"enable\":true,\"userAccess\":{\"" + userId + "\":\"" + userName + "\"}}");
        FolderHistoryChangeDto destination = new FolderHistoryChangeDto();

        //action
        folderVersioningMapper.mapSpecificFields(source, destination);

        //check
        assertEquals(source.isAutoCookieDisabled(), destination.getIsAutoCookieDisabled());
        assertEquals(source.isDisableFollowingRedirect(), destination.getDisableFollowingRedirect());
        assertEquals(source.isDisableSslClientCertificate(), destination.getDisableSslClientCertificate());
        assertEquals(source.isDisableSslCertificateVerification(), destination.getDisableSslCertificateVerification());

        assertEquals(new PermissionDto().isEnable(true).userAccess(Arrays.asList(userName)), destination.getPermission());
        assertEquals(Arrays.asList(childFolder), destination.getChildFolders());
        assertEquals(Arrays.asList(childRequest), destination.getRequests());
    }
}
