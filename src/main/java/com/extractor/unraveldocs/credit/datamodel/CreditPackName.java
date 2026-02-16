package com.extractor.unraveldocs.credit.datamodel;

import lombok.Getter;

/**
 * Enum representing available credit pack names.
 */
@Getter
public enum CreditPackName {
    STARTER_PACK("Starter Pack"),
    VALUE_PACK("Value Pack"),
    POWER_PACK("Power Pack");

    private final String displayName;

    CreditPackName(String displayName) {
        this.displayName = displayName;
    }

}
