// src/main/java/com/nairacore/corebankingapi/common/config/BankingProperties.java
package com.nairacore.corebankingapi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.math.BigDecimal;

@ConfigurationProperties(prefix = "app.banking")
public record BankingProperties(BigDecimal minimumBalance) {
}