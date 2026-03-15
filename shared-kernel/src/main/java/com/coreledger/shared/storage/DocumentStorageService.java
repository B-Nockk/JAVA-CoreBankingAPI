// user-module/src/main/java/com/coreledger/user/application/service/DocumentStorageService.java
package com.coreledger.shared.storage;

/**
 * Abstraction for storing and retrieving KYC document binaries.
 * Different implementations can target S3, GridFS, filesystem, etc.
 */
public interface DocumentStorageService {

    /**
     * Save a document binary to storage.
     *
     * @param content  the raw file bytes
     * @param filename the original filename
     * @param mimeType the validated MIME type (from enum)
     * @return storage path reference (string or typed object)
     */
    String save(byte[] content, String filename, String mimeType);

    /**
     * Load a document binary from storage.
     *
     * @param storagePath reference returned by save()
     * @return raw file bytes
     */
    byte[] load(String storagePath);

    /**
     * Delete a document from storage.
     *
     * @param storagePath reference returned by save()
     */
    void delete(String storagePath);
}
