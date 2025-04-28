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

package org.qubership.atp.itf.lite.backend.schedulers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CachedFilesCleaner {
    private static final List<Path> listOfDirectories = Arrays.asList(Constants.DEFAULT_BINARY_FILES_FOLDER,
            Constants.DEFAULT_FORM_DATA_FOLDER);

    @Value("${atp.itf.lite.clean.file.cache.time-sec}")
    private String cleanFileCacheTimeout;


    @Scheduled(cron = "${atp.itf.lite.clean.file.cache.cron.expression}")
    public void filesCleanUpJob() {
        log.debug("Start cleaning files in directories {}.", listOfDirectories);
        deleteOldFiles(listOfDirectories);
    }

    /**
     * Delete not needed files.
     *
     * @directories list path where
     */
    public void deleteOldFiles(List<Path> directories) {
        List<String> listOfDeletedFiles = new ArrayList<>();
        directories.forEach(directory -> {
            if (Files.exists(directory)) {
                try (Stream<Path> pathStream = Files.walk(directory)) {
                    pathStream
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .filter(file -> file.isFile()
                                    && (System.currentTimeMillis() - file.lastModified())
                                    > Long.parseLong(cleanFileCacheTimeout))
                            .forEach(file -> {
                                try {
                                    if (file.delete()) {
                                        listOfDeletedFiles.add(file.getAbsolutePath());
                                    }
                                } catch (Exception e) {
                                    log.error("Can not delete file {}", file.getPath(), e);
                                }
                            });
                } catch (Exception e) {
                    log.error("Can not delete files from directory {}", directory, e);
                }
            }
        });
        log.debug("Lod of deleted files: {}", listOfDeletedFiles);
    }
}
