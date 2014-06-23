import java.rmi.Naming;

public class Client {

	MasterServerClientInterface stubServer;

	public Client(String serverAddr, int portNo) {

		// initialize RMI
		stubServer = null;
		try {
			/*
			 * if (System.getSecurityManager() == null)
			 * System.setSecurityManager(new SecurityManager());
			 */stubServer = (MasterServerClientInterface) Naming.lookup("//"
					+ serverAddr + ":" + portNo + "/MasterServer");
		} catch (Exception e1) {
			e1.printStackTrace();
			return;
		}
	}

	public void readFile(String fileName) {
		// get replica addresses that contains the file
		try {
			ReplicaLoc[] locations = stubServer.read(fileName);
			if (locations!=null&&locations.length > 0) {
				ReplicaLoc loc = locations[0];// try first replica
				ReplicaServerClientInterface stubRep = null;
				// if (System.getSecurityManager() == null)
				// System.setSecurityManager(new SecurityManager());
				stubRep = (ReplicaServerClientInterface) Naming.lookup("//"
						+ loc.getAddress() + ":" + loc.getPort()
						+ "/ReplicaServer");

				FileContent content = stubRep.read(fileName);
				if (content.getData().equals("notfound-1"))
					System.out.println("File Not Found");
				else {
					System.out.println("Print file content of file: "
							+ fileName);
					System.out.println(content.getData());
				}
			} else
				System.err.println("File Not Found on Master metadata ");
		} catch (Exception e) {
			System.err.println("Error in reading file at client");
			e.printStackTrace();
		}
	}

	public void writeToFile(String fileName, String update, int numOfWrites,
			int flag) {
		FileContent c = new FileContent();
		c.setFileName(fileName);
		c.append(update);
		try {
			WriteMsg info = stubServer.write(c);
			// send the request to the primary replica.
			ReplicaServerClientInterface stubRep = null;
			// if (System.getSecurityManager() == null)
			// System.setSecurityManager(new SecurityManager());
			stubRep = (ReplicaServerClientInterface) Naming.lookup("//"
					+ info.getLoc().getAddress() + ":"
					+ info.getLoc().getPort() + "/ReplicaServer");

			// long msgSeqno = 0;
			for (int i = 0; i < numOfWrites; i++)
				stubRep.write(info.getTransactionId(), i, c);

			switch (flag) {
			case 1:
				// then request to commit
				stubRep.commit(info.getTransactionId(), numOfWrites);
				break;
			case 2:
				// then abort
				stubRep.abort(info.getTransactionId());
				break;
			default:
				// do nothing..
				break;
			}
			System.out.println("transaction with id " + info.getTransactionId()
					+ "finished writing to " + fileName);

		} catch (Exception e) {
			System.err.println("Error in writeToFile Method");
			e.printStackTrace();
		}
	}

}
