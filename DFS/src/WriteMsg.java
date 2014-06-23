import java.io.Serializable;

public class WriteMsg implements Serializable {
	private long transactionId;
	private long timeStamp;
	private ReplicaLoc loc;

	public WriteMsg(long transactionId, long timeStamp, ReplicaLoc loc) {
		this.transactionId = transactionId;
		this.timeStamp = timeStamp;
		this.loc = loc;

	}

	public ReplicaLoc getLoc() {
		return loc;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public long getTransactionId() {
		return transactionId;
	}
}
