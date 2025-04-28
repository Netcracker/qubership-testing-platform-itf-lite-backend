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

package org.qubership.atp.itf.lite.backend.model.api;

public interface ApiPath {
    String ID = "id";
    String REQUEST_ID = "itfLiteRequestId";

    String ID_PATH_VARIABLE = "{" + ID + "}";

    String REQUEST_ID_PATH_VARIABLE = "{" + REQUEST_ID + "}";

    String REST_DEPLOYMENT = "/rest/deployment";
    String LIVENESS = "liveness";
    String READINESS = "readiness";

    String SERVICE_API_V1_PATH = "/atp-itf-lite/api/v1";

    String CACHE_EVICT_PATH = "/cache/evict";
    String FOLDER_PATH = "/folder";
    String FOLDERS_PATH = "/folders";
    String HISTORY_PATH = "/history";
    String REQUESTS_PATH = "/requests";
    String REQUEST_SNAPSHOT_PATH = "/requestSnapshot";
    String REQUEST_PATH = "/request";
    String REQUEST_HEADERS_PATH = "/request-headers";
    String REQUEST_PARAMS_PATH = "/request-params";
    String SSE_PATH = "/sse";
    String USER_SETTINGS_PATH = "/user-settings";
    String PING_PATH = "/ping";

    String ITF_PATH = "/itf";
    String MIA_PATH = "/mia";

    String ID_PATH = "/" + ID_PATH_VARIABLE;
    String REQUEST_ID_PATH = "/" + REQUEST_ID_PATH_VARIABLE;
    String SETTINGS_PATH = "/settings";

    String ACTIONS_PATH = "/actions";
    String EDIT_PATH = "/edit";
    String CERTIFICATE_PATH = "/certificate";
    String COLLECTION_PATH = "/collections";
    String CONNECT_PATH = "/connect";
    String CONTEXT_PATH = "/context";
    String COPY_PATH = "/copy";
    String COUNT_HEIRS_PATH = "/countHeirs";
    String DICTIONARY_PATH = "/dictionary";
    String BINARY_PATH = "/binary";
    String FILE_PATH = "/file";
    String FILE_ID = "fileId";
    String FILE_ID_PATH = "/{" + FILE_ID + "}";
    String UPLOAD_PATH = "/upload";
    String DOWNLOAD_PATH = "/download";
    String DOWNLOAD_RESPONSE_PATH = "/downloadResponse";
    String DISABLE_PATH = "/disable";
    String ENABLE_PATH = "/enable";
    String ENVIRONMENT_PATH = "/environment";
    String EXECUTE_PATH = "/execute";
    String DOCUMENTATION_PATH = "/documentation";
    String EXECUTORS_PATH = "/executors";
    String EXPORT_PATH = "/export";
    String IMPORT_PATH = "/import";
    String CONTEXT_VARIABLES_PATH = "/contextVariables";
    String MOVE_PATH = "/move";
    String ORDER_PATH = "/order";
    String TREE_PATH = "/tree";
    String REDIRECT_PATH = "/redirect-uri/";
    String RAM_DOWNLOAD_FILE_PATH = "/api/logrecords/file/";

    String FORM_DATA_PATH = "/formdata";
    String FORM_DATA_ID = "formDataPartId";
    String FORM_DATA_ID_PATH = "/{" + FORM_DATA_ID + "}";
    String FILE_UPLOAD_PATH = FORM_DATA_PATH + FORM_DATA_ID_PATH + FILE_PATH + UPLOAD_PATH;
    String FILE_DOWNLOAD_PATH = FILE_PATH + FILE_ID_PATH + DOWNLOAD_PATH;

    String PROJECT_PATH = "/project";
    String PROJECT_ID = "projectId";
    String PROJECT_ID_PATH = "/{" + PROJECT_ID + "}";
    String COOKIES_PATH = "/cookies";
    String SESSION_PATH = "/session";
    String SESSION_ID = "sessionId";
    String SESSION_ID_PATH = "/{" + SESSION_ID + "}";

}
