/***************************************************************************************************
How to use:
* Copy this java file into your nxt/src/java/nxt folder
* Update the addTx section to fund which ever addresses you wish
* run ./compile.sh
* run java -cp nxt.jar nxt.GenesisCreator
  This will output all the variables required for a new genesis block
* Update the Genesis.java that came with the NRS with the variables outputted above
* run ./compile.sh again

Notes
=====
Although this will generate a valid GenesisBlock, NXT will not be able to generate a valid blockchain
unless the checkpoints and transparent forging start points are removed.  Thats what the patch file 
will do. Patch NXT and compile to allow a clean blockchain to start start building.

***************************************************************************************************/
package nxt;

import nxt.util.Logger;
import nxt.crypto.Crypto;
import nxt.crypto.Curve25519;
import nxt.util.Convert;

import java.security.MessageDigest;
import java.util.SortedMap;
import java.util.TreeMap;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

class GenesisCreator {

	public static final String secretPhrase = "All these worlds are yours – except Europa. Attempt no landing there. Use them together. Use them in peace.";
	public static SortedMap<Long,TransactionImpl> transactionsMap = new TreeMap<>();
	public static byte[] pubkey;
		
    public static void main(String[] args) {
		try {			
			pubkey = Crypto.getPublicKey(secretPhrase);	
			
			
			 Long accountIdLong = Convert.fullHashToId(pubkey);
			 System.out.println("PubKey hash: " + Convert.toHexString(pubkey));
			 System.out.println("PubKey Long: " + accountIdLong);
			 System.out.println("PubKey human:" + Convert.toUnsignedLong(accountIdLong));		     
			
			addTx("3705364957971254799",1000000000);
			addTx("2766217825807328998",1000000000);
			addTx("2330184721294966748",1000000000);
			addTx("2752264569666051083",1000000000);
			addTx("13353854314109802469",1000000000);
	

			MessageDigest digest = Crypto.sha256();
			long total_amount = 0;
            for (Transaction tx : transactionsMap.values()) {
                digest.update(tx.getBytes());
				total_amount += tx.getAmountNQT();
            }
			
			System.out.println("Total Amount: " + total_amount);
			
			byte[] payloadHash = digest.digest();
			
			//calculate block signature
			byte[] block_signature = new byte[64];
			
			ByteBuffer block_buffer = ByteBuffer.allocate(4 + 4 + 8 + 8 + 8 + 4 + 4 + 32 + 32 + (32 + 32) + 64);
			block_buffer.order(ByteOrder.LITTLE_ENDIAN);
			block_buffer.putInt(-1); //version
			block_buffer.putInt(0); //timestamp
			block_buffer.putLong(0); //previous block id
			block_buffer.putLong(transactionsMap.size()); //transaction size
			block_buffer.putLong(total_amount); //totalAmount
			block_buffer.putInt(0); //totalFee
			block_buffer.putInt(transactionsMap.size() * 160); //payloadLength
			block_buffer.put(payloadHash); //payloadHash
			block_buffer.put(pubkey); //generatorPublicKey
			block_buffer.put(new byte[64]); //generationSignature
			block_buffer.put(block_signature); //blocksignature
		
			byte[] data = block_buffer.array();       
			byte[] data2 = new byte[data.length - 64];
			System.arraycopy(data, 0, data2, 0, data2.length);
			block_signature = Crypto.sign(data2, secretPhrase);
			
			System.out.print("Block Signature: ");
			for (int a=0;a<block_signature.length;a++)
				System.out.print(block_signature[a] + ", ");
			System.out.println("");

            BlockImpl genesisBlock = new BlockImpl(-1, 0, null, total_amount, 0, transactionsMap.size() * 160, payloadHash,
                    pubkey, new byte[64], block_signature, null, new ArrayList<>(transactionsMap.values()));

            genesisBlock.setPrevious(null);
			System.out.println("Block ID: " + genesisBlock.getId());
			
			System.out.println("Done");
			
		} catch (NxtException.ValidationException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
	
	public static TransactionImpl addTx(String acct, long amount) {
	
			Long rx_acct = (new BigInteger(acct)).longValue();
	
			byte zero_byte = (byte)0;
			byte[] signature = new byte[64];
			ByteBuffer buffer = ByteBuffer.allocate(160);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			buffer.put(zero_byte); //1 
			buffer.put(zero_byte); //1
			buffer.putInt(zero_byte); //4
			buffer.putShort(zero_byte); //2
			buffer.put(pubkey); //32
			buffer.putLong(Convert.nullToZero(rx_acct)); //8
			buffer.putLong(amount); //8
			buffer.putLong(zero_byte); //8
			//buffer.putLong(zero_byte);
			buffer.put(new byte[32]); //32
			buffer.put(signature); //64
			
			signature = Crypto.sign(buffer.array(), secretPhrase);
	
			System.out.print("TX Signature: ");
			for (int a=0;a<signature.length;a++)
				System.out.print(signature[a] + ", ");
			System.out.println("");
				
			TransactionImpl transaction = null;
			try {
				//create the genesis txfr
				transaction = new TransactionImpl(TransactionType.Payment.ORDINARY, 0, (short) 0, pubkey,
						rx_acct, amount, 0, null, signature);			
			}
			catch (Exception ex) {}
			
			transactionsMap.put(transaction.getId(), transaction);
			
			return transaction;
	}
}
