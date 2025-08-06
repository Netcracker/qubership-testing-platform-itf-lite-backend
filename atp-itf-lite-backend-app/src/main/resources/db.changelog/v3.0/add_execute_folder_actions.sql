INSERT INTO actions(id, name, description, deprecated)
SELECT uuid_generate_v1(),
       'Execute requests folder "folderUuid"',
       'Execute requests folder in ITF Lite using provided uuid',
       false
WHERE NOT EXISTS (
    SELECT 1 FROM actions WHERE name = 'Execute requests folder "folderUuid"'
);

INSERT INTO actions(id, name, description, deprecated)
SELECT uuid_generate_v1(),
       'Execute requests folder by path ("parentFolderName", "childFolderName")',
       'Execute requests folder in ITF Lite using provided path to folder',
       false
WHERE NOT EXISTS (
    SELECT 1 FROM actions WHERE name = 'Execute requests folder by path ("parentFolderName", "childFolderName")'
);
