package com.supundevendra.iso;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/*
 * ISOFieldDictionary
 * Contains all ISO 8583 field definitions (fields 1-128).
 * Each entry maps: field number -> [fieldName, description]
 */

public class ISOFieldDictionary {

    private static final Map<Integer, String[]> FIELDS;

    static {
        Map<Integer, String[]> map = new HashMap<>();

        map.put(1,   new String[]{"Bitmap",                                    "Primary bitmap indicating presence of fields 1-64"});
        map.put(2,   new String[]{"Primary Account Number (PAN)",               "Card number used for the transaction"});
        map.put(3,   new String[]{"Processing Code",                            "Transaction type and account type identifier"});
        map.put(4,   new String[]{"Transaction Amount",                         "Amount of the transaction in minor currency units"});
        map.put(5,   new String[]{"Settlement Amount",                          "Amount in settlement currency"});
        map.put(6,   new String[]{"Cardholder Billing Amount",                  "Amount in cardholder billing currency"});
        map.put(7,   new String[]{"Transmission Date and Time",                 "Date and time of message transmission (MMDDhhmmss)"});
        map.put(8,   new String[]{"Cardholder Billing Fee Amount",              "Fee charged to cardholder billing account"});
        map.put(9,   new String[]{"Settlement Conversion Rate",                 "Conversion rate used for settlement"});
        map.put(10,  new String[]{"Cardholder Billing Conversion Rate",         "Conversion rate used for cardholder billing"});
        map.put(11,  new String[]{"System Trace Audit Number (STAN)",           "Unique transaction identifier assigned by originator"});
        map.put(12,  new String[]{"Local Transaction Time",                     "Local time of transaction (hhmmss)"});
        map.put(13,  new String[]{"Local Transaction Date",                     "Local date of transaction (MMDD)"});
        map.put(14,  new String[]{"Expiration Date",                            "Card expiration date (YYMM)"});
        map.put(15,  new String[]{"Settlement Date",                            "Date the transaction will be settled (MMDD)"});
        map.put(16,  new String[]{"Currency Conversion Date",                   "Date of currency conversion (MMDD)"});
        map.put(17,  new String[]{"Capture Date",                               "Date of capture (MMDD)"});
        map.put(18,  new String[]{"Merchant Category Code",                     "ISO merchant category code (MCC)"});
        map.put(19,  new String[]{"Acquiring Institution Country Code",         "Country code of acquiring institution"});
        map.put(20,  new String[]{"PAN Extended Country Code",                  "Country code associated with PAN"});
        map.put(21,  new String[]{"Forwarding Institution Country Code",        "Country code of forwarding institution"});
        map.put(22,  new String[]{"Point of Service Entry Mode",                "POS terminal entry mode (e.g. chip, swipe, manual)"});
        map.put(23,  new String[]{"Application PAN Sequence Number",            "Sequence number of PAN application"});
        map.put(24,  new String[]{"Function Code",                              "Network function code"});
        map.put(25,  new String[]{"Point of Service Condition Code",            "Condition at point of service"});
        map.put(26,  new String[]{"Point of Service Capture Code",              "Capture method at point of service"});
        map.put(27,  new String[]{"Authorizing Identification Response Length", "Length of authorizing ID response"});
        map.put(28,  new String[]{"Transaction Fee Amount",                     "Fee amount charged for transaction"});
        map.put(29,  new String[]{"Settlement Fee Amount",                      "Fee amount in settlement currency"});
        map.put(30,  new String[]{"Transaction Processing Fee Amount",          "Processing fee for the transaction"});
        map.put(31,  new String[]{"Settlement Processing Fee Amount",           "Processing fee in settlement currency"});
        map.put(32,  new String[]{"Acquiring Institution Identification Code",  "ID of the acquiring institution"});
        map.put(33,  new String[]{"Forwarding Institution Identification Code", "ID of the forwarding institution"});
        map.put(34,  new String[]{"Primary Account Number Extended",            "Extended PAN data"});
        map.put(35,  new String[]{"Track 2 Data",                               "Magnetic stripe track 2 data"});
        map.put(36,  new String[]{"Track 3 Data",                               "Magnetic stripe track 3 data"});
        map.put(37,  new String[]{"Retrieval Reference Number",                 "Reference number for transaction retrieval"});
        map.put(38,  new String[]{"Authorization Identification Response",      "Approval code from issuer"});
        map.put(39,  new String[]{"Response Code",                              "Transaction response code (e.g. 00=Approved)"});
        map.put(40,  new String[]{"Service Restriction Code",                   "Restrictions on card service"});
        map.put(41,  new String[]{"Card Acceptor Terminal Identification",      "Terminal where the transaction occurred"});
        map.put(42,  new String[]{"Card Acceptor Identification Code",          "Merchant/acceptor identification code"});
        map.put(43,  new String[]{"Card Acceptor Name and Location",            "Name and location of card acceptor"});
        map.put(44,  new String[]{"Additional Response Data",                   "Additional data from issuer response"});
        map.put(45,  new String[]{"Track 1 Data",                               "Magnetic stripe track 1 data"});
        map.put(46,  new String[]{"Additional Data ISO",                        "ISO-defined additional data"});
        map.put(47,  new String[]{"Additional Data National",                   "Nationally defined additional data"});
        map.put(48,  new String[]{"Additional Data Private",                    "Privately defined additional data"});
        map.put(49,  new String[]{"Transaction Currency Code",                  "ISO currency code of transaction (e.g. 840=USD)"});
        map.put(50,  new String[]{"Settlement Currency Code",                   "ISO currency code for settlement"});
        map.put(51,  new String[]{"Cardholder Billing Currency Code",           "ISO currency code for cardholder billing"});
        map.put(52,  new String[]{"Personal Identification Number (PIN) Data",  "Encrypted PIN block data"});
        map.put(53,  new String[]{"Security Related Control Information",       "Security control data"});
        map.put(54,  new String[]{"Additional Amounts",                         "Additional balance or amount information"});
        map.put(55,  new String[]{"ICC Data (EMV)",                             "EMV chip card data with multiple TLV tags"});
        map.put(56,  new String[]{"Reserved ISO",                               "Reserved for ISO use"});
        map.put(57,  new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(58,  new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(59,  new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(60,  new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(61,  new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(62,  new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(63,  new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(64,  new String[]{"Message Authentication Code (MAC)",          "Primary MAC for message integrity"});
        map.put(65,  new String[]{"Extended Bitmap",                            "Secondary bitmap indicating presence of fields 65-128"});
        map.put(66,  new String[]{"Settlement Code",                            "Code for settlement type"});
        map.put(67,  new String[]{"Extended Payment Code",                      "Code for extended payment type"});
        map.put(68,  new String[]{"Receiving Institution Country Code",         "Country code of receiving institution"});
        map.put(69,  new String[]{"Settlement Institution Country Code",        "Country code of settlement institution"});
        map.put(70,  new String[]{"Network Management Information Code",        "Network management function code"});
        map.put(71,  new String[]{"Message Number",                             "Sequence number of this message"});
        map.put(72,  new String[]{"Message Number Last",                        "Sequence number of last message"});
        map.put(73,  new String[]{"Action Date",                                "Date action is to be taken (YYMMDD)"});
        map.put(74,  new String[]{"Credits Number",                             "Number of credit transactions"});
        map.put(75,  new String[]{"Credits Reversal Number",                    "Number of credit reversal transactions"});
        map.put(76,  new String[]{"Debits Number",                              "Number of debit transactions"});
        map.put(77,  new String[]{"Debits Reversal Number",                     "Number of debit reversal transactions"});
        map.put(78,  new String[]{"Transfer Number",                            "Number of transfer transactions"});
        map.put(79,  new String[]{"Transfer Reversal Number",                   "Number of transfer reversal transactions"});
        map.put(80,  new String[]{"Inquiries Number",                           "Number of inquiry transactions"});
        map.put(81,  new String[]{"Authorizations Number",                      "Number of authorization transactions"});
        map.put(82,  new String[]{"Credits Processing Fee Amount",              "Fee for processing credit transactions"});
        map.put(83,  new String[]{"Credits Transaction Fee Amount",             "Transaction fee for credits"});
        map.put(84,  new String[]{"Debits Processing Fee Amount",               "Fee for processing debit transactions"});
        map.put(85,  new String[]{"Debits Transaction Fee Amount",              "Transaction fee for debits"});
        map.put(86,  new String[]{"Credits Amount",                             "Total amount of credit transactions"});
        map.put(87,  new String[]{"Credits Reversal Amount",                    "Total amount of credit reversals"});
        map.put(88,  new String[]{"Debits Amount",                              "Total amount of debit transactions"});
        map.put(89,  new String[]{"Debits Reversal Amount",                     "Total amount of debit reversals"});
        map.put(90,  new String[]{"Original Data Elements",                     "Key fields of the original transaction"});
        map.put(91,  new String[]{"File Update Code",                           "Code for file update operation"});
        map.put(92,  new String[]{"File Security Code",                         "Security code for file access"});
        map.put(93,  new String[]{"Response Indicator",                         "Indicator of response type"});
        map.put(94,  new String[]{"Service Indicator",                          "Service type indicator"});
        map.put(95,  new String[]{"Replacement Amounts",                        "Replacement amounts for reversal transactions"});
        map.put(96,  new String[]{"Message Security Code",                      "Security code for message authentication"});
        map.put(97,  new String[]{"Net Settlement Amount",                      "Net amount for settlement"});
        map.put(98,  new String[]{"Payee",                                      "Name of the payee"});
        map.put(99,  new String[]{"Settlement Institution Identification Code", "ID of settlement institution"});
        map.put(100, new String[]{"Receiving Institution Identification Code",  "ID of receiving institution"});
        map.put(101, new String[]{"File Name",                                  "Name of file being processed"});
        map.put(102, new String[]{"Account Identification 1",                   "Primary account identification"});
        map.put(103, new String[]{"Account Identification 2",                   "Secondary account identification"});
        map.put(104, new String[]{"Transaction Description",                    "Description of the transaction"});
        map.put(105, new String[]{"Reserved ISO",                               "Reserved for ISO use"});
        map.put(106, new String[]{"Reserved ISO",                               "Reserved for ISO use"});
        map.put(107, new String[]{"Reserved ISO",                               "Reserved for ISO use"});
        map.put(108, new String[]{"Reserved ISO",                               "Reserved for ISO use"});
        map.put(109, new String[]{"Reserved ISO",                               "Reserved for ISO use"});
        map.put(110, new String[]{"Reserved ISO",                               "Reserved for ISO use"});
        map.put(111, new String[]{"Reserved ISO",                               "Reserved for ISO use"});
        map.put(112, new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(113, new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(114, new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(115, new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(116, new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(117, new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(118, new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(119, new String[]{"Reserved National",                          "Reserved for national use"});
        map.put(120, new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(121, new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(122, new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(123, new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(124, new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(125, new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(126, new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(127, new String[]{"Reserved Private",                           "Reserved for private use"});
        map.put(128, new String[]{"Message Authentication Code (MAC) Field",    "Secondary MAC for extended message integrity"});

        FIELDS = Collections.unmodifiableMap(map);
    }

    /**
     * Get the field name for a given field number.
     *
     * @param fieldNumber ISO 8583 field number (1-128)
     * @return field name, or "Unknown Field" if not found
     */
    public static String getName(int fieldNumber) {
        String[] entry = FIELDS.get(fieldNumber);
        return (entry != null) ? entry[0] : "Unknown Field";
    }

    /*
     * Get the description for a given field number.
     * @param fieldNumber ISO 8583 field number (1-128)
     * return field description, or "No description available" if not found
     */
    public static String getDescription(int fieldNumber) {
        String[] entry = FIELDS.get(fieldNumber);
        return (entry != null) ? entry[1] : "No description available";
    }

    /*
     * Return a human-readable MTI description.
     * @param mti 4-character MTI string (e.g. "0100")
     * return description string
     */
    public static String getMtiDescription(String mti) {
        if (mti == null || mti.length() < 4) return "Unknown";
        switch (mti) {
            case "0100": return "Authorization Request";
            case "0110": return "Authorization Response";
            case "0120": return "Authorization Advice";
            case "0121": return "Authorization Advice Repeat";
            case "0130": return "Authorization Advice Response";
            case "0200": return "Financial Transaction Request";
            case "0210": return "Financial Transaction Response";
            case "0220": return "Financial Transaction Advice";
            case "0221": return "Financial Transaction Advice Repeat";
            case "0230": return "Financial Transaction Advice Response";
            case "0400": return "Reversal Request";
            case "0410": return "Reversal Response";
            case "0420": return "Reversal Advice";
            case "0421": return "Reversal Advice Repeat";
            case "0430": return "Reversal Advice Response";
            case "0600": return "Administrative Request";
            case "0610": return "Administrative Response";
            case "0620": return "Administrative Advice";
            case "0630": return "Administrative Advice Response";
            case "0800": return "Network Management Request";
            case "0810": return "Network Management Response";
            case "0820": return "Network Management Advice";
            default:
                char cls = mti.charAt(1);
                switch (cls) {
                    case '1': return "Authorization Message";
                    case '2': return "Financial Message";
                    case '4': return "Reversal Message";
                    case '6': return "Administrative Message";
                    case '8': return "Network Management Message";
                    default:  return "ISO 8583 Message";
                }
        }
    }
}
