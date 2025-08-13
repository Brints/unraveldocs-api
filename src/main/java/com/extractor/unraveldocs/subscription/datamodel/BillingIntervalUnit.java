package com.extractor.unraveldocs.subscription.datamodel;

public enum BillingIntervalUnit {
    MONTH("month"),
    WEEK("week"),
    YEAR("year");

    private final String value;

    BillingIntervalUnit(String value) {
        this.value = value;
    }

    public static BillingIntervalUnit fromValue(String value) {
        for (BillingIntervalUnit unit : BillingIntervalUnit.values()) {
            if (unit.value.equalsIgnoreCase(value)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unknown billing interval unit: " + value);
    }
}
