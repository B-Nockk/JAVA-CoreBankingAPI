// user-module/src/main/java/com/coreledger/user/domain/model/ss.java
package com.coreledger.user.domain.model;

public record StoragePath(String value, StorageBackend backend) {
}