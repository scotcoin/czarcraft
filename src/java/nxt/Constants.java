package nxt;

import java.util.Calendar;
import java.util.TimeZone;

public final class Constants {

	public static final boolean isTestnet = Nxt.getBooleanProperty("nxt.isTestnet");
	
	public static final int BLOCK_HEADER_LENGTH = 232;
    public static final int MAX_NUMBER_OF_TRANSACTIONS = 1023;
    public static final int MAX_PAYLOAD_LENGTH = MAX_NUMBER_OF_TRANSACTIONS * 176;
    public static final long MAX_BALANCE_NXT = 5000000000L;
    public static final long ONE_NXT = 100000000;
    public static final long MAX_BALANCE_NQT = MAX_BALANCE_NXT * ONE_NXT;
    public static final long INITIAL_BASE_TARGET = 153722867;
    public static final long MAX_BASE_TARGET = MAX_BALANCE_NXT * INITIAL_BASE_TARGET;

    public static final int MAX_ALIAS_URI_LENGTH = 1000;
    public static final int MAX_ALIAS_LENGTH = 100;

    public static final int MAX_ARBITRARY_MESSAGE_LENGTH = 1000;
    public static final int MAX_ENCRYPTED_MESSAGE_LENGTH = 1000;

    public static final int MAX_ACCOUNT_NAME_LENGTH = 100;
    public static final int MAX_ACCOUNT_DESCRIPTION_LENGTH = 1000;

    public static final long MAX_ASSET_QUANTITY_QNT = 1000000000L * 100000000L;
    public static final long ASSET_ISSUANCE_FEE_NQT = isTestnet ? 1000 * ONE_NXT : 10000 * ONE_NXT;
    public static final int MIN_ASSET_NAME_LENGTH = 3;
    public static final int MAX_ASSET_NAME_LENGTH = 10;
    public static final int MAX_ASSET_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_ASSET_TRANSFER_COMMENT_LENGTH = 1000;

    public static final int MAX_POLL_NAME_LENGTH = 100;
    public static final int MAX_POLL_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_POLL_OPTION_LENGTH = 100;
    public static final int MAX_POLL_OPTION_COUNT = 100;

    public static final int MAX_DGS_LISTING_QUANTITY = 1000000000;
    public static final int MAX_DGS_LISTING_NAME_LENGTH = 100;
    public static final int MAX_DGS_LISTING_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_DGS_LISTING_TAGS_LENGTH = 100;
    public static final int MAX_DGS_GOODS_LENGTH = 10240;

    public static final int MAX_HUB_ANNOUNCEMENT_URIS = 100;
    public static final int MAX_HUB_ANNOUNCEMENT_URI_LENGTH = 1000;
    public static final long MIN_HUB_EFFECTIVE_BALANCE = 100000;
    
    public static final int GENESIS_FORGING_BLOCK = isTestnet ? Integer.MAX_VALUE : 2880;
    public static final boolean isOffline = Nxt.getBooleanProperty("nxt.isOffline");
    
    public static final int TRANSPARENT_FORGING_BLOCK = 0;
    public static final int TRANSPARENT_FORGING_BLOCK_7 = Integer.MAX_VALUE;
    public static final int TRANSPARENT_FORGING_BLOCK_8 = isTestnet ? 41000 : Integer.MAX_VALUE;
    public static final int NQT_BLOCK = 0;
    public static final int ASSET_EXCHANGE_BLOCK = 0;
    public static final int REFERENCED_TRANSACTION_FULL_HASH_BLOCK = isTestnet ? 0 : 1000;
    public static final int VOTING_SYSTEM_BLOCK = isTestnet ? 0 : Integer.MAX_VALUE;
    public static final int DIGITAL_GOODS_STORE_BLOCK = isTestnet ? 41000 : 89000;
    public static final int PUBLIC_KEY_ANNOUNCEMENT_BLOCK = Integer.MAX_VALUE; //never forced

    static final long UNCONFIRMED_POOL_DEPOSIT_NQT = (isTestnet ? 50 : 100) * ONE_NXT;

    public static final long EPOCH_BEGINNING;
    static {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.YEAR, 2014);
        calendar.set(Calendar.MONTH, isTestnet ? Calendar.MAY : Calendar.JUNE);
        calendar.set(Calendar.DAY_OF_MONTH, isTestnet ? 22 : 05);
        calendar.set(Calendar.HOUR_OF_DAY, isTestnet ? 22 : 06);
        calendar.set(Calendar.MINUTE, isTestnet ? 0 : 21);
        calendar.set(Calendar.SECOND, isTestnet ? 0 :19);
        calendar.set(Calendar.MILLISECOND, isTestnet ? 0 :83);
        EPOCH_BEGINNING = calendar.getTimeInMillis();
    }

    public static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyz";

    public static final int EC_RULE_TERMINATOR = 600; /* cfb: This constant defines a straight edge when "longest chain"
                                                        rule is outweighed by "economic majority" rule; the terminator
                                                        is set as number of seconds before the current time. */

    public static final int EC_BLOCK_DISTANCE_LIMIT = 60;

    private Constants() {} // never

}
