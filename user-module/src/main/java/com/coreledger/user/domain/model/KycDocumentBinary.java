// user-module/src/main/java/com/coreledger/user/domain/model/kdd.java
package com.coreledger.user.domain.model;

public record KycDocumentBinary(
        KycDocumentId id,
        String filename,
        byte[] content,
        String contentType) {
}
