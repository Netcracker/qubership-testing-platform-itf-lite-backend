package org.qubership.atp.itf.lite.backend.service;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateFolder;
import static org.qubership.atp.itf.lite.backend.mocks.EntitiesGenerator.generateHttpRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.opentest4j.AssertionFailedError;
import org.qubership.atp.auth.springbootstarter.entities.UserInfo;
import org.qubership.atp.auth.springbootstarter.security.permissions.PolicyEnforcement;
import org.qubership.atp.auth.springbootstarter.services.UsersService;
import org.qubership.atp.auth.springbootstarter.ssl.Provider;
import org.qubership.atp.itf.lite.backend.configuration.MapperConfiguration;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.FolderRepository;
import org.qubership.atp.itf.lite.backend.dataaccess.repository.RequestRepository;
import org.qubership.atp.itf.lite.backend.enums.EntityType;
import org.qubership.atp.itf.lite.backend.model.api.request.FolderTreeSearchRequest;
import org.qubership.atp.itf.lite.backend.model.api.response.GroupResponse;
import org.qubership.atp.itf.lite.backend.model.entities.Folder;
import org.qubership.atp.itf.lite.backend.model.entities.Request;
import org.qubership.atp.itf.lite.backend.model.entities.converters.ListConverter;
import org.qubership.atp.itf.lite.backend.model.entities.converters.PermissionEntityConverter;
import org.qubership.atp.itf.lite.backend.service.history.iface.DeleteHistoryService;
import org.qubership.atp.itf.lite.backend.utils.StreamUtils;

@ExtendWith(MockitoExtension.class)
public class FolderServiceRequestTreeBuildTest {

    private FolderRepository repository;
    private RequestRepository requestRepository;
    private UsersService usersService;
    private PolicyEnforcement policyEnforcement;
    private Provider<UserInfo> userInfoProvider;
    private RequestAuthorizationService requestAuthorizationService;
    private FolderSpecificationService folderSpecificationService;
    private DeleteHistoryService deleteHistoryService;
    private FolderService folderService;

    private static final ModelMapper modelMapper = new MapperConfiguration().modelMapper();;
    private static UUID projectId;
    private static Folder uat, uat01, uat02, uat02_backup, uat02_backup_copy, env, tmp, Test01, test, TEst_QA, prod, svt,
                   test_samples, test_empty, empty;
    private static Request Test_1, Request_6, test1, Some_Request, Some_Request_1, Some_Request_2, test2, Request_5, Request_4,
                    Test_Request, Request_2, Request_3, test_postman, performance_test, a1, a1_Copy, Request_1, Test_Request_1;
    private static List<Folder> projectFolders;
    private static List<Request> projectRequests;

    /*
     * Initial tree:                           Type (F - folder, R - request)     After search by contains "test":
     *
     * |-- uat                                 [F]                                |-- uat
     * |   |--- uat01                          [F]                                |   |--- uat01
     * |        |--- Test 1                    [R]                                |        |--- Test 1
     * |        |--- Request 6                 [R]                                |   |--- uat02
     * |   |--- uat02                          [F]                                |        |--- uat02_backup
     * |        |--- uat02_backup              [F]                                |             |--- test 1
     * |             |--- test 1               [R]                                |        |--- env
     * |             |--- Some request         [R]                                |             |--- tmp
     * |        |--- uat02_backup_copy         [F]                                |                  |--- test 2
     * |             |--- Some request 1       [R]                                |
     * |        |--- env                       [F]                                |-- Test01
     * |             |--- tmp                  [F]                                |    |--- Test Request
     * |                  |--- Some request 2  [R]                                |
     * |                  |--- test 2          [R]                                |-- test
     * |        |--- Request 5                 [R]                                |   |--- TEst_QA
     * |   |--- Request 4                      [R]                                |        |--- Request 3
     * |                                                                          |-- prod
     * |-- Test01                              [F]                                |   |--- test postman
     * |    |--- Test Request                  [R]                                |
     * |    |--- Request 2                     [R]                                |-- test samples
     * |                                                                          |   |--- a1
     * |-- test                                [F]                                |   |--- a1 Copy
     * |   |--- TEst_QA                        [F]                                |
     * |        |--- Request 3                 [R]                                |-- test_empty
     * |                                                                          |
     * |-- prod                                [F]                                |-- Test Request 1
     * |   |--- test postman                   [R]
     * |
     * |-- svt                                 [F]
     * |   |--- performance_request            [R]
     * |
     * |-- test samples                        [F]
     * |   |--- a1                             [R]
     * |   |--- a1 Copy                        [R]
     * |
     * |-- test_empty                          [F]
     * |
     * |-- empty                               [F]
     * |
     * |-- Request 1                           [R]
     * |
     * |-- Test Request 1                      [R]
     */
    @BeforeAll
    public static void init() {
        projectId = UUID.randomUUID();

        uat = generateFolder("uat", projectId);
        uat01 = generateFolder("uat01", projectId, uat.getId());
        uat02 = generateFolder("uat02", projectId, uat.getId());
        uat02_backup = generateFolder("uat02_backup", projectId, uat02.getId());
        uat02_backup_copy = generateFolder("uat02_backup_copy", projectId, uat02.getId());
        env = generateFolder("env", projectId, uat02.getId());
        tmp = generateFolder("tmp", projectId, env.getId());
        Test01 = generateFolder("Test01", projectId);
        test = generateFolder("test", projectId);
        TEst_QA = generateFolder("TEst_QA", projectId, test.getId());
        prod = generateFolder("prod", projectId);
        svt = generateFolder("svt", projectId);
        test_samples = generateFolder("test samples", projectId);
        test_empty = generateFolder("test_empty", projectId);
        empty = generateFolder("empty", projectId);

        Test_1 = generateHttpRequest("Test  1", projectId, uat01.getId());
        Request_6 = generateHttpRequest("Request 6", projectId, uat01.getId());
        test1 = generateHttpRequest("test 1", projectId, uat02_backup.getId());
        Some_Request = generateHttpRequest("Some request", projectId, uat02_backup.getId());
        Some_Request_1 = generateHttpRequest("Some request 1", projectId, uat02_backup_copy.getId());
        Some_Request_2 = generateHttpRequest("Some request 2", projectId, tmp.getId());
        test2 = generateHttpRequest("test 2", projectId, tmp.getId());
        Request_5 = generateHttpRequest("Request 5", projectId, uat02.getId());
        Request_4 = generateHttpRequest("Request 4", projectId, uat.getId());
        Test_Request = generateHttpRequest("Test Request", projectId, Test01.getId());
        Request_2 = generateHttpRequest("Request 2", projectId, Test01.getId());
        Request_3 = generateHttpRequest("Request 3", projectId, TEst_QA.getId());
        test_postman = generateHttpRequest("test postman", projectId, prod.getId());
        performance_test = generateHttpRequest("performance_request", projectId, svt.getId());
        a1 = generateHttpRequest("a1", projectId, test_samples.getId());
        a1_Copy = generateHttpRequest("a1 Copy", projectId, test_samples.getId());
        Request_1 = generateHttpRequest("Request 1", projectId);
        Test_Request_1 = generateHttpRequest("Test Request 1", projectId);

        projectRequests = new ArrayList<>(asList(Test_1, Request_6, test1, Some_Request, Some_Request_1, Some_Request_2, test2, Request_5, Request_4,
                Test_Request, Request_2, Request_3, test_postman, performance_test, a1, a1_Copy, Request_1, Test_Request_1));
        projectFolders = new ArrayList<>(asList(uat, uat01, uat02, uat02_backup, uat02_backup_copy, env, tmp, Test01, test, TEst_QA, prod, svt,
                test_samples, test_empty, empty));
    }

    @BeforeEach
    public void setUp() {
        repository = mock(FolderRepository.class);
        requestRepository = mock(RequestRepository.class);
        usersService = mock(UsersService.class);
        policyEnforcement = mock(PolicyEnforcement.class);
        userInfoProvider = mock(Provider.class);
        requestAuthorizationService = mock(RequestAuthorizationService.class);
        folderSpecificationService = mock(FolderSpecificationService.class);
        deleteHistoryService = mock(DeleteHistoryService.class);
        folderService = new FolderService(repository, requestRepository, modelMapper, usersService, policyEnforcement,
                userInfoProvider, requestAuthorizationService, folderSpecificationService, deleteHistoryService,
                new PermissionEntityConverter(), new ListConverter());
    }

    @Test
    public void getFolderRequestsTree_whenOnlyProjectIdSpecified_shouldTreeResponse() {
        // given
        FolderTreeSearchRequest request = new FolderTreeSearchRequest();
        request.setProjectId(projectId);

        //when
        when(requestRepository.findAllByProjectId(projectId)).thenReturn(projectRequests);
        when(repository.findAllByProjectId(projectId)).thenReturn(projectFolders);

        //then
        GroupResponse response = folderService.getFolderRequestsTree(false, request);
        Collection<GroupResponse> groupResponses = response.getChildren();

        assertNotNull(groupResponses, "Response shouldn't be null");

        assertContainsFolders(groupResponses, uat, Test01, test, prod, svt, test_samples, test_empty, empty);
        assertContainsRequests(groupResponses, Request_1, Test_Request_1);

        Collection<GroupResponse> uatChildren = findFolderChildrenResponses(groupResponses, uat);
        assertContainsFolders(uatChildren, uat01, uat02);
        assertContainsRequests(uatChildren, Request_4);

        Collection<GroupResponse> uat01Children = findFolderChildrenResponses(uatChildren, uat01);
        assertContainsRequests(uat01Children, Test_1, Request_6);

        Collection<GroupResponse> uat02Children = findFolderChildrenResponses(uatChildren, uat02);
        assertContainsFolders(uat02Children, uat02_backup, uat02_backup_copy, env);
        assertContainsRequests(uat02Children, Request_5);

        Collection<GroupResponse> uat02_backupChildren = findFolderChildrenResponses(uat02Children, uat02_backup);
        assertContainsRequests(uat02_backupChildren, test1, Some_Request);

        Collection<GroupResponse> uat02_backup_copyChildren = findFolderChildrenResponses(uat02Children, uat02_backup_copy);
        assertContainsRequests(uat02_backup_copyChildren, Some_Request_1);

        Collection<GroupResponse> envChildren = findFolderChildrenResponses(uat02Children, env);
        assertContainsFolders(envChildren, tmp);

        Collection<GroupResponse> tmpChildren = findFolderChildrenResponses(envChildren, tmp);
        assertContainsRequests(tmpChildren, Some_Request_2, test2);

        Collection<GroupResponse> Test01Children = findFolderChildrenResponses(groupResponses, Test01);
        assertContainsRequests(Test01Children, Test_Request, Request_2);

        Collection<GroupResponse> testChildren = findFolderChildrenResponses(groupResponses, test);
        assertContainsFolders(testChildren, TEst_QA);
        Collection<GroupResponse> TEst_QAChildren = findFolderChildrenResponses(testChildren, TEst_QA);
        assertContainsRequests(TEst_QAChildren, Request_3);

        Collection<GroupResponse> prodChildren = findFolderChildrenResponses(groupResponses, prod);
        assertContainsRequests(prodChildren, test_postman);

        Collection<GroupResponse> svtChildren = findFolderChildrenResponses(groupResponses, svt);
        assertContainsRequests(svtChildren, performance_test);

        Collection<GroupResponse> test_samplesChildren = findFolderChildrenResponses(groupResponses, test_samples);
        assertContainsRequests(test_samplesChildren, a1, a1_Copy);
    }

    @Test
    public void getFolderRequestsTree_whenProjectIdAndSearchSpecified_shouldTreeResponse() {
        // given
        String searchString = "test";
        FolderTreeSearchRequest request = new FolderTreeSearchRequest();
        request.setProjectId(projectId);
        request.setSearch(searchString);

        //when
        when(requestRepository.findAllByProjectId(projectId)).thenReturn(projectRequests);
        when(repository.findAllByProjectId(projectId)).thenReturn(projectFolders);

        //then
        GroupResponse response = folderService.getFolderRequestsTree(false, request);
        Collection<GroupResponse> groupResponses = response.getChildren();

        assertNotNull(groupResponses, "Response shouldn't be null");

        assertContainsFolders(groupResponses, uat, Test01, test, prod, test_samples, test_empty);
        assertContainsRequests(groupResponses, Test_Request_1);

        assertNotContainsFolder(groupResponses, svt);
        assertNotContainsFolder(groupResponses, empty);
        assertNotContainsRequest(groupResponses, Request_1);

        Collection<GroupResponse> uatChildren = findFolderChildrenResponses(groupResponses, uat);
        assertContainsFolders(uatChildren, uat01, uat02);
        assertNotContainsRequest(uatChildren, Request_4);

        Collection<GroupResponse> uat01Children = findFolderChildrenResponses(uatChildren, uat01);
        assertContainsRequests(uat01Children, Test_1);
        assertNotContainsRequest(uat01Children, Request_6);

        Collection<GroupResponse> uat02Children = findFolderChildrenResponses(uatChildren, uat02);
        assertContainsFolders(uat02Children, uat02_backup, env);
        assertNotContainsFolder(uat02Children, uat02_backup_copy);
        assertNotContainsRequest(uat02Children, Request_5);

        Collection<GroupResponse> uat02_backupChildren = findFolderChildrenResponses(uat02Children, uat02_backup);
        assertContainsRequests(uat02_backupChildren, test1);
        assertNotContainsRequest(uat01Children, Some_Request);

        Collection<GroupResponse> envChildren = findFolderChildrenResponses(uat02Children, env);
        assertContainsFolders(envChildren, tmp);

        Collection<GroupResponse> tmpChildren = findFolderChildrenResponses(envChildren, tmp);
        assertContainsRequests(tmpChildren, test2);
        assertNotContainsRequest(tmpChildren, Some_Request_2);

        Collection<GroupResponse> Test01Children = findFolderChildrenResponses(groupResponses, Test01);
        assertContainsRequests(Test01Children, Test_Request);
        assertNotContainsRequest(Test01Children, Request_2);

        Collection<GroupResponse> testChildren = findFolderChildrenResponses(groupResponses, test);
        assertContainsFolders(testChildren, TEst_QA);
        Collection<GroupResponse> TEst_QAChildren = findFolderChildrenResponses(testChildren, TEst_QA);
        assertContainsRequests(TEst_QAChildren, Request_3);

        Collection<GroupResponse> prodChildren = findFolderChildrenResponses(groupResponses, prod);
        assertContainsRequests(prodChildren, test_postman);

        Collection<GroupResponse> test_samplesChildren = findFolderChildrenResponses(groupResponses, test_samples);
        assertContainsRequests(test_samplesChildren, a1, a1_Copy);
    }

    @Test
    public void getFolderRequestsTree_whenProjectIdSpecifiedAndOnlyFoldersIsTrue_shouldTreeResponse() {
        // given
        FolderTreeSearchRequest request = new FolderTreeSearchRequest();
        request.setProjectId(projectId);

        //when
        when(repository.findAllByProjectId(projectId)).thenReturn(projectFolders);

        //then
        GroupResponse response = folderService.getFolderRequestsTree(true, request);
        Collection<GroupResponse> groupResponses = response.getChildren();

        assertNotNull(groupResponses, "Response shouldn't be null");

        assertContainsFolders(groupResponses, uat, Test01, test, prod, svt, test_samples, test_empty, empty);
        assertNotContainAnyRequests(groupResponses);

        Collection<GroupResponse> uatChildren = findFolderChildrenResponses(groupResponses, uat);
        assertContainsFolders(uatChildren, uat01, uat02);
        assertNotContainAnyRequests(uatChildren);

        Collection<GroupResponse> uat01Children = findFolderChildrenResponses(uatChildren, uat01);
        assertNotContainAnyRequests(uat01Children);

        Collection<GroupResponse> uat02Children = findFolderChildrenResponses(uatChildren, uat02);
        assertContainsFolders(uat02Children, uat02_backup, uat02_backup_copy, env);
        assertNotContainAnyRequests(uat02Children);

        Collection<GroupResponse> uat02_backupChildren = findFolderChildrenResponses(uat02Children, uat02_backup);
        assertNotContainAnyRequests(uat02_backupChildren);

        Collection<GroupResponse> uat02_backup_copyChildren = findFolderChildrenResponses(uat02Children, uat02_backup_copy);
        assertNotContainAnyRequests(uat02_backup_copyChildren);

        Collection<GroupResponse> envChildren = findFolderChildrenResponses(uat02Children, env);
        assertContainsFolders(envChildren, tmp);
        assertNotContainAnyRequests(envChildren);

        Collection<GroupResponse> tmpChildren = findFolderChildrenResponses(envChildren, tmp);
        assertNotContainAnyRequests(tmpChildren);

        Collection<GroupResponse> Test01Children = findFolderChildrenResponses(groupResponses, Test01);
        assertNotContainAnyRequests(Test01Children);

        Collection<GroupResponse> testChildren = findFolderChildrenResponses(groupResponses, test);
        assertContainsFolders(testChildren, TEst_QA);
        Collection<GroupResponse> TEst_QAChildren = findFolderChildrenResponses(testChildren, TEst_QA);
        assertNotContainAnyRequests(TEst_QAChildren);

        Collection<GroupResponse> prodChildren = findFolderChildrenResponses(groupResponses, prod);
        assertNotContainAnyRequests(prodChildren);

        Collection<GroupResponse> svtChildren = findFolderChildrenResponses(groupResponses, svt);
        assertNotContainAnyRequests(svtChildren);

        Collection<GroupResponse> test_samplesChildren = findFolderChildrenResponses(groupResponses, test_samples);
        assertNotContainAnyRequests(test_samplesChildren);
    }

    private Collection<GroupResponse> findFolderChildrenResponses(Collection<GroupResponse> responses, Folder folder) {
        return responses.stream()
                .filter(response -> response.getId().equals(folder.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionFailedError("Failed to found response for folder with id: " + folder.getId()))
                .getChildren();
    }

    private static void assertNotContainAnyRequests(Collection<GroupResponse> responses) {
        assertItemsSizeByType(responses, 0, EntityType.REQUEST);
    }

    private static void assertContainsRequests(Collection<GroupResponse> responses, Request... requests) {
        assertItemsSizeByType(responses, requests.length, EntityType.REQUEST);

        Map<UUID, GroupResponse> responseMap = StreamUtils.toEntityMap(responses, GroupResponse::getId);

        Arrays.stream(requests).forEach(request -> {
            GroupResponse response = responseMap.get(request.getId());

            if (response == null) {
                throw new AssertionFailedError("Failed to found response for request with name: " + request.getName());
            }

            assertEquals(request.getName(), response.getName(), "Response and request names didn't match");
            assertEquals(EntityType.REQUEST, response.getType(), "Response and request types didn't match");
        });
    }

    private static void assertNotContainsRequest(Collection<GroupResponse> responses, Request request) {
        Map<UUID, GroupResponse> responseMap = StreamUtils.toEntityMap(responses, GroupResponse::getId);
        assertNull(responseMap.get(request.getId()));
    }

    private static void assertContainsFolders(Collection<GroupResponse> responses, Folder... folders) {
        assertItemsSizeByType(responses, folders.length, EntityType.FOLDER);

        Map<UUID, GroupResponse> responseMap = StreamUtils.toEntityMap(responses, GroupResponse::getId);

        Arrays.stream(folders).forEach(folder -> {
            GroupResponse response = responseMap.get(folder.getId());

            if (response == null) {
                throw new AssertionFailedError("Failed to found response for folder with name: " + folder.getName());
            }

            assertEquals(folder.getName(), response.getName(), "Response and folder names didn't match");
            assertEquals(EntityType.FOLDER, response.getType(), "Response and folder types didn't match");
        });
    }

    private static void assertNotContainsFolder(Collection<GroupResponse> responses, Folder folder) {
        Map<UUID, GroupResponse> responseMap = StreamUtils.toEntityMap(responses, GroupResponse::getId);
        assertNull(responseMap.get(folder.getId()));
    }

    private static void assertItemsSizeByType(Collection<GroupResponse> responses, long expectedSize, EntityType type) {
        long actualCount = responses.stream()
                .filter(response -> type.equals(response.getType()))
                .count();

        assertEquals(expectedSize, actualCount, "Count mismatch by entity type: " + type);
    }
}
