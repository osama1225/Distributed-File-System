import java.rmi.RemoteException;

public class Main {
	public static void main(String[] args) {

		String masterAddress = "169.254.219.17";
		int masterPort = 5656;
		String replicaAddress = "169.254.219.17";
		int replicaPort = 1099;
		// run replicas
		try {
			ReplicaServer server = new ReplicaServer(replicaAddress,
					replicaPort, masterAddress, masterPort);
		} catch (RemoteException e) {
			System.err.println("drb fl replica server");
			e.printStackTrace();
		}
		// run master
		try {
			MasterServer server = new MasterServer(masterAddress, masterPort);
		} catch (Exception e) {
			System.err.println("drb fl server");
			e.printStackTrace();
		}

		// run clients

		try {
			final Client c1 = new Client(masterAddress, masterPort);
			final Client c2 = new Client(masterAddress, masterPort);
			Thread t = new Thread(new Runnable() {

				@Override
				public void run() {
					c1.writeToFile("file1", "client osama", 10, 2);

				}
			});
			Thread t2 = new Thread(new Runnable() {

				@Override
				public void run() {

					c2.writeToFile("file4", "client osama", 40, 1);
				}
			});
			t.start();
			t2.start();
		} catch (Exception e) {
			System.err.println("drb fl client");
			e.printStackTrace();
		}
	}
}
