INSERT INTO actions(id, name, description, deprecated) VALUES
    (uuid_generate_v1(), 'Execute requests folder "folderUuid"', 'Execute requests folder in ITF Lite using provided uuid', false),
    (uuid_generate_v1(), 'Execute requests folder by path ("parentFolderName", "childFolderName")', 'Execute requests folder in ITF Lite using provided path to folder', false);