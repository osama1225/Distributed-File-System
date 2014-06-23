import java.io.Serializable;

public class ReplicaLoc implements Serializable {

	private String address;
	private int port;

	public ReplicaLoc(String address, int port) {
		this.address = address;
		this.port = port;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}
}
