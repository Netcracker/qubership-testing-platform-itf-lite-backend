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

import static org.qubership.atp.itf.lite.backend.utils.FileUtils.deleteDirectoryRecursively;

import java.io.IOException;

import org.qubership.atp.common.lock.LockManager;
import org.qubership.atp.itf.lite.backend.service.GridFsService;
import org.qubership.atp.itf.lite.backend.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class DictionariesCleaner {

    @Value("${gridfs.dictionary.remove.days}")
    private Integer fileRemoveDays;

    private final LockManager lockManager;
    private final GridFsService gridFsService;

    @Autowired
    public DictionariesCleaner(LockManager lockManager, GridFsService gridFsService) {
            this.lockManager = lockManager;
            this.gridFsService = gridFsService;
    }

    /**
     * Scheduled task to clean up downloaded zipped dictionaries from file system with lock manager.
     */
    @Scheduled(cron = "0 0/30 * * * *")
    public void dictionariesCleanupWithLockManager() {
        lockManager.executeWithLock("dictionariesCleanup", this::dictionariesCleanup);
    }

    /**
     * Clean up downloaded zipped dictionaries from file system.
     */
    public void dictionariesCleanup() {
        try {
            deleteDirectoryRecursively(Constants.DEFAULT_DICTIONARIES_FOLDER);
        } catch (IOException exception) {
            log.error("Can't cleanup dictionaries folder.", exception);
        }
    }

    /**
     * Scheduled task to clean up downloaded zipped dictionaries from file system with lock manager.
     */
    @Scheduled(cron = "0 1 1 * * ?")
    public void gridFsDictionariesCleanupWithLockManager() {
        lockManager.executeWithLock("gridFsDictionariesCleanup", this::gridFsFilesCleanup);
    }

    /**
     * Clean up dictionaries from grid fs.
     */
    public void gridFsFilesCleanup() {
        gridFsService.removeFilesByDate(fileRemoveDays);
    }
}
