// transfer-module/src/test/java/com/coreledger/transfer/TestTransferModuleApplication.java
package com.coreledger.transfer;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestTransferModuleApplication {
    // exists only to satisfy @DataJpaTest and @WebMvcTest context scan
    // never runs in production
}