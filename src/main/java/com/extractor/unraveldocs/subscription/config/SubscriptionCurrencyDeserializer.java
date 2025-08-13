package com.extractor.unraveldocs.subscription.config;

import com.extractor.unraveldocs.subscription.datamodel.SubscriptionCurrency;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

public class SubscriptionCurrencyDeserializer extends JsonDeserializer<SubscriptionCurrency> {

    @Override
    public SubscriptionCurrency deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        try {
            return SubscriptionCurrency.fromIdentifier(value);
        } catch (IllegalArgumentException e) {
            String validValues = String.join(", ", SubscriptionCurrency.getAllValidCurrencies());
            throw new IOException("Invalid currency: '" + value + "'. Valid values are: " + validValues, e);
        }
    }
}