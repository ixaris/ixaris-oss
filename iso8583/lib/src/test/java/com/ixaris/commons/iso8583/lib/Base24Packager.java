/*
 * Copyright 2002 - 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.ixaris.commons.iso8583.lib;

import static com.ixaris.commons.iso8583.lib.FieldContentCoding.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Packager for BASE24 messages. Takes care of assembling a message byte stream and disassembling a byte stream into a message object.
 *
 * @author Brian Vella
 *     <p>BASE 24 Message Format
 *     <pre>
 * ------
 * HEADER
 * ------
 *
 * Always starts with literal (ISO)
 *
 * Position    Length      Description
 * -----------------------------------------------------------------------------------------
 *   1-2         2         Product Indicator
 *                         -----------------
 *                         (00) Base (network management messages)
 *                         (02) BASE24-pos
 *
 *   3-4         2         Release Number
 *                         --------------
 *                         (60) 6.0
 *
 *   5-7         3         Status
 *                         ------
 *                         (000) Normal
 *                         (XYZ) where 1 <= XYZ <= 128
 *                               Indicated bad data in field XYZ
 *                         (196) Key synchronization error
 *                         (197) Invalid MAC error
 *                         (198) Security operation failed
 *                         (199) Security device failure
 *
 *   8           1         Originator
 *                         ----------
 *                         (0) Undetermined
 *                         (1) Device controlled by a BASE24 process
 *                         (2) Device Handler process
 *                         (3) Authorization process
 *                         (4) ISO Host Interface process
 *                         (5) Host
 *                         (6) Interchange Interface process or remote banking
 *                             standard unit interface process
 *                         (7) Interchange or remote banking endpoint device
 *                         (8) Scheduled Transaction Initiator process
 *                         (9) XPNET Billpay Server process
 *
 *   9           1         Responder
 *                         ---------
 *                         same as Originator
 *
 *   10-13       4         Message Type Indicator
 *                         ----------------------
 *                         (0200) Financial Transaction Request
 *                         (0210) Financial Transaction Request Response
 *                         (0800) Network Management Request
 *                         (0810) Network Management Request Response
 *                         refer to baemsg.pdf (Section 2) for full list message type indicator codes
 *
 *   14-29       16        Primary Bit Map
 *                         ---------------
 *                         (XXXXXXXXXXXXXXXX) HEX value
 *                         When converted to binary indicates which of fields 1-64
 *                         will be present in the message (0 - field absent, 1 - field present)
 *
 *                         e.g.
 *                         8F00000000000000 becomes binary
 *                         1000111100000000000000000000000000000000000000000000000000000000
 *                         meaning fields 1, 5, 6, 7 and 8 will be present in the message
 *
 * -------------
 * FIELD LENGTHS
 * -------------
 *
 * Fixed-Length Data Elements
 * --------------------------
 * Data placed in numeric, fixed-length data elements must be right-justified, with leading zeros.
 * Data placed in all other fixed-length data elements must be leftjustified, with trailing blanks.
 *
 * Variable-Length Data Elements
 * -----------------------------
 * Data placed in variable-length data elements can vary in length from zero positions
 * up to the individual maximum length stated for the data element.
 *
 * The actual length of the data placed in a variable-length data element must always
 * be specified in a fixed-length prefix immediately preceding the data. This fixed
 * length field is known as the field length indicator.
 *
 * For variable-length data elements with a maximum length of less than 100
 * characters, a two-position field length indicator containing the length of the data in
 * the field precedes the data element.
 *
 * For variable-length data elements with a maximum length greater than 99 and less
 * than 1,000 characters, a three-position field length indicator containing the length
 * of the data in the field precedes the data element.
 *
 * These prefixes must be right-justified and zero-filled.
 *
 * e.g.
 * if a variablelength data element could be up to 200 characters, but only seven characters were
 * actually to be loaded into the element, the required fixed-length prefix would be 007. In this case,
 * if the seven characters were 1234567, the entire data element to be included in the external message
 * would be 0071234567-ten positions in all. Had the data element been limited to a maximum of less
 * than 100 characters, the fixed-length prefix would be 07, and the entire data element would be
 * 071234567-nine positions in all.
 *
 * N X    means Numberic of length X
 * AN X   means Alphanumeric of length X
 * ANS X  means Alphanumeric & special characters of length X
 * VN X   means Variable-length Numberic of maximum length X
 * VAN X  means Variable-length Alphanumeric of maximum length X
 * VANS X means Variable-length Alphanumeric & special characters of maximum length X
 *
 * -------------
 * FIELDS 1 - 64
 * -------------
 *
 * Field       Length      Description
 * -----------------------------------------------------------------------------------------
 *   1         AN 16       Secondary Bit Map
 *                         -----------------
 *                         Same as Primary Bit Map but indicates which of fields 65 - 128
 *                         will be present in the message (after fields 1-64)
 *
 *   2         N 19        Primary Account Number
 *                         ----------------------
 *                         optional for 0200/0210
 *
 *   3         N 6         Processing Code
 *                         ---------------
 *                         (02XXXX) Debit adjustment
 *                         (22XXXX) Credit adjustment
 *                         (30XXXX) Balance Enquiry
 *                         (XX30XX) Credit account type
 *                         (XXXX00) No account specified
 *
 *                         refer to baemsg.pdf (Appendix C) for full list processing codes
 *
 *                         mandatory for 0200/0210
 *
 *   4         N 12        Transaction Amount
 *                         ------------------
 *                         Decimalization of the amount is implied by the Transaction Currency Code (field 49)
 *                         000000000000 in case of balance enquiry
 *
 *                         mandatory for 0200/0210
 *
 *   5                     Not used by BASE24
 *
 *   6                     Not used by BASE24
 *
 *   7         N 10        Transmission Date and Time
 *                         --------------------------
 *                         (MMDDhhmmss)
 *
 *                         mandatory for all messages
 *
 *   8                     Not used by BASE24
 *
 *   9                     Not used by BASE24
 *
 *   10                    Not used by BASE24
 *
 *   11        N 6         Systems Trace Audit Number
 *                         --------------------------
 *                         In network management messages, the systems trace audit number is used to match
 *                         the network management request with its response. On outgoing 0810 messages, the
 *                         ISO Host Interface process echoes the number sent in the corresponding 0800 messages.
 *
 *                         mandatory for all messages
 *
 *   12        N 6         Local Transaction Time
 *                         ----------------------
 *                         (hhmmss)
 *
 *                         mandatory for 0200/0210
 *
 *   13        N 4         Local Transaction Date
 *                         ----------------------
 *                         (MMDD)
 *
 *                         mandatory for 0200/0210
 *
 *   14        N 4         Expiration Date
 *                         ---------------
 *                         (YYMM)
 *
 *                         optional
 *
 *   15                    optional
 *
 *   16                    Not used by BASE24
 *
 *   17        N 4         Capture Date
 *                         ------------
 *                         (MMDD)
 *
 *                         mandatory for 0200/0210
 *
 *   18        N 4         Merchant Type
 *                         -------------
 *                         Standard Industrial Classification (SIC) code
 *
 *                         optional
 *
 *   19                    Not used by BASE24
 *
 *   20                    Not used by BASE24
 *
 *   21                    Not used by BASE24
 *
 *   22        N 3         Point of Service Entry Mode
 *                         ---------------------------
 *                         (010) Unknown
 *                         (012) Manual entry, no PIN entry cap.
 *
 *   23                    Not required for message types 0200/0800
 *
 *   24                    Not used by BASE24
 *
 *   25                    optional
 *
 *   26                    Not used by BASE24
 *
 *   27                    optional
 *
 *   28                    Not required for message types 0200/0800
 *
 *   29                    Not used by BASE24
 *
 *   30                    Not used by BASE24
 *
 *   31                    Not used by BASE24
 *
 *   32        VN 11       Acquiring Institution Identification Code
 *                         -----------------------------------------
 *                         013
 *
 *                         mandatory for 0200/0210
 *
 *   33                    optional
 *
 *   34                    Not required for message types 0200/0800
 *
 *   35        VANS 37     Track 2 Data
 *                         ------------
 *                         ( 2  length
 *                           19 primary account number (PAN), left justified
 *                           1  Field separator (=)
 *                           3  Country code (if present)
 *                           4  YYMM Expiration date
 *                           3  Service code (if present)
 *                              Discretionary data )
 *
 *                         mandatory for 0200/0210
 *
 *   36                    Not required for message types 0200/0800
 *
 *   37        AN 12       Retrieval Reference Number
 *                         --------------------------
 *                         mandatory for 0200/0210
 *
 *   38        AN 6        Authorization Identification Response
 *                         -------------------------------------
 *                         mandatory for 0210
 *
 *   39        AN 2        Response Code
 *                         -------------
 *                         for 0810 messages
 *                         (00) Approved
 *                         (05) Denied
 *                         (12) Bad check digits
 *                         (91) DPC down
 *
 *                         for pos messages
 *                         (06) Unknown Card
 *                         (51) Insufficient Funds
 *
 *                         refer to baemsg.pdf (Appendix C) for full list response codes
 *
 *                         mandatory for 0210/0810
 *
 *   40                    Not used by BASE24
 *
 *   41        ANS 16      Card Acceptor Terminal Identification
 *                         -------------------------------------
 *                         mandatory for 0200/0210
 *
 *   42                    optional
 *
 *   43        ANS 40      Card Acceptor Name/Location
 *                         ---------------------------
 *                         (22 owner 13 city 3 state 2 country)
 *                         HTTPS://WWW.IXARIS.COMINTA_XBIEXMLT000ML
 *                         HTTPS://WWW.IXARIS.COM:INTA_XBIEXMLT:000:ML
 *
 *                         mandatory for 0200
 *
 *   44                    Not required for message types 0200/0800
 *
 *   45                    optional
 *
 *   46                    Not used by BASE24
 *
 *   47                    Not used by BASE24
 *
 *   48        ANS 30      Retailer Data
 *                         -------------
 *                         ( 3  length
 *                           19 retailer
 *                           4  group
 *                           4 region )
 *                         027___________________00010001
 *                         027:___________________:0001:0001
 *
 *                         mandatory for 0200/0210
 *
 *   49        N 3         Transaction Currency Code
 *                         -------------------------
 *                         (826) GBP
 *                         (840) USD
 *
 *                         mandatory for 0200/0210
 *
 *   50                    Not used by BASE24
 *
 *   51                    Not used by BASE24
 *
 *   52                    optional
 *
 *   53                    Not required for message types 0200/0800
 *
 *   54                    optional
 *
 *   55                    Not used by BASE24
 *
 *   56                    Not used by BASE24
 *
 *   57                    Not used by BASE24
 *
 *   58                    Not required for message types 0200/0800
 *
 *   59                    Not required for message types 0200/0800
 *
 *   60        ANS 19      Terminal Data
 *                         -------------
 *                         ( 3  length
 *                           4  owner FIID
 *                           4  logical network
 *                           4  time offset
 *                           4  pseudo terminal ID )
 *                         016IXA ____00000000
 *                         016:IXA :____:0000:0000
 *
 *                         mandatory for 0200/0210
 *
 *   61        ANS 22      Card Issuer-Category-Response Code Data
 *                         ---------------------------------------
 *                         ( 3  length
 *                           4  issuer FIID
 *                           4  logical network
 *                           1  category
 *                           2  save account indicator
 *                           8  interchange response code )
 *                         019IXA ____03100000000
 *                         019:IXA :____:0:31:00000000
 *
 *                         mandatory for 0200/0210
 *
 *   62        ANS 13      Postal Code
 *                         -----------
 *                         ( 3  length
 *                           10 post code )
 *                         010__________
 *                         010:__________
 *
 *                         optional
 *
 *   63        VANS 600    Additional Data
 *                         ---------------
 *                         If CVV2 is to be passed, this goes here
 *                         e.g.
 *                         057& 0000200048! C000026 999     87120     7  1 1
 *                         057:& 0000200048:! C000026 :999     87120     7  1 1
 *
 *   64        AN 16       Primary Message Authentication Code
 *                         -----------------------------------
 *                         Not being used. If field 128 is present, this one is not.
 *
 * ---------------
 * FIELDS 65 - 128
 * ---------------
 *
 * Field       Length      Description
 * ----------------------------------------------------------------
 *   65                    Not used by BASE24
 *
 *   66                    Not used by BASE24
 *
 *   67                    Not used by BASE24
 *
 *   68                    Not used by BASE24
 *
 *   69                    Not used by BASE24
 *
 *   70        N 3         Network Management Information Code
 *                         -----------------------------------
 *                         (001) Logon
 *                         (002) Logoff
 *                         (301) Echo-test
 *
 *                         mandatory for 0800/0810
 *
 *   71                    Not used by BASE24
 *
 *   72                    Not used by BASE24
 *
 *   73                    Not required for message types 0200/0800
 *
 *   74                    Not used by BASE24
 *
 *   75                    Not used by BASE24
 *
 *   76                    Not used by BASE24
 *
 *   77                    Not used by BASE24
 *
 *   78                    Not used by BASE24
 *
 *   79                    Not used by BASE24
 *
 *   80                    Not used by BASE24
 *
 *   81                    Not used by BASE24
 *
 *   82                    Not used by BASE24
 *
 *   83                    Not used by BASE24
 *
 *   84                    Not used by BASE24
 *
 *   85                    Not used by BASE24
 *
 *   86                    Not used by BASE24
 *
 *   87                    Not used by BASE24
 *
 *   88                    Not used by BASE24
 *
 *   89                    Not used by BASE24
 *
 *   90        N 42        Original Data Elements
 *                         ----------------------
 *                         optional for 0200 adjustment
 *
 *   91                    Not required for message types 0200/0800
 *
 *   92                    Not used by BASE24
 *
 *   93                    Not used by BASE24
 *
 *   94                    Not used by BASE24
 *
 *   95        AN 42       Replacement Amounts
 *                         -------------------
 *                         optional for 0200 adjustment
 *
 *   96                    Not used by BASE24
 *
 *   97                    Not used by BASE24
 *
 *   98                    Not required for message types 0200/0800
 *
 *   99                    Not used by BASE24
 *
 *   100       VN 11       Receiving Institution Identification Code
 *                         -----------------------------------------
 *                         011
 *
 *                         mandatory for 0200/0210
 *
 *   101                   Not required for message types 0200/0800
 *
 *   102       VANS 28     Account Identification 1
 *                         ------------------------
 *                         optional for 0210
 *
 *   103                   Not required for message types 0200/0800
 *
 *   104                   Not required for message types 0200/0800
 *
 *   105                   Not used by BASE24
 *
 *   106                   Not used by BASE24
 *
 *   107                   Not used by BASE24
 *
 *   108                   Not used by BASE24
 *
 *   109                   Not used by BASE24
 *
 *   110                   Not used by BASE24
 *
 *   111                   Not used by BASE24
 *
 *   112                   Not required for message types 0200/0800
 *
 *   113                   Not used by BASE24
 *
 *   114                   Not required for message types 0200/0800
 *
 *   115                   Not required for message types 0200/0800
 *
 *   116                   Not required for message types 0200/0800
 *
 *   117                   Not required for message types 0200/0800
 *
 *   118                   Not required for message types 0200/0800
 *
 *   119                   Not required for message types 0200/0800
 *
 *   120                   optional
 *
 *   121       ANS 23      Authorization Indicators
 *                         ------------------------
 *                         ( 3  length
 *                           6  clerk ID
 *                           4  CRT authorization group
 *                           8  CRT authorization user ID
 *                           1  auth indicator
 *                           1  auth indicator 2 )
 *                         020000000000000000000P0
 *                         020:000000:0000:00000000:P:0
 *
 *                         mandatory for 0200/0210
 *
 *   122                   Not required for message types 0200/0800
 *
 *   123                   optional
 *
 *   124       ANS 12      Batch and Shift Data
 *                         --------------------
 *                         ( 3  length
 *                           3  batch sequence number
 *                           3  batch number
 *                           3  shift number )
 *                         009001001000
 *                         009:001:001:000
 *
 *                         mandatory for 0200/0210
 *
 *   125       ANS 15     Settlement Data
 *                        ---------------
 *                         ( 3  length
 *                           2  services
 *                           4  originator
 *                           4  destination
 *                           1  draft capture flag
 *                           1  settlement flag )
 *                         012000000000000
 *                         012:00:0000:0000:0:0
 *
 *                         mandatory for 0200/0210
 *
 *   126       ANS 41      Preauthorization and Chargeback Data
 *                         ------------------------------------
 *                         ( 3  length
 *                           3  preauthorization hold (0=mins, 1=hours, 2=days, 111=11 hours)
 *                           12 preauthorization sequence number
 *                           20 referral phone number
 *                           2  reason for chargeback
 *                           1  number of chargeback )
 *                         03811100000000000000000000000000000000000
 *                         038:111:000000000000:00000000000000000000:00:0
 *
 *                         mandatory for 0200
 *
 *   127       VANS 200    User Data
 *                         ---------
 *                         Contains user-defined information that the message can carry in its internal message,
 *                         but does not recognize and does not use for processing.
 *
 *   128       AN 16       Secondary Message Authentication Code
 *                         -------------------------------------
 *                         Not being used.
 * </pre>
 */
public final class Base24Packager extends ISOPackager {
    
    /**
     * The End-Of-Message single-byte BASE24 message frame trailer.
     */
    public static final byte EOM = 3;
    
    private static final Base24Packager INSTANCE = new Base24Packager();
    
    public static Base24Packager getInstance() {
        return INSTANCE;
    }
    
    private Base24Packager() {
        super(MtiType.ASCII,
            VarFieldLengthType.ASCII,
            StandardCharsets.UTF_8,
            /* 000 */ new ISOFixedFieldSpec(ASCII, 16, "Primary Bit map"),
            /* 001 */ new ISOFixedFieldSpec(ASCII, 16, "Secondary Bit map"),
            /* 002 */ new ISOFixedFieldSpec(ASCII, 19, "Primary Account Number"),
            /* 003 */ new ISOFixedFieldSpec(ASCII, 6, "Processing Code"),
            /* 004 */ new ISOFixedFieldSpec(ASCII, 12, "Transaction Amount"),
            /* 005 */ new ISOUnusedFieldSpec(),
            /* 006 */ new ISOUnusedFieldSpec(),
            /* 007 */ new ISOFixedFieldSpec(ASCII, 10, "Transmission Date and Time"),
            /* 008 */ new ISOUnusedFieldSpec(),
            /* 009 */ new ISOUnusedFieldSpec(),
            /* 010 */ new ISOUnusedFieldSpec(),
            /* 011 */ new ISOFixedFieldSpec(ASCII, 6, "Systems Trace Audit Number"),
            /* 012 */ new ISOFixedFieldSpec(ASCII, 6, "Local Transaction Time"),
            /* 013 */ new ISOFixedFieldSpec(ASCII, 4, "Local Transaction Date"),
            /* 014 */ new ISOFixedFieldSpec(ASCII, 4, "Expiration Date"),
            /* 015 */ new ISOFixedFieldSpec(ASCII, 4, "Settlement Date"),
            /* 016 */ new ISOUnusedFieldSpec(),
            /* 017 */ new ISOFixedFieldSpec(ASCII, 4, "Capture Date"),
            /* 018 */ new ISOFixedFieldSpec(ASCII, 4, "Merchant Type"),
            /* 019 */ new ISOUnusedFieldSpec(),
            /* 020 */ new ISOUnusedFieldSpec(),
            /* 021 */ new ISOUnusedFieldSpec(),
            /* 022 */ new ISOFixedFieldSpec(ASCII, 3, "Point of Service Entry Mode"),
            /* 023 */ new ISOFixedFieldSpec(ASCII, 3, "Card Sequence Number"),
            /* 024 */ new ISOUnusedFieldSpec(),
            /* 025 */ new ISOFixedFieldSpec(ASCII, 2, "Point of Service Condition Code"),
            /* 026 */ new ISOUnusedFieldSpec(),
            /* 027 */ new ISOFixedFieldSpec(ASCII, 1, "Authorization Identification Response Length"),
            /* 028 */ new ISOFixedFieldSpec(ASCII, 9, "Transaction Fee Amount"),
            /* 029 */ new ISOUnusedFieldSpec(),
            /* 030 */ new ISOUnusedFieldSpec(),
            /* 031 */ new ISOUnusedFieldSpec(),
            /* 032 */ new ISOLLVariableFieldSpec(ASCII, 11, "Acquiring Institution Identification Code"),
            /* 033 */ new ISOLLVariableFieldSpec(ASCII, 11, "Forwarding Institution Identification Code"),
            /* 034 */ new ISOUnusedFieldSpec(),
            /* 035 */ new ISOLLVariableFieldSpec(ASCII, 37, "Track 2 data"),
            /* 036 */ new ISOUnusedFieldSpec(),
            /* 037 */ new ISOFixedFieldSpec(ASCII, 12, "Retrieval Reference Number"),
            /* 038 */ new ISOFixedFieldSpec(ASCII, 6, "Authorization Identification Response"),
            /* 039 */ new ISOFixedFieldSpec(ASCII, 2, "Response Code"),
            /* 040 */ new ISOUnusedFieldSpec(),
            /* 041 */ new ISOFixedFieldSpec(ASCII, 16, "Card Acceptor Terminal Identification"),
            /* 042 */ new ISOFixedFieldSpec(ASCII, 15, "Card Acceptor Identification Code"),
            /* 043 */ new ISOFixedFieldSpec(ASCII, 40, "Card Acceptor Name/Location"),
            /* 044 */ new ISOUnusedFieldSpec(),
            /* 045 */ new ISOLLVariableFieldSpec(ASCII, 76, "Track 1 data"),
            /* 046 */ new ISOUnusedFieldSpec(),
            /* 047 */ new ISOUnusedFieldSpec(),
            /* 048 */ new ISOLLLVariableFieldSpec(ASCII, 27, "Retailer Data"),
            /* 049 */ new ISOFixedFieldSpec(ASCII, 3, "Transaction Currency Code"),
            /* 050 */ new ISOUnusedFieldSpec(),
            /* 051 */ new ISOUnusedFieldSpec(),
            /* 052 */ new ISOFixedFieldSpec(ASCII, 16, "Personal Identification Number (PIN) Data"),
            /* 053 */ new ISOUnusedFieldSpec(),
            /* 054 */ new ISOLLLVariableFieldSpec(ASCII, 12, "BASE24 Additional Amounts"),
            /* 055 */ new ISOUnusedFieldSpec(),
            /* 056 */ new ISOUnusedFieldSpec(),
            /* 057 */ new ISOUnusedFieldSpec(),
            /* 058 */ new ISOUnusedFieldSpec(),
            /* 059 */ new ISOUnusedFieldSpec(),
            /* 060 */ new ISOLLLVariableFieldSpec(ASCII, 16, "Terminal Data"),
            /* 061 */ new ISOLLLVariableFieldSpec(ASCII, 19, "Card Issuer-Category-Response Code Data"),
            /* 062 */ new ISOLLLVariableFieldSpec(ASCII, 10, "Postal Code"),
            /* 063 */ new ISOLLLVariableFieldSpec(ASCII, 600, "BASE24-pos Additional Data"),
            /* 064 */ new ISOFixedFieldSpec(ASCII, 16, "Primary Message Authentication Code"),
            /* 065 */ new ISOUnusedFieldSpec(),
            /* 066 */ new ISOUnusedFieldSpec(),
            /* 067 */ new ISOUnusedFieldSpec(),
            /* 068 */ new ISOUnusedFieldSpec(),
            /* 069 */ new ISOUnusedFieldSpec(),
            /* 070 */ new ISOFixedFieldSpec(ASCII, 3, "Network Management Information Code"),
            /* 071 */ new ISOUnusedFieldSpec(),
            /* 072 */ new ISOUnusedFieldSpec(),
            /* 073 */ new ISOUnusedFieldSpec(),
            /* 074 */ new ISOUnusedFieldSpec(),
            /* 075 */ new ISOUnusedFieldSpec(),
            /* 076 */ new ISOUnusedFieldSpec(),
            /* 077 */ new ISOUnusedFieldSpec(),
            /* 078 */ new ISOUnusedFieldSpec(),
            /* 079 */ new ISOUnusedFieldSpec(),
            /* 080 */ new ISOUnusedFieldSpec(),
            /* 081 */ new ISOUnusedFieldSpec(),
            /* 082 */ new ISOUnusedFieldSpec(),
            /* 083 */ new ISOUnusedFieldSpec(),
            /* 084 */ new ISOUnusedFieldSpec(),
            /* 085 */ new ISOUnusedFieldSpec(),
            /* 086 */ new ISOUnusedFieldSpec(),
            /* 087 */ new ISOUnusedFieldSpec(),
            /* 088 */ new ISOUnusedFieldSpec(),
            /* 089 */ new ISOUnusedFieldSpec(),
            /* 090 */ new ISOFixedFieldSpec(ASCII, 42, "Original Data Elements"),
            /* 091 */ new ISOUnusedFieldSpec(),
            /* 092 */ new ISOUnusedFieldSpec(),
            /* 093 */ new ISOUnusedFieldSpec(),
            /* 094 */ new ISOUnusedFieldSpec(),
            /* 095 */ new ISOFixedFieldSpec(ASCII, 42, "Replacement Amounts"),
            /* 096 */ new ISOUnusedFieldSpec(),
            /* 097 */ new ISOUnusedFieldSpec(),
            /* 098 */ new ISOUnusedFieldSpec(),
            /* 099 */ new ISOUnusedFieldSpec(),
            /* 100 */ new ISOLLVariableFieldSpec(ASCII, 11, "Receiving Institution Identification Code"),
            /* 101 */ new ISOUnusedFieldSpec(),
            /* 102 */ new ISOLLVariableFieldSpec(ASCII, 28, "Account Identification 1"),
            /* 103 */ new ISOUnusedFieldSpec(),
            /* 104 */ new ISOUnusedFieldSpec(),
            /* 105 */ new ISOUnusedFieldSpec(),
            /* 106 */ new ISOUnusedFieldSpec(),
            /* 107 */ new ISOUnusedFieldSpec(),
            /* 108 */ new ISOUnusedFieldSpec(),
            /* 109 */ new ISOUnusedFieldSpec(),
            /* 110 */ new ISOUnusedFieldSpec(),
            /* 111 */ new ISOUnusedFieldSpec(),
            /* 112 */ new ISOUnusedFieldSpec(),
            /* 113 */ new ISOUnusedFieldSpec(),
            /* 114 */ new ISOUnusedFieldSpec(),
            /* 115 */ new ISOUnusedFieldSpec(),
            /* 116 */ new ISOUnusedFieldSpec(),
            /* 117 */ new ISOUnusedFieldSpec(),
            /* 118 */ new ISOUnusedFieldSpec(),
            /* 119 */ new ISOUnusedFieldSpec(),
            /* 120 */ new ISOLLLVariableFieldSpec(ASCII, 29, "Terminal Address-Branch"),
            /* 121 */ new ISOLLLVariableFieldSpec(ASCII, 20, "Authorization Indicators"),
            /* 122 */ new ISOUnusedFieldSpec(),
            /* 123 */ new ISOLLLVariableFieldSpec(ASCII, 20, "Invoice Data"),
            /* 124 */ new ISOLLLVariableFieldSpec(ASCII, 9, "Batch and Shift Data"),
            /* 125 */ new ISOLLLVariableFieldSpec(ASCII, 12, "Settlement Data"),
            /* 126 */ new ISOLLLVariableFieldSpec(ASCII, 38, "Preauthorization and Chargeback Data"),
            /* 127 */ new ISOLLLVariableFieldSpec(ASCII, 200, "User Data"),
            /* 128 */ new ISOFixedFieldSpec(ASCII, 16, "Secondary Message Authentication Code"));
    }
    
    /**
     * Constructs a byte array from a message object <i> Note that the length of the message includes the trailer byte </i>
     *
     * @param m ISOMsg object to be converted to a byte array
     * @param os
     * @throws IOException
     */
    public final void assemble(final Base24Msg m, final OutputStream os) throws IOException {
        
        int len = m.getLength() + 4; // +3 ISO literal +1 EOM byte
        if (m.getHeader() != null) {
            len += m.getHeader().length;
        }
        
        os.write((byte) (len >>> 8));
        os.write((byte) len);
        
        // add the ISO literal
        os.write(new byte[] { 'I', 'S', 'O' });
        
        // add the header
        
        if (m.getHeader() != null) {
            os.write(m.getHeader());
        }
        
        pack(m, os);
        
        os.write(EOM);
    }
    
    /**
     * Constructs a message object from a byte array
     *
     * @param b byte array containing the raw read message
     * @return BASE24Msg object containing the data held in the message
     * @throws IOException
     */
    public final Base24Msg disassemble(final InputStream is) throws IOException {
        
        final DataInputStream dis = new DataInputStream(is);
        
        // Check message length from first 2 bytes & check EOM byte at the end
        final int len = dis.readShort();
        
        // Check for ISO literal in the beginning of the message
        if (!new String(new byte[] { dis.readByte(), dis.readByte(), dis.readByte() }).equals("ISO")) {
            throw new IllegalArgumentException("Invalid Base24 message - ISO literal not found");
        }
        
        // Create a new message object
        final Base24Msg m = new Base24Msg();
        
        // copy the header from the byte stream
        byte[] header = new byte[9];
        dis.readFully(header);
        m.setHeader(header);
        
        // unpack the rest of the message
        unpack(m, is);
        
        if (len != (m.getLength() + header.length + 4)) {
            throw new IllegalArgumentException("Mismatched header length [" + len + "] and message length [" + m.getLength() + "]");
        }
        if (dis.readByte() != EOM) {
            throw new IllegalArgumentException("Invalid Base24 message - EOM not found");
        }
        
        return m;
    }
    
}
