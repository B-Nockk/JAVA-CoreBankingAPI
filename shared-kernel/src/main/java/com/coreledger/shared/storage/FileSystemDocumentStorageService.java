// shared-kernel/src/main/java/com/coreledger/shared/storage/fs.java
package com.coreledger.shared.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileSystemDocumentStorageService implements DocumentStorageService {

    private final Path rootDir;

    public FileSystemDocumentStorageService(@Value("${storage.fs.root}") String rootDir) {
        this.rootDir = Paths.get(rootDir);
    }

    @Override
    public String save(byte[] content, String filename, String mimeType) {
        String uniqueName = UUID.randomUUID() + "-" + filename;
        Path path = rootDir.resolve(uniqueName);
        try {
            Files.write(path, content);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save document to filesystem", e);
        }
        return path.toString(); // storagePath = absolute path
    }

    @Override
    public byte[] load(String storagePath) {
        try {
            return Files.readAllBytes(Paths.get(storagePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to load document from filesystem", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(Paths.get(storagePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete document from filesystem", e);
        }
    }
}
