package tzr.util.NSCAssets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import nxt.Appendix;
import nxt.Appendix.Message;
import nxt.Attachment.ColoredCoinsAssetTransfer;
import nxt.Block;
import nxt.BlockImpl;
import nxt.Constants;
import nxt.Db;
import nxt.Nxt;
import nxt.TransactionImpl;
import nxt.TransactionType;
import nxt.util.Convert;
import nxt.util.DbIterator;
import nxt.util.DbUtils;
import nxt.util.Logger;

public class NSCAssetTransfer {

	private static final boolean isTestnet = nxt.Constants.isTestnet;
	private static final int START_HEIGHT = (isTestnet ? 70000 : 85000);
	public static final Long NSC_ASSET_ID = (isTestnet ? 8522997021956756245L : 8537833860395844488L);
	public static final String NSC_SENDER_ID = (isTestnet ? "TZR-C7Z7-RWVF-9MAG-DDMNL" : "TZR-BM65-Z57M-LGNK-5HCMK");
	static final HashSet<String> ASSET_TRANSFER_ACCOUNT_BLACKLIST = new HashSet<String>(Arrays.asList(
			"TZR-YJP3-32TT-P88Q-22222", "TZR-MHPL-4NQF-WCE6-22222", "TZR-FMFQ-4U9B-ZNN3-22222",
			"TZR-EUFT-4FJS-Z5KU-22222", "TZR-H98Z-39P5-4GX8-22222", "TZR-UY7N-VCUH-U5QU-B724B",
			"TZR-BM65-Z57M-LGNK-5HCMK", "TZR-9PHZ-HTPL-JVBR-AB73P", NSC_SENDER_ID,
			(isTestnet ? "Genesis not blacklisted" : "TZR-WLU9-3ARR-65S6-58VUF")));

	private static final String CMD_ARGUMENT_FORCE = "--forceDb";
	private static final String CMD_ARGUMENT_HTTP = "--http";
	private static final String CMD_ARGUMENT_DRYRUN = "--dry-run";

	private static String dbUrl;
	private static String secret;

	private static boolean isHttpsConnection = true;
	private static boolean isDryRun = false;

	public static void main(String[] args) {

		HashMap<Long, GeneratorAccount> generatorAccounts = new HashMap<Long, GeneratorAccount>();
		int blockChainHeight = 0;

		processCmdArguments(args);
		init();

		Connection dbCon = null;
		try {
			dbCon = Db.getConnection();

			PreparedStatement pstmt = dbCon.prepareStatement("select max(height) as height from block;");
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				blockChainHeight = rs.getInt("height");
			}
			rs.close();

			pstmt = dbCon.prepareStatement("select * from block where height>=" + START_HEIGHT
					+ " order by height desc;");

			try (DbIterator<? extends Block> iterator = Nxt.getBlockchain().getBlocks(dbCon, pstmt)) {
				while (iterator.hasNext()) {
					BlockImpl block = (BlockImpl) iterator.next();

					// reward after at least 1440 confirmations
					if (block.getHeight() < blockChainHeight - 1440) {
						GeneratorAccount generatorAccount;
						if (generatorAccounts.containsKey(block.getGeneratorId())) {
							generatorAccount = generatorAccounts.get(block.getGeneratorId());
						} else {
							generatorAccount = new GeneratorAccount(block.getGeneratorId());
						}
						generatorAccount.addBlockGeneratorId(block.getId());
						generatorAccounts.put(block.getGeneratorId(), generatorAccount);
					}

					for (TransactionImpl transaction : block.getTransactions()) {

						if (transaction.getAttachment().getTransactionType() == TransactionType.findTransactionType(
								TransactionType.TYPE_COLORED_COINS,
								TransactionType.SUBTYPE_COLORED_COINS_ASSET_TRANSFER)
								&& Convert.rsAccount(transaction.getSenderId()).equals(NSC_SENDER_ID)) {

							nxt.Attachment.ColoredCoinsAssetTransfer assetTransfer = null;
							nxt.Appendix.Message message = null;

							for (Appendix appendage : transaction.getAppendages()) {

								if (appendage instanceof nxt.Attachment.ColoredCoinsAssetTransfer) {
									assetTransfer = (ColoredCoinsAssetTransfer) appendage;
								}

								if (appendage instanceof nxt.Appendix.Message) {
									message = (Message) appendage;
								}

								if (assetTransfer != null
										&& assetTransfer.getAssetId().equals(NSC_ASSET_ID)
										&& (block.getHeight() < nxt.Constants.DIGITAL_GOODS_STORE_BLOCK || (block
												.getHeight() >= nxt.Constants.DIGITAL_GOODS_STORE_BLOCK && message != null))) {
									GeneratorAccount assetRecipient;
									if (generatorAccounts.containsKey(transaction.getRecipientId())) {
										assetRecipient = generatorAccounts.get(transaction.getRecipientId());
									} else {
										assetRecipient = new GeneratorAccount(transaction.getRecipientId());
									}
									if (assetRecipient.addTransaction(assetTransfer, message))
										generatorAccounts.put(transaction.getRecipientId(), assetRecipient);
								}
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			DbUtils.close(dbCon);
			throw new RuntimeException(e.toString(), e);
		}

		// statistic
		int transferredAssetCount = 0;
		HashSet<String> differentAccounts = new HashSet<String>();

		for (Iterator<Long> iterator = generatorAccounts.keySet().iterator(); iterator.hasNext();) {
			Long generatorId = iterator.next();
			GeneratorAccount generatorAccount = generatorAccounts.get(generatorId);

			Logger.logInfoMessage(generatorAccount.getRSAccountId()
					+ (generatorAccount.isBlacklisted() ? " blacklisted" : "") + " Pending: "
					+ generatorAccount.getOutstandingBlockIds().size() + " Blocks rewarded: "
					+ generatorAccount.getCountOfRewardedBlocks() + " Assets sent total: " + generatorAccount.getTotalQuantity());

			for (TransferMessage transferMessage : generatorAccount.getBlockIdsForMessage()) {
				if (!generatorAccount.isBlacklisted()) {

					Logger.logDebugMessage(">>> " + generatorAccount.getRSAccountId() + " quantity="
							+ transferMessage.getQuantity() + " blockIds=" + transferMessage.getMessage());

					StringBuffer url = new StringBuffer();
					url.append((isHttpsConnection ? "https" : "http"));
					url.append("://localhost:");
					url.append((isTestnet ? "9876" : Nxt.getIntProperty("nxt.apiServerPort")));
					url.append("/nxt?requestType=transferAsset");
					url.append("&secretPhrase=" + secret);
					url.append("&comment=" + transferMessage.getMessage());
					url.append("&recipient=" + Convert.toUnsignedLong(generatorAccount.getAccountId()));
					url.append("&asset=" + NSC_ASSET_ID);
					url.append("&quantityQNT=" + transferMessage.getQuantity());
					url.append("&feeNQT=100000000&deadline=100");

					StringBuffer buffer;
					if (!isDryRun) {
						buffer = APIRequest.sendRequest(url.toString());
					} else {
						buffer = new StringBuffer("mode --dry-run: sending of transactions is disabled");
					}

					if (buffer != null) {
						if (buffer.indexOf("\"errorCode\"") == -1) {
							transferredAssetCount += transferMessage.getQuantity();
							differentAccounts.add(generatorAccount.getAccountId().toString());
							Logger.logInfoMessage("Transferred " + transferMessage.getQuantity()
									+ " assets to account " + generatorAccount.getRSAccountId());
							Logger.logDebugMessage(buffer.toString());
						} else {
							Logger.logErrorMessage(buffer.toString());
						}
					}
				}
			}
		}

		Logger.logInfoMessage("In total " + transferredAssetCount + " assets transferred to "
				+ differentAccounts.size() + " different accounts.");

		shutdown();
	}

	public static void init() {
		Db.init();
	}

	public static void shutdown() {
		Db.shutdown();
	}

	private static void processCmdArguments(String[] args) {

		final HashSet<String> parameters = new HashSet<String>(Arrays.asList(args));

		dbUrl = Constants.isTestnet ? Nxt.getStringProperty("nxt.testDbUrl") : Nxt.getStringProperty("nxt.dbUrl");

		// stand alone h2 server or a fresh copy of tzr_db/ is recommended
		if (!dbUrl.toLowerCase().startsWith("jdbc:h2:tcp:") && !parameters.contains(CMD_ARGUMENT_FORCE)) {
			Logger.logErrorMessage("It's not recommended to use the same database directory as the tzr client!");
			Logger.logErrorMessage("If you know what you are doing force it with parameter --forceDb.");
			System.exit(1);
		}

		parameters.remove(CMD_ARGUMENT_FORCE);

		// default is https
		if (parameters.contains(CMD_ARGUMENT_HTTP)) {
			isHttpsConnection = false;
			parameters.remove(CMD_ARGUMENT_HTTP);
		}
		Logger.logInfoMessage("isHttpsConnection=" + new Boolean(isHttpsConnection).toString());

		// default is false
		if (parameters.contains(CMD_ARGUMENT_DRYRUN)) {
			isDryRun = true;
			parameters.remove(CMD_ARGUMENT_DRYRUN);
		}
		Logger.logInfoMessage("isDryRun=" + new Boolean(isDryRun).toString());

		secret = null;

		if (parameters.isEmpty() || parameters.size() > 1) {
			Logger.logErrorMessage("Wrong count of parameters, abort!");
			Logger.logErrorMessage("Possible command arguments are --forceDb --http --dry-run secret");
			Logger.logErrorMessage("default configuration: db h2 tcp server, https connection, post requests, localhost");
			System.exit(1);
		} else {
			for (String sec : parameters) {
				// only one parameter left
				secret = sec;
			}
		}

	}

}
