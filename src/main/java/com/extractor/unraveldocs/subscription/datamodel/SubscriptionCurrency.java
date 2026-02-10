package com.extractor.unraveldocs.subscription.datamodel;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum SubscriptionCurrency {
    USD("$", "United States Dollar", "USD"),
    EUR("€", "Euro", "EUR"),
    GBP("£", "British Pound Sterling", "GBP"),
    INR("₹", "Indian Rupee", "INR"),
    JPY("¥", "Japanese Yen", "JPY"),
    AUD("A$", "Australian Dollar", "AUD"),
    CAD("C$", "Canadian Dollar", "CAD"),
    CNY("¥", "Chinese Yuan Renminbi", "CNY"),
    RUB("₽", "Russian Ruble", "RUB"),
    BRL("R$", "Brazilian Real", "BRL"),
    NGN("₦", "Nigerian Naira", "NGN"),
    ZAR("R", "South African Rand", "ZAR"),
    MXN("$", "Mexican Peso", "MXN"),
    KRW("₩", "South Korean Won", "KRW"),
    CHF("CHF", "Swiss Franc", "CHF"),
    SEK("kr", "Swedish Krona", "SEK"),
    NZD("$", "New Zealand Dollar", "NZD"),
    AED("د.إ", "United Arab Emirates Dirham", "AED"),
    SGD("$", "Singapore Dollar", "SGD"),
    HKD("$", "Hong Kong Dollar", "HKD"),
    TRY("₺", "Turkish Lira", "TRY"),
    PLN("zł", "Polish Zloty", "PLN"),
    NOK("kr", "Norwegian Krone", "NOK"),
    DKK("kr", "Danish Krone", "DKK"),
    THB("฿", "Thai Baht", "THB"),
    IDR("Rp",  "Indonesian Rupiah", "IDR"),
    MYR("RM", "Malaysian Ringgit", "MYR"),
    PHP("₱", "Philippine Peso", "PHP"),
    VND("₫", "Vietnamese Dong", "VND"),
    ARS("$", "Argentine Peso", "ARS"),
    CLP("$", "Chilean Peso", "CLP"),
    COP("$", "Colombian Peso", "COP"),
    PEN("S/", "Peruvian Sol", "PEN"),
    ILS("₪", "Israeli New Shekel", "ILS"),
    KZT("₸", "Kazakhstani Tenge", "KZT"),
    UAH("₴", "Ukrainian Hryvnia", "UAH"),
    RON("lei", "Romanian Leu", "RON"),
    HUF("Ft", "Hungarian Forint", "HUF"),
    CZK("Kč", "Czech Koruna", "CZK"),
    BGN("лв.", "Bulgarian Lev", "BGN"),
    HRK("kn", "Croatian Kuna", "HRK"),
    ISK("kr", "Icelandic Króna", "ISK"),
    LTL("Lt", "Lithuanian Litas", "LTL"),
    LVL("Ls", "Latvian Lats", "LVL"),
    EGP("ج.م", "Egyptian Pound", "EGP"),
    PKR("₨", "Pakistani Rupee", "PKR"),
    TWD("NT$", "New Taiwan Dollar", "TWD"),
    ZMW("ZK", "Zambian Kwacha", "ZMW"),
    KES("KSh", "Kenyan Shilling", "KES"),
    GHS("GH₵", "Ghanaian Cedi", "GHS"),
    MAD("د.م.", "Moroccan Dirham", "MAD"),
    JMD("J$", "Jamaican Dollar", "JMD"),
    BDT("৳", "Bangladeshi Taka", "BDT"),
    LKR("Rs", "Sri Lankan Rupee", "LKR"),
    MUR("₨", "Mauritian Rupee", "MUR"),
    TND("د.ت", "Tunisian Dinar", "TND"),
    OMR("ر.ع.", "Omani Rial", "OMR"),
    QAR("ر.ق", "Qatari Riyal", "QAR"),
    KWD("د.ك", "Kuwaiti Dinar", "KWD"),
    BHD("د.ب", "Bahraini Dinar", "BHD"),
    JOD("د.أ", "Jordanian Dinar", "JOD");

    private final String fullName;
    private final String symbol;
    private final String code;

    SubscriptionCurrency(String symbol, String fullName, String code) {
        this.symbol = symbol;
        this.fullName = fullName;
        this.code = code;
    }

    public static boolean isValidCurrency(SubscriptionCurrency currency) {
        for (SubscriptionCurrency validCurrency : SubscriptionCurrency.values()) {
            if (validCurrency == currency) {
                return true;
            }
        }
        return false;
    }

    public static String[] getAllValidCurrencies() {
        return Arrays.stream(SubscriptionCurrency.values())
                .map(currency -> currency.getCode() + "(" + currency.getFullName() + ")")
                .toArray(String[]::new);
    }

    public static SubscriptionCurrency fromIdentifier(String identifier) {
        return Arrays.stream(values())
                .filter(currency -> currency.fullName.equalsIgnoreCase(identifier) ||
                        currency.code.equalsIgnoreCase(identifier) ||
                        currency.name().equalsIgnoreCase(identifier))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No currency found for identifier: " + identifier));
    }
}
