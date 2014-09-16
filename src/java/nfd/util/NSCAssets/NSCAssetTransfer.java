package nfd.util.NSCAssets;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
	public static final String NSC_SENDER_ID = (isTestnet ? "NFD-C7Z7-RWVF-9MAG-DDMNL" : "NFD-BM65-Z57M-LGNK-5HCMK");
	static final HashSet<String> ASSET_TRANSFER_ACCOUNT_BLACKLIST = new HashSet<String>(Arrays.asList(
			"NFD-YJP3-32TT-P88Q-22222", "NFD-MHPL-4NQF-WCE6-22222", "NFD-FMFQ-4U9B-ZNN3-22222",
			"NFD-EUFT-4FJS-Z5KU-22222", "NFD-H98Z-39P5-4GX8-22222", "NFD-UY7N-VCUH-U5QU-B724B",
			"NFD-BM65-Z57M-LGNK-5HCMK", "NFD-9PHZ-HTPL-JVBR-AB73P", 
			(isTestnet? "Genesis not blacklisted":"NFD-G3GS-J4SW-T2WM-3DDYJ"), NSC_SENDER_ID));
	
	private static final String FORCE = "--force";
	
	public static void main(String[] args) {
		
		HashMap<Long, GeneratorAccount> generatorAccounts = new HashMap<Long, GeneratorAccount>();
		HashMap<Integer, Block> blocks = new HashMap<Integer, Block>();

		final HashSet<String> parameters=new HashSet<String>(Arrays.asList(args));
		 
		
		String dbUrl = Constants.isTestnet ? Nxt.getStringProperty("nxt.testDbUrl") : Nxt.getStringProperty("nxt.dbUrl");
		
		//stand alone h2 server or fresh copy of nfd_db/ is recommended
		if (! dbUrl.toLowerCase().startsWith("jdbc:h2:tcp:") && ! parameters.contains("--force")) {
			Logger.logErrorMessage("It's not recommended to use the same database directory as the nfd client!");
			Logger.logErrorMessage("If you know what you are doing force it with parameter --force.");
			System.exit(1);
		}
		
		parameters.remove(FORCE);

		String secret=null;
		
		if (parameters.isEmpty() || parameters.size()>1) {
			Logger.logErrorMessage("Wrong count of parameters, abort!");
			Logger.logErrorMessage("command --force secret");
			System.exit(1);			
		} else {			
			for (String sec : parameters) {
				// only one parameter left
				secret=sec;				
			}
		}

		init();

		Connection con = null;
		try {
			con = Db.getConnection();
			PreparedStatement pstmt = con
					.prepareStatement("select * from block where height< (SELECT max(heiGHT)  FROM BLOCK) "
							+ (nxt.Constants.isTestnet ? "" : "-1440") + " and height>=" + START_HEIGHT
							+ " order by height desc;");

			try (DbIterator<? extends Block> iterator = Nxt.getBlockchain().getBlocks(con, pstmt)) {
				while (iterator.hasNext()) {
					BlockImpl block = (BlockImpl) iterator.next();
					blocks.put(block.getHeight(), block);

					GeneratorAccount generatorAccount;
					if (generatorAccounts.containsKey(block.getGeneratorId())) {

						generatorAccount = generatorAccounts.get(block.getGeneratorId());
					} else {
						generatorAccount = new GeneratorAccount(block.getGeneratorId());
					}
					generatorAccount.addBlockGeneratorId(block.getId());
					generatorAccounts.put(block.getGeneratorId(), generatorAccount);

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
			DbUtils.close(con);
			throw new RuntimeException(e.toString(), e);
		}
		
		//statistic
		int transferredAssetCount = 0;
		HashSet<String> differentAccounts = new HashSet<String>();
		
		for (Iterator<Long> iterator = generatorAccounts.keySet().iterator(); iterator.hasNext();) {
			Long generatorId = iterator.next();
			GeneratorAccount generatorAccount = generatorAccounts.get(generatorId);
			
			Logger.logDebugMessage(generatorAccount.getRSAccountId() + "	" + generatorAccount.getTotalQuantity()
					+ (generatorAccount.isBlacklisted() ? " blacklisted" : "") + "	Outstanding: "
					+ generatorAccount.getOutstandingBlockIds().size() + "	Done: "
					+ generatorAccount.getTransferedAssetsCount());

			for (TransferMessage transferMessage : generatorAccount.getBlockIdsForMessage()) {
				if (!generatorAccount.isBlacklisted()) {
					
					Logger.logDebugMessage(">>> " + generatorAccount.getRSAccountId() + "\t" + transferMessage.getQuantity()
							+ "\t" + transferMessage.getMessage());

					StringBuffer url = new StringBuffer();
					url.append("https://localhost:" + (isTestnet ? "9876" : Nxt.getIntProperty("nxt.apiServerPort"))); 
					url.append("/nxt?requestType=transferAsset");
					url.append("&secretPhrase=" + secret);
					url.append("&comment=" + transferMessage.getMessage());
					url.append("&recipient=" + generatorAccount.getAccountId());
					url.append("&asset=" + NSC_ASSET_ID);
					url.append("&quantityQNT=" + transferMessage.getQuantity());
					url.append("&feeNQT=100000000&deadline=1440");
					
					StringBuffer buffer = HttpsAPIRequests.sendHTTPSPostRequest(url.toString());
					if (buffer != null) {
						transferredAssetCount += transferMessage.getQuantity();
						differentAccounts.add(generatorAccount.getAccountId().toString());
					}
				}
			}
		}
		
		Logger.logInfoMessage("In total " + transferredAssetCount + " assets transferred to " + differentAccounts.size()
				+ " different accounts.");

		shutdown();
	}

	public static void init() {
		Db.init();
	}

	public static void shutdown() {
		Db.shutdown();
	}

}