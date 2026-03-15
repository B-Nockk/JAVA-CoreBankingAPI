// user-module/src/main/java/com/coreledger/user/domain/model/DocumentFileType.java
package com.coreledger.user.domain.model;

public enum DocumentFileType {
    PDF("application/pdf"),
    JPEG("image/jpeg"),
    PNG("image/png");

    private final String mimeType;

    DocumentFileType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String mimeType() {
        return mimeType;
    }

    public static DocumentFileType fromFilename(String filename) {
        if (filename.endsWith(".pdf"))
            return PDF;
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg"))
            return JPEG;
        if (filename.endsWith(".png"))
            return PNG;
        throw new IllegalArgumentException("Unsupported file type: " + filename);
    }
}
