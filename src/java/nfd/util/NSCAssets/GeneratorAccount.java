package nfd.util.NSCAssets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import nxt.Appendix.Message;
import nxt.Attachment.ColoredCoinsAssetTransfer;
import nxt.util.Convert;

public class GeneratorAccount {

	private Long accountID;
	private HashSet<Long> transferredAssetsBlockIds = new HashSet<Long>();
	private HashSet<Long> generatorBlockIds = new HashSet<Long>();
	private Long totalQuantity = 0L;

	public GeneratorAccount(Long accountID) {
		this.accountID = accountID;
	}

	public boolean addTransaction(ColoredCoinsAssetTransfer assetTransfer, Message message) {

		if (assetTransfer != null) {

			if (!assetTransfer.getAssetId().equals(NSCAssetTransfer.NSC_ASSET_ID))
				return false;

			totalQuantity += assetTransfer.getQuantityQNT();

			String comment;
			if (message == null) {
				comment = assetTransfer.getComment();
			} else {
				comment = Convert.toString(message.getMessage());
			}

			try {

				String[] blockIds = comment.split(";");
				for (int i = 0; i < blockIds.length; i++) {
					this.transferredAssetsBlockIds.add(Long.parseLong(blockIds[i]));
				}

			} catch (NumberFormatException nfe) {
				return false;
			}

			return true;
		}
		return false;
	}

	public String toString() {
		return (Convert.rsAccount(accountID) + "	" + totalQuantity + "	" + transferredAssetsBlockIds.toString());
	}

	public void addBlockGeneratorId(Long blockId) {
		generatorBlockIds.add(blockId);
	}

	public HashSet<Long> getOutstandingBlockIds() {
		@SuppressWarnings("unchecked")
		HashSet<Long> newGeneratorBlockIds = (HashSet<Long>) generatorBlockIds.clone();
		newGeneratorBlockIds.removeAll(transferredAssetsBlockIds);
		return newGeneratorBlockIds;
	}

	public ArrayList<TransferMessage> getBlockIdsForMessage() {
		ArrayList<TransferMessage> transferMessages = new ArrayList<TransferMessage>();

		StringBuffer message = new StringBuffer();
		int count = 0;
		for (Iterator<Long> iterator = getOutstandingBlockIds().iterator(); iterator.hasNext();) {
			Long blockId = iterator.next();
			if (!(message.length() + blockId.toString().length() + 1 < nxt.Constants.MAX_ARBITRARY_MESSAGE_LENGTH)) {
				TransferMessage transferMessage = new TransferMessage(message.toString(), count);
				transferMessages.add(transferMessage);
				message = new StringBuffer();
				count=0;
			}
			message.append(blockId.toString()+";");
			count++;
		}
		
		if (message.length()>0) {
			TransferMessage transferMessage = new TransferMessage(message.toString(), count);
			transferMessages.add(transferMessage);
		}
		
		return transferMessages;
	}

	public int getTransferedAssetsCount() {
		return generatorBlockIds.size() - getOutstandingBlockIds().size();
	}

	public Long getAccountId(){
		return accountID;
	}
	
	public String getRSAccountId(){
		return Convert.rsAccount(accountID);
	}
	
	public Long getTotalQuantity(){
		return totalQuantity;
	}
	
	public boolean isBlacklisted(){
		return NSCAssetTransfer.ASSET_TRANSFER_ACCOUNT_BLACKLIST.contains(getRSAccountId());
	}
}
