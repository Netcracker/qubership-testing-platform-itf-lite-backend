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

import java.nio.file.Path;
import java.nio.file.Paths;

public interface Constants {
    String COPY_POSTFIX = " Copy";
    String TEMPORARY_LINE_SEPARATOR = "<tls>";
    Path DEFAULT_DICTIONARIES_FOLDER = Paths.get("dictionaries");
    Path DEFAULT_BINARY_FILES_FOLDER = Paths.get("binary");
    Path DEFAULT_FORM_DATA_FOLDER = Paths.get("formData");

    String CONTENT_TYPE_HEADER_NAME = "Content-Type";
    String URL_ENCODED_HEADER_VALUE = "application/x-www-form-urlencoded";

    String ZIP_EXTENSION = "zip";
    String JSON_EXTENSION = "json";

    String PROJECT_ID_HEADER_NAME = "X-Project-Id";
    // Postman collection tokens
    String INFO = "info";
    String ITEM = "item";
    String NAME = "name";
    String REQUEST = "request";
    String METHOD = "method";
    String URL = "url";
    String HEADER = "header";
    String PARAM = "query";
    String BODY = "body";
    String KEY = "key";
    String VALUE = "value";
    String DESCRIPTION = "description";
    String RAW = "raw";
    String MODE = "mode";
    String OPTIONS = "options";
    String LANGUAGE = "language";
    String URLENCODED = "urlencoded";
    String FORMDATA = "formdata";
    String SRC = "src";
    String CONTENT_TYPE = "contentType";
    String DISABLED = "disabled";
    String GRAPHQL = "graphql";
    String QUERY = "query";
    String VARIABLES = "variables";

    String EVENT = "event";
    String LISTEN = "listen";
    String SCRIPT = "script";
    String EXEC = "exec";
    // Postman collection tokens value
    String PREREQUEST = "prerequest";
    String TEST = "test";
    // Postman collections auth tokens
    String AUTH = "auth";
    String TYPE = "type";
    String GRANT_TYPE = "grant_type";
    String SCOPE = "scope";
    String PASSWORD = "password";
    String USERNAME = "username";
    String CLIENT_SECRET = "clientSecret";
    String CLIENT_ID = "clientId";
    String ACCESS_TOKEN_URL = "accessTokenUrl";
    String HEADER_PREFIX_CAMEL_CASE = "headerPrefix";
    String HEADER_PREFIX_SNAKE_CASE = "header_prefix";
    String HEADER_PREFIX = "headerPrefix";
    String BEARER = "bearer";
    String TOKEN = "token";

    String DOT_CHARACTER = ".";
    String DOUBLE_QUOTE_CHARACTER = "\"";
    String EMPTY_STRING = "";

    String NESTING_FOLDER_DEPTH = "5";
    String NESTING_REQUEST_DEPTH = "6";

    String ITF_DESTINATION_TEMPLATE = "{\"itfUrl\": \"%s\", \"systemId\": \"%s\", \"operationId\": \"%s\"}";
    String ATP_EXPORT_FINISHED_TEMPLATE = "Export to %s process is finished. Destination = %s";
    String SSE_EMITTER_EXPIRED = "SSE emitter is expired. Please establish new connection.";
    String SSE_EMITTER_WITH_SSE_ID_NOT_FOUND = "Sse emitter with sseId = {} not found.";

    String STARS = "**********";

    // directory names for export-import
    String FOLDERS = "Folders";
    String REQUESTS = "Requests";

    String COLLECTION = "collection";

    String ATP_ITF_LITE_ROOT_REQUESTS = "ATP_ITF_LITE_ROOT_REQUESTS";
    String FILES = "Files";
    // metric names
    String REQUEST_METRICS_NAME = "atp.requests.duration";
    String PROJECT_ID_LABEL_NAME = "project_id";
    String REQUEST_TYPE_LABEL_NAME = "type";

    String ENV_ID_KEY = "ENV_ID";
    String EXECUTOR_NAME_ITF_LITE = "EXECUTOR_NAME_ITF_LITE";
    String EXECUTION_REQUEST_NAME_ITF_LITE = "EXECUTION_REQUEST_NAME_ITF_LITE";

    String COOKIE_HEADER_KEY = "Cookie";
    String COOKIE_RESP_HEADER_KEY = "Set-Cookie";
    String DOMAIN_KEY = "Domain";
    String PATH_KEY = "Path";
    String EXPIRES_KEY = "Expires";
    String SECURE_KEY = "Secure";
    String HTTP_ONLY_KEY = "HttpOnly";

}
