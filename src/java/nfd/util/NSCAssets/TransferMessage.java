package nfd.util.NSCAssets;

public class TransferMessage {
	
	private int quantity=0;
	
	private String message;
	
	public TransferMessage(String message, int quantity){
		this.message=message;
		this.quantity=quantity;		
		
	}

	public int getQuantity() {
		return quantity;
	}

	public String getMessage() {
		return message;
	}

}
