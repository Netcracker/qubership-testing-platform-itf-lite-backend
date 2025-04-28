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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.http.entity.ContentType;
import org.apache.tika.Tika;
import org.qubership.atp.itf.lite.backend.model.entities.gridfs.FileData;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FileUtils {
    private static final List<String> TEXT_CONTENT_TYPES = Arrays.asList(ContentType.DEFAULT_TEXT.getMimeType(),
            ContentType.TEXT_PLAIN.getMimeType(), ContentType.TEXT_HTML.getMimeType(),
            ContentType.TEXT_XML.getMimeType(), ContentType.APPLICATION_JSON.getMimeType(),
            ContentType.APPLICATION_ATOM_XML.getMimeType(), ContentType.APPLICATION_XML.getMimeType(),
            ContentType.APPLICATION_XHTML_XML.getMimeType(), ContentType.APPLICATION_SOAP_XML.getMimeType(),
            ContentType.MULTIPART_FORM_DATA.getMimeType());

    /**
     * Deletes directory recursively.
     * @param directory directory
     * @throws IOException IOException
     */
    public static void deleteDirectoryRecursively(Path directory) throws IOException {
        log.debug("Clean all files and directories from {}", directory);
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    /**
     * Prepares directories before dictionary save.
     * @param dictionariesFolder dictionaries folder
     * @param requestId request id
     * @return Created request folder path
     * @throws IOException could be problem with recording to file system
     */
    public static Path prepareDirectoriesBeforeSave(Path dictionariesFolder, UUID requestId) throws IOException {
        Path requestFolderPath = Paths.get(dictionariesFolder.toString(),
                requestId.toString());
        if (Files.exists(requestFolderPath)) {
            deleteDirectoryRecursively(requestFolderPath);
        }
        Files.createDirectories(requestFolderPath);
        return requestFolderPath;
    }

    /**
     * Saves multipart file dictionary to requestId's directory.
     * @param dictionariesFolder dictionaries folder
     * @param requestId request id
     * @param dictionary zipped dictionary
     * @throws IOException could be problem with recording to file system
     */
    public static void saveMultipartFileDictionaryToFileSystem(Path dictionariesFolder,
                                                               UUID requestId,
                                                               MultipartFile dictionary)
            throws IOException {
        Path requestFolderPath = prepareDirectoriesBeforeSave(dictionariesFolder, requestId);
        Path dictionaryFilePath = Paths.get(requestFolderPath.toString(), dictionary.getOriginalFilename());
        dictionary.transferTo(dictionaryFilePath);
    }

    /**
     * Saves multipart file form-data to requestId's directory.
     * @param formDataFolder form-data folder
     * @param requestId request id
     * @param file zipped dictionary
     * @throws IOException could be problem with recording to file system
     */
    public static void saveMultipartFileFormDataToFileSystem(Path formDataFolder, UUID requestId,
                                                               MultipartFile file)
            throws IOException {
        Path requestFolderPath = prepareDirectoriesBeforeSave(formDataFolder, requestId);
        Path filePath = Paths.get(requestFolderPath.toString(), file.getOriginalFilename());
        file.transferTo(filePath);
    }

    /**
     * Saves byte array dictionary to requestId's directory.
     * @param dictionariesFolder dictionaries folder
     * @param requestId request id
     * @param dictionaryFileData zipped dictionary bytes
     * @throws IOException could be problem with recording to file system
     */
    public static Path saveFileDataDictionaryToFileSystem(Path dictionariesFolder, UUID requestId,
                                                          FileData dictionaryFileData)
            throws IOException {
        Path requestFolderPath = prepareDirectoriesBeforeSave(dictionariesFolder, requestId);
        Path dictionaryFilePath = Paths.get(requestFolderPath.toString(), dictionaryFileData.getFileName());
        return Files.write(dictionaryFilePath, dictionaryFileData.getContent());
    }

    /**
     * Add fileName sanitation to avoid Path Traversal vulnerability.
     * @param fileName fileName
     * @return sanitized fileName
     */
    public static String sanitizeFileName(String fileName) {
        Path p = Paths.get(fileName);
        return p.getFileName().toString();
    }

    /**
     * Get Multipart file how string.
     */
    public static Optional<String> readFileToString(MultipartFile file) throws IOException {
        final String contentType = file.getContentType();
        if (contentType != null && TEXT_CONTENT_TYPES.contains(ContentType.parse(contentType).getMimeType())) {
            log.debug("Reading file with name '{}' and content type '{}'", file.getOriginalFilename(), contentType);
            return Optional.of(new String(file.getBytes()));
        }
        return Optional.empty();
    }

    public static ContentType guessContentTypeFromName(String fileName) {
        String mimeType = new Tika().detect(fileName);
        return ContentType.parse(mimeType);
    }
}
