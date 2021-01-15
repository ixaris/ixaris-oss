/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.financial.lib;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * All static information known about currencies used in the system. This <em>does not</em> include ephemeral/volatile information such as FX
 * rates, but <em>does</em> include data such as the ISO 3166 and 4217 codes, decimals to round to for display and storage, and decimals to shift
 * for integer representation
 *
 * <p>For lists of these codes see, for example:
 *
 * <ul>
 *   <li><a href="http:// www.xe.com/iso4217.htm">ISO 4217 Type Currency Symbol List</a>
 *   <li><a href="https://en.wikipedia.org/wiki/ISO_4217">ISO 4217 Currency Symbol List</a>
 * </ul>
 *
 * <p>Immutable record of static information we hold on each currency. The data held here are:
 *
 * <ul>
 *   <li>The alphabetic and numeric ISO currency codes.
 *   <li>The decimals of precision to be used for storage/display
 * </ul>
 *
 * <p>This list includes the ISO 4217 currencies up until Amendment 169, as specified in <a
 * href="https://www.currency-iso.org/en/shared/amendments/iso-4217-amendment.html">ISO 4217 Amendment 169</a>.
 */
public final class CurrencyInfo {
    
    /**
     * Immutable Map (String to CurrencyInfo) of currency code values statically known to the system.
     */
    private static final Map<String, CurrencyInfo> CURR_CODES = new HashMap<>(256);
    
    private static final Map<String, CurrencyInfo> CURR_NUMERIC = new HashMap<>(256);
    
    static {
        addCurrency(CurrencyCode.get("AED"), CurrencyNumeric.get("784"), 2); // United Arab Emirates dirham
        addCurrency(CurrencyCode.get("AFN"), CurrencyNumeric.get("971"), 2); // Afghan afghani
        addCurrency(CurrencyCode.get("ALL"), CurrencyNumeric.get("008"), 2); // Albanian lek
        addCurrency(CurrencyCode.get("AMD"), CurrencyNumeric.get("051"), 2); // Armenian dram
        addCurrency(CurrencyCode.get("ANG"), CurrencyNumeric.get("532"), 2); // Netherlands Antillean guilder
        addCurrency(CurrencyCode.get("AOA"), CurrencyNumeric.get("973"), 2); // Angolan kwanza
        addCurrency(CurrencyCode.get("ARS"), CurrencyNumeric.get("032"), 2); // Argentine peso
        addCurrency(CurrencyCode.get("AUD"), CurrencyNumeric.get("036"), 2); // Australian dollar
        addCurrency(CurrencyCode.get("AWG"), CurrencyNumeric.get("533"), 2); // Aruban florin
        addCurrency(CurrencyCode.get("AZN"), CurrencyNumeric.get("944"), 2); // Azerbaijani manat
        addCurrency(CurrencyCode.get("BAM"), CurrencyNumeric.get("977"), 2); // Bosnia and Herzegovina convertible mark
        addCurrency(CurrencyCode.get("BBD"), CurrencyNumeric.get("052"), 2); // Barbados dollar
        addCurrency(CurrencyCode.get("BDT"), CurrencyNumeric.get("050"), 2); // Bangladeshi taka
        addCurrency(CurrencyCode.get("BGN"), CurrencyNumeric.get("975"), 2); // Bulgarian lev
        addCurrency(CurrencyCode.get("BHD"), CurrencyNumeric.get("048"), 3); // Bahraini dinar
        addCurrency(CurrencyCode.get("BIF"), CurrencyNumeric.get("108"), 0); // Burundian franc
        addCurrency(CurrencyCode.get("BMD"), CurrencyNumeric.get("060"), 2); // Bermudian dollar (customarily known as Bermuda dollar)
        addCurrency(CurrencyCode.get("BND"), CurrencyNumeric.get("096"), 2); // Brunei dollar
        addCurrency(CurrencyCode.get("BOB"), CurrencyNumeric.get("068"), 2); // Boliviano
        addCurrency(CurrencyCode.get("BOV"), CurrencyNumeric.get("984"), 2); // Bolivian Mvdol (funds code)
        addCurrency(CurrencyCode.get("BRL"), CurrencyNumeric.get("986"), 2); // Brazilian real
        addCurrency(CurrencyCode.get("BSD"), CurrencyNumeric.get("044"), 2); // Bahamian dollar
        addCurrency(CurrencyCode.get("BTN"), CurrencyNumeric.get("064"), 2); // Bhutanese ngultrum
        addCurrency(CurrencyCode.get("BWP"), CurrencyNumeric.get("072"), 2); // Botswana pula
        addCurrency(CurrencyCode.get("BYN"), CurrencyNumeric.get("933"), 2); // Belarusian Ruble
        addCurrency(CurrencyCode.get("BYR"), CurrencyNumeric.get("974"), 0); // Belarusian ruble (deprecated)
        addCurrency(CurrencyCode.get("BZD"), CurrencyNumeric.get("084"), 2); // Belize dollar
        addCurrency(CurrencyCode.get("CAD"), CurrencyNumeric.get("124"), 2); // Canadian dollar
        addCurrency(CurrencyCode.get("CDF"), CurrencyNumeric.get("976"), 2); // Congolese franc
        addCurrency(CurrencyCode.get("CHE"), CurrencyNumeric.get("947"), 2); // WIR bank (complementary currency)
        addCurrency(CurrencyCode.get("CHF"), CurrencyNumeric.get("756"), 2); // Swiss franc
        addCurrency(CurrencyCode.get("CHW"), CurrencyNumeric.get("948"), 2); // WIR bank (complementary currency)
        addCurrency(CurrencyCode.get("CLF"), CurrencyNumeric.get("990"), 4); // Unidad de Fomento (funds code)
        addCurrency(CurrencyCode.get("CLP"), CurrencyNumeric.get("152"), 0); // Chilean peso
        addCurrency(CurrencyCode.get("CNY"), CurrencyNumeric.get("156"), 2); // Chinese yuan
        addCurrency(CurrencyCode.get("COP"), CurrencyNumeric.get("170"), 2); // Colombian peso
        addCurrency(CurrencyCode.get("COU"), CurrencyNumeric.get("970"), 2); // Unidad de Valor Real
        addCurrency(CurrencyCode.get("CRC"), CurrencyNumeric.get("188"), 2); // Costa Rican colon
        addCurrency(CurrencyCode.get("CUC"), CurrencyNumeric.get("931"), 2); // Cuban convertible peso
        addCurrency(CurrencyCode.get("CUP"), CurrencyNumeric.get("192"), 2); // Cuban peso
        addCurrency(CurrencyCode.get("CVE"), CurrencyNumeric.get("132"), 2); // Cape Verde escudo
        addCurrency(CurrencyCode.get("CZK"), CurrencyNumeric.get("203"), 2); // Czech koruna
        addCurrency(CurrencyCode.get("DJF"), CurrencyNumeric.get("262"), 0); // Djiboutian franc
        addCurrency(CurrencyCode.get("DKK"), CurrencyNumeric.get("208"), 2); // Danish krone
        addCurrency(CurrencyCode.get("DOP"), CurrencyNumeric.get("214"), 2); // Dominican peso
        addCurrency(CurrencyCode.get("DZD"), CurrencyNumeric.get("012"), 2); // Algerian dinar
        addCurrency(CurrencyCode.get("EEK"), CurrencyNumeric.get("233"), 2); // Estonian kroon (deprecated)
        addCurrency(CurrencyCode.get("EGP"), CurrencyNumeric.get("818"), 2); // Egyptian pound
        addCurrency(CurrencyCode.get("ERN"), CurrencyNumeric.get("232"), 2); // Eritrean nakfa
        addCurrency(CurrencyCode.get("ESA"), CurrencyNumeric.get("996"), 0); // Spanish peseta (account A) (deprecated)
        addCurrency(CurrencyCode.get("ETB"), CurrencyNumeric.get("230"), 2); // Ethiopian birr
        addCurrency(CurrencyCode.get("EUR"), CurrencyNumeric.get("978"), 2); // Euro
        addCurrency(CurrencyCode.get("FJD"), CurrencyNumeric.get("242"), 2); // Fiji dollar
        addCurrency(CurrencyCode.get("FKP"), CurrencyNumeric.get("238"), 2); // Falkland Islands pound
        addCurrency(CurrencyCode.get("GBP"), CurrencyNumeric.get("826"), 2); // Pound sterling
        addCurrency(CurrencyCode.get("GEL"), CurrencyNumeric.get("981"), 2); // Georgian lari
        addCurrency(CurrencyCode.get("GHS"), CurrencyNumeric.get("936"), 2); // Ghanaian cedi
        addCurrency(CurrencyCode.get("GIP"), CurrencyNumeric.get("292"), 2); // Gibraltar pound
        addCurrency(CurrencyCode.get("GMD"), CurrencyNumeric.get("270"), 2); // Gambian dalasi
        addCurrency(CurrencyCode.get("GNF"), CurrencyNumeric.get("324"), 0); // Guinean franc
        addCurrency(CurrencyCode.get("GTQ"), CurrencyNumeric.get("320"), 2); // Guatemalan quetzal
        addCurrency(CurrencyCode.get("GYD"), CurrencyNumeric.get("328"), 2); // Guyanese dollar
        addCurrency(CurrencyCode.get("HKD"), CurrencyNumeric.get("344"), 2); // Hong Kong dollar
        addCurrency(CurrencyCode.get("HNL"), CurrencyNumeric.get("340"), 2); // Honduran lempira
        addCurrency(CurrencyCode.get("HRK"), CurrencyNumeric.get("191"), 2); // Croatian kuna
        addCurrency(CurrencyCode.get("HTG"), CurrencyNumeric.get("332"), 2); // Haitian gourde
        addCurrency(CurrencyCode.get("HUF"), CurrencyNumeric.get("348"), 2); // Hungarian forint
        addCurrency(CurrencyCode.get("IDR"), CurrencyNumeric.get("360"), 2); // Indonesian rupiah
        addCurrency(CurrencyCode.get("ILS"), CurrencyNumeric.get("376"), 2); // Israeli new sheqel
        addCurrency(CurrencyCode.get("INR"), CurrencyNumeric.get("356"), 2); // Indian rupee
        addCurrency(CurrencyCode.get("IQD"), CurrencyNumeric.get("368"), 3); // Iraqi dinar
        addCurrency(CurrencyCode.get("IRR"), CurrencyNumeric.get("364"), 2); // Iranian rial
        addCurrency(CurrencyCode.get("ISK"), CurrencyNumeric.get("352"), 0); // Icelandic krona
        addCurrency(CurrencyCode.get("JMD"), CurrencyNumeric.get("388"), 2); // Jamaican dollar
        addCurrency(CurrencyCode.get("JOD"), CurrencyNumeric.get("400"), 3); // Jordanian dinar
        addCurrency(CurrencyCode.get("JPY"), CurrencyNumeric.get("392"), 0); // Japanese yen
        addCurrency(CurrencyCode.get("KES"), CurrencyNumeric.get("404"), 2); // Kenyan shilling
        addCurrency(CurrencyCode.get("KGS"), CurrencyNumeric.get("417"), 2); // Kyrgyzstani som
        addCurrency(CurrencyCode.get("KHR"), CurrencyNumeric.get("116"), 2); // Cambodian riel
        addCurrency(CurrencyCode.get("KMF"), CurrencyNumeric.get("174"), 0); // Comoro franc
        addCurrency(CurrencyCode.get("KPW"), CurrencyNumeric.get("408"), 2); // North Korean won
        addCurrency(CurrencyCode.get("KRW"), CurrencyNumeric.get("410"), 0); // South Korean won
        addCurrency(CurrencyCode.get("KWD"), CurrencyNumeric.get("414"), 3); // Kuwaiti dinar
        addCurrency(CurrencyCode.get("KYD"), CurrencyNumeric.get("136"), 2); // Cayman Islands dollar
        addCurrency(CurrencyCode.get("KZT"), CurrencyNumeric.get("398"), 2); // Kazakhstani tenge
        addCurrency(CurrencyCode.get("LAK"), CurrencyNumeric.get("418"), 2); // Lao kip
        addCurrency(CurrencyCode.get("LBP"), CurrencyNumeric.get("422"), 2); // Lebanese pound
        addCurrency(CurrencyCode.get("LKR"), CurrencyNumeric.get("144"), 2); // Sri Lanka rupee
        addCurrency(CurrencyCode.get("LRD"), CurrencyNumeric.get("430"), 2); // Liberian dollar
        addCurrency(CurrencyCode.get("LSL"), CurrencyNumeric.get("426"), 2); // Lesotho loti
        addCurrency(CurrencyCode.get("LTL"), CurrencyNumeric.get("440"), 2); // Lithuanian litas (deprecated)
        addCurrency(CurrencyCode.get("LVL"), CurrencyNumeric.get("428"), 2); // Latvian lats (deprecated)
        addCurrency(CurrencyCode.get("LYD"), CurrencyNumeric.get("434"), 3); // Libyan dinar
        addCurrency(CurrencyCode.get("MAD"), CurrencyNumeric.get("504"), 2); // Moroccan dirham
        addCurrency(CurrencyCode.get("MDL"), CurrencyNumeric.get("498"), 2); // Moldovan leu
        addCurrency(CurrencyCode.get("MGA"), CurrencyNumeric.get("969"), 2); // Malagasy ariary - Exponent should technically be 0.69897
        addCurrency(CurrencyCode.get("MKD"), CurrencyNumeric.get("807"), 2); // Macedonian denar
        addCurrency(CurrencyCode.get("MMK"), CurrencyNumeric.get("104"), 2); // Myanma kyat
        addCurrency(CurrencyCode.get("MNT"), CurrencyNumeric.get("496"), 2); // Mongolian tugrik
        addCurrency(CurrencyCode.get("MOP"), CurrencyNumeric.get("446"), 2); // Macanese pataca
        addCurrency(CurrencyCode.get("MRO"), CurrencyNumeric.get("478"), 2); // Mauritanian ouguiya - Exponent should technically be 0.69897 (deprecated)
        addCurrency(CurrencyCode.get("MRU"), CurrencyNumeric.get("929"), 2); // Mauritanian ouguiya
        addCurrency(CurrencyCode.get("MUR"), CurrencyNumeric.get("480"), 2); // Mauritian rupee
        addCurrency(CurrencyCode.get("MVR"), CurrencyNumeric.get("462"), 2); // Maldivian rufiyaa
        addCurrency(CurrencyCode.get("MWK"), CurrencyNumeric.get("454"), 2); // Malawian kwacha
        addCurrency(CurrencyCode.get("MXN"), CurrencyNumeric.get("484"), 2); // Mexican peso
        addCurrency(CurrencyCode.get("MXV"), CurrencyNumeric.get("979"), 2); // Mexican Unidad de Inversion (UDI) (funds code)
        addCurrency(CurrencyCode.get("MYR"), CurrencyNumeric.get("458"), 2); // Malaysian ringgit
        addCurrency(CurrencyCode.get("MZN"), CurrencyNumeric.get("943"), 2); // Mozambican metical
        addCurrency(CurrencyCode.get("NAD"), CurrencyNumeric.get("516"), 2); // Namibian dollar
        addCurrency(CurrencyCode.get("NGN"), CurrencyNumeric.get("566"), 2); // Nigerian naira
        addCurrency(CurrencyCode.get("NIO"), CurrencyNumeric.get("558"), 2); // Cordoba oro
        addCurrency(CurrencyCode.get("NOK"), CurrencyNumeric.get("578"), 2); // Norwegian krone
        addCurrency(CurrencyCode.get("NPR"), CurrencyNumeric.get("524"), 2); // Nepalese rupee
        addCurrency(CurrencyCode.get("NZD"), CurrencyNumeric.get("554"), 2); // New Zealand dollar
        addCurrency(CurrencyCode.get("OMR"), CurrencyNumeric.get("512"), 3); // Omani rial
        addCurrency(CurrencyCode.get("PAB"), CurrencyNumeric.get("590"), 2); // Panamanian balboa
        addCurrency(CurrencyCode.get("PEN"), CurrencyNumeric.get("604"), 2); // Peruvian nuevo sol
        addCurrency(CurrencyCode.get("PGK"), CurrencyNumeric.get("598"), 2); // Papua New Guinean kina
        addCurrency(CurrencyCode.get("PHP"), CurrencyNumeric.get("608"), 2); // Philippine peso
        addCurrency(CurrencyCode.get("PKR"), CurrencyNumeric.get("586"), 2); // Pakistani rupee
        addCurrency(CurrencyCode.get("PLN"), CurrencyNumeric.get("985"), 2); // Polish zloty
        addCurrency(CurrencyCode.get("PYG"), CurrencyNumeric.get("600"), 0); // Paraguayan guarani
        addCurrency(CurrencyCode.get("QAR"), CurrencyNumeric.get("634"), 2); // Qatari rial
        addCurrency(CurrencyCode.get("RON"), CurrencyNumeric.get("946"), 2); // Romanian new leu
        addCurrency(CurrencyCode.get("RSD"), CurrencyNumeric.get("941"), 2); // Serbian dinar
        addCurrency(CurrencyCode.get("RUB"), CurrencyNumeric.get("643"), 2); // Russian rouble
        addCurrency(CurrencyCode.get("RWF"), CurrencyNumeric.get("646"), 0); // Rwandan franc
        addCurrency(CurrencyCode.get("SAR"), CurrencyNumeric.get("682"), 2); // Saudi riyal
        addCurrency(CurrencyCode.get("SBD"), CurrencyNumeric.get("090"), 2); // Solomon Islands dollar
        addCurrency(CurrencyCode.get("SCR"), CurrencyNumeric.get("690"), 2); // Seychelles rupee
        addCurrency(CurrencyCode.get("SDG"), CurrencyNumeric.get("938"), 2); // Sudanese pound
        addCurrency(CurrencyCode.get("SEK"), CurrencyNumeric.get("752"), 2); // Swedish krona/kronor
        addCurrency(CurrencyCode.get("SGD"), CurrencyNumeric.get("702"), 2); // Singapore dollar
        addCurrency(CurrencyCode.get("SHP"), CurrencyNumeric.get("654"), 2); // Saint Helena pound
        addCurrency(CurrencyCode.get("SLL"), CurrencyNumeric.get("694"), 2); // Sierra Leonean leone
        addCurrency(CurrencyCode.get("SOS"), CurrencyNumeric.get("706"), 2); // Somali shilling
        addCurrency(CurrencyCode.get("SRD"), CurrencyNumeric.get("968"), 2); // Surinamese dollar
        addCurrency(CurrencyCode.get("SSP"), CurrencyNumeric.get("728"), 2); // South Sudanese pound
        addCurrency(CurrencyCode.get("STD"), CurrencyNumeric.get("678"), 2); // Sao Tome and Principe dobra (deprecated)
        addCurrency(CurrencyCode.get("STN"), CurrencyNumeric.get("930"), 2); // Sao Tome and Principe dobra
        addCurrency(CurrencyCode.get("SVC"), CurrencyNumeric.get("222"), 2); // Salvadoran colón
        addCurrency(CurrencyCode.get("SYP"), CurrencyNumeric.get("760"), 2); // Syrian pound
        addCurrency(CurrencyCode.get("SZL"), CurrencyNumeric.get("748"), 2); // Lilangeni
        addCurrency(CurrencyCode.get("THB"), CurrencyNumeric.get("764"), 2); // Thai baht
        addCurrency(CurrencyCode.get("TJS"), CurrencyNumeric.get("972"), 2); // Tajikistani somoni
        addCurrency(CurrencyCode.get("TMT"), CurrencyNumeric.get("934"), 2); // Turkmenistani manat
        addCurrency(CurrencyCode.get("TND"), CurrencyNumeric.get("788"), 3); // Tunisian dinar
        addCurrency(CurrencyCode.get("TOP"), CurrencyNumeric.get("776"), 2); // Tongan pa'anga
        addCurrency(CurrencyCode.get("TRY"), CurrencyNumeric.get("949"), 2); // Turkish lira
        addCurrency(CurrencyCode.get("TTD"), CurrencyNumeric.get("780"), 2); // Trinidad and Tobago dollar
        addCurrency(CurrencyCode.get("TWD"), CurrencyNumeric.get("901"), 2); // New Taiwan dollar
        addCurrency(CurrencyCode.get("TZS"), CurrencyNumeric.get("834"), 2); // Tanzanian shilling
        addCurrency(CurrencyCode.get("UAH"), CurrencyNumeric.get("980"), 2); // Ukrainian hryvnia
        addCurrency(CurrencyCode.get("UGX"), CurrencyNumeric.get("800"), 0); // Ugandan shilling
        addCurrency(CurrencyCode.get("USD"), CurrencyNumeric.get("840"), 2); // United States dollar
        // addCurrency(CurrencyCode.get("USN"), CurrencyNumeric.get("997"), 2); // United States dollar (next day)
        // (funds code)
        // addCurrency(CurrencyCode.get("USS"), CurrencyNumeric.get("998"), 2); // United States dollar (same day)
        // (funds code)
        addCurrency(CurrencyCode.get("UYI"), CurrencyNumeric.get("940"), 0); // Uruguay Peso en Unidades Indexadas (UI)
        addCurrency(CurrencyCode.get("UYU"), CurrencyNumeric.get("858"), 2); // Uruguayan peso
        addCurrency(CurrencyCode.get("UYW"), CurrencyNumeric.get("927"), 4); // Unidad Previsional
        addCurrency(CurrencyCode.get("UZS"), CurrencyNumeric.get("860"), 2); // Uzbekistan som
        addCurrency(CurrencyCode.get("VEF"), CurrencyNumeric.get("937"), 2); // Venezuelan bolivar fuerte (deprecated)
        addCurrency(CurrencyCode.get("VES"), CurrencyNumeric.get("928"), 2); // Venezuelan Soberano
        addCurrency(CurrencyCode.get("VND"), CurrencyNumeric.get("704"), 0); // Vietnamese dong
        addCurrency(CurrencyCode.get("VUV"), CurrencyNumeric.get("548"), 0); // Vanuatu vatu
        addCurrency(CurrencyCode.get("WST"), CurrencyNumeric.get("882"), 2); // Samoan tala
        addCurrency(CurrencyCode.get("XAF"), CurrencyNumeric.get("950"), 0); // CFA franc BEAC
        // addCurrency(CurrencyCode.get("XAG"), CurrencyNumeric.get("961"), 0); // Silver (one troy ounce)
        // addCurrency(CurrencyCode.get("XAU"), CurrencyNumeric.get("959"), 0); // Gold (one troy ounce)
        // addCurrency(CurrencyCode.get("XBA"), CurrencyNumeric.get("955"), 0); // European Composite Unit (EURCO) (bond
        // market unit)
        // addCurrency(CurrencyCode.get("XBB"), CurrencyNumeric.get("956"), 0); // European Monetary Unit (E.M.U.-6)
        // (bond market unit)
        // addCurrency(CurrencyCode.get("XBC"), CurrencyNumeric.get("957"), 0); // European Unit of Account 9 (E.U.A.-9)
        // (bond market unit)
        // addCurrency(CurrencyCode.get("XBD"), CurrencyNumeric.get("958"), 0); // European Unit of Account 17
        // (E.U.A.-17) (bond market unit)
        addCurrency(CurrencyCode.get("XCD"), CurrencyNumeric.get("951"), 2); // East Caribbean dollar
        // addCurrency(CurrencyCode.get("XDR"), CurrencyNumeric.get("960"), 0); // Special Drawing Rights
        // addCurrency(CurrencyCode.get("XFU"), CurrencyNumeric.get("Nil"), 0); // UIC franc (special settlement
        // currency)
        addCurrency(CurrencyCode.get("XOF"), CurrencyNumeric.get("952"), 0); // CFA Franc BCEAO
        // addCurrency(CurrencyCode.get("XPD"), CurrencyNumeric.get("964"), 0); // Palladium (one troy ounce)
        addCurrency(CurrencyCode.get("XPF"), CurrencyNumeric.get("953"), 0); // CFP franc
        // addCurrency(CurrencyCode.get("XPT"), CurrencyNumeric.get("962"), 0); // Platinum (one troy ounce)
        // addCurrency(CurrencyCode.get("XTS"), CurrencyNumeric.get("963"), 0); // Code reserved for testing purposes
        // addCurrency(CurrencyCode.get("XXX"), CurrencyNumeric.get("999"), 0); // No currency
        addCurrency(CurrencyCode.get("YER"), CurrencyNumeric.get("886"), 2); // Yemeni rial
        addCurrency(CurrencyCode.get("ZAR"), CurrencyNumeric.get("710"), 2); // South African rand
        addCurrency(CurrencyCode.get("ZMK"), CurrencyNumeric.get("894"), 0); // Zambian kwacha (deprecated)
        addCurrency(CurrencyCode.get("ZMW"), CurrencyNumeric.get("967"), 2); // Zambian kwacha
        addCurrency(CurrencyCode.get("ZWL"), CurrencyNumeric.get("932"), 2); // Zimbabwe dollar
    }
    
    /**
     * Get the CurrencyInfo for a given ISO/Swift code; never null.
     *
     * @param currencyCode currency to look up; must be non-null
     * @return CurrencyInfo value or null
     */
    public static CurrencyInfo get(final CurrencyCode currencyCode) {
        return CURR_CODES.get(currencyCode.getCode());
    }
    
    /**
     * Get the CurrencyInfo for a given ISO/8853 code; never null.
     *
     * @param currencyNumeric currency to look up; must be non-null
     * @return CurrencyInfo value or null
     */
    public static CurrencyInfo get(final CurrencyNumeric currencyNumeric) {
        return CURR_NUMERIC.get(currencyNumeric.getCode());
    }
    
    public static Map<String, CurrencyInfo> getCurrencyCodes() {
        return Collections.unmodifiableMap(CURR_CODES);
    }
    
    /**
     * Gets the ISO Swift currency code equivalent of an ISO 8853 code. For example, would return GBP for 426.
     *
     * @param currencyNumeric currency code to look up; must be non-null
     * @return CurrencyCode value; never null
     * @throws IllegalArgumentException if we hold no info on the currency
     */
    public static CurrencyCode convert(final CurrencyNumeric currencyNumeric) {
        return get(currencyNumeric).getCurrencyCode();
    }
    
    /**
     * Gets the ISO 8853 currency code equivalent of an ISO Swift currency code. For example, would return 427 for GBP.
     *
     * @param currencyCode currency code to look up; must be non-null
     * @return CurrencyNumeric value; never null
     * @throws IllegalArgumentException if we hold no info on the currency
     */
    public static CurrencyNumeric convert(final CurrencyCode currencyCode) {
        return get(currencyCode).getCurrencyNumeric();
    }
    
    private static void addCurrency(final CurrencyCode currencyCode, final CurrencyNumeric currencyNumeric, final int leftShift) {
        
        if (CURR_CODES.containsKey(currencyCode.getCode()) || CURR_NUMERIC.containsKey(currencyNumeric.getCode())) {
            throw new IllegalStateException("Currency with codes " + currencyCode + " " + currencyNumeric + " already added");
        }
        
        final CurrencyInfo currencyInfo = new CurrencyInfo(currencyCode, currencyNumeric, leftShift);
        CURR_CODES.put(currencyCode.getCode(), currencyInfo);
        CURR_NUMERIC.put(currencyNumeric.getCode(), currencyInfo);
    }
    
    /**
     * The ISO/SWIFT alphabetical currency code, eg "GBP"; never null.
     */
    private final CurrencyCode currencyCode;
    
    /**
     * The ISO 8853 numerical currency code, eg "726"; never null.
     */
    private final CurrencyNumeric currencyNumeric;
    
    /**
     * The digits in the minor unit of the currency or 0 if none; non-negative.
     */
    private final int leftShift;
    
    /**
     * Construct an instance of this object containing all static data on a particular currency.
     *
     * @param currencyCode alphabetic currency code; non-null
     * @param currencyNumeric numeric currency code; non-null
     * @param leftShift digits in the minor currency unit for display and storage; non-negative
     */
    private CurrencyInfo(final CurrencyCode currencyCode, final CurrencyNumeric currencyNumeric, final int leftShift) {
        
        if (currencyCode == null) {
            throw new IllegalArgumentException("currencyCode is null");
        }
        if (currencyNumeric == null) {
            throw new IllegalArgumentException("currencyNumeric is null");
        }
        if ((leftShift < 0) || (leftShift > 4)) {
            throw new IllegalArgumentException("Only left shifts between 0 and 4 supported. Given ["
                + leftShift
                + "] for currency ["
                + this
                + "]");
        }
        
        this.currencyCode = currencyCode;
        this.currencyNumeric = currencyNumeric;
        this.leftShift = leftShift;
    }
    
    /**
     * @return a human-readable String containing the main content of this instance; never null.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [code = " + currencyCode + ", numeric = " + currencyNumeric + ", leftShift = " + leftShift + "]";
    }
    
    /**
     * @return the ISO8853 currency type; never null.
     */
    public CurrencyCode getCurrencyCode() {
        return currencyCode;
    }
    
    /**
     * @return the ISO8853 currency type; never null.
     */
    public CurrencyNumeric getCurrencyNumeric() {
        return currencyNumeric;
    }
    
    /**
     * @return left-shift decimal digits; non-negative.
     */
    public int getLeftShift() {
        return leftShift;
    }
    
}
