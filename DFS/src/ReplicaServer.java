import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class ReplicaServer extends UnicastRemoteObject implements
		ReplicaServerClientInterface, ReplicaInterfaceAdditional {

	private Hashtable<Long, ArrayList<FileContent>> tempWrites;
	private String masterAddress;
	private int masterPort;
	private final String dir = ".\\Files\\";

	// private Hashtable<String, Semaphore> fileSemaphoreMap;

	protected ReplicaServer(String ReplicaAddress, int port,
			String masterAddress, int masterPort) throws RemoteException {
		super();
		// some ini
		this.masterAddress = masterAddress;
		this.masterPort = masterPort;
		tempWrites = new Hashtable<Long, ArrayList<FileContent>>();
		// fileSemaphoreMap = new Hashtable<String, Semaphore>();
		// set up the server
		try {
			// LocateReg/*istry.createRegistry(port);
			// Naming.re*/bind("//" + ReplicaAddress + "/ReplicaServer", this);
			Registry registry = LocateRegistry.createRegistry(port);
			registry.rebind("ReplicaServer", this);
			System.out.println("ReplicaServer Started...");
		} catch (Exception e) {
			System.out.println("fail to setup the replica server");
			e.printStackTrace();
		}

	}

	@Override
	public WriteMsg write(long txnID, long msgSeqNum, FileContent data)
			throws RemoteException, IOException {
		synchronized (this) {
			if (!tempWrites.containsKey(txnID))
				tempWrites.put(txnID, new ArrayList<FileContent>());
			tempWrites.get(txnID).add(data);
		}
		return null;
	}

	@Override
	public boolean commit(long txnID, long numOfMsgs)
			throws MessageNotFoundException, RemoteException {

		ArrayList<FileContent> updates = tempWrites.get(txnID);
		String fileName = updates.get(0).getFileName();
		// check consistency here
		// if (!fileSemaphoreMap.containsKey(fileName))
		// fileSemaphoreMap.put(fileName, new Semaphore(1));
		while (!canCommit(txnID, fileName)) {
			System.out
					.println("Transaction with id " + txnID + " is waiting..");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		boolean res = actualWrite(fileName, updates);

		// propagate to other replicas ..not yet
		// get list of other replica locations
		try {
			MasterServerClientInterface stub = null;
			// if (System.getSecurityManager() == null)
			// System.setSecurityManager(new SecurityManager());
			stub = (MasterServerClientInterface) Naming.lookup("//"
					+ masterAddress + ":" + masterPort + "/MasterServer");
			ReplicaLoc[] otherReplicas = stub.read(fileName);
			// connect to each one of them and send them updates
			for (int i = 1; i < otherReplicas.length; i++) {
				ReplicaLoc loc = otherReplicas[i];
				ReplicaInterfaceAdditional stubRep = null;
				// if (System.getSecurityManager() == null)
				// System.setSecurityManager(new SecurityManager());
				stubRep = (ReplicaInterfaceAdditional) Naming.lookup("//"
						+ loc.getAddress() + ":" + loc.getPort()
						+ "/ReplicaServer");
				boolean state = stubRep.propagate(fileName, updates);
				if (!state) {
					System.err.println("updates of file: " + fileName
							+ " didn't propagated to replica "
							+ loc.getAddress());
					// m3rfsh hn3ml eh law dah 7asl b2a
				}
			}
		} catch (Exception e) {
			System.err.println("Error in fetching other replica addresses");
			e.printStackTrace();
		}

		if (!res)
			return false;
		synchronized (tempWrites) {
			tempWrites.remove(txnID);
		}
		return true;
	}

	private boolean actualWrite(String fileName, ArrayList<FileContent> updates) {
		File f = new File(dir + fileName);
		String oldData = "";
		String newData = "";
		if (!f.exists()) { // file not exist before.
			try {
				f.createNewFile();
			} catch (IOException e1) {
				System.err.println("Error in creating file in replica");
				e1.printStackTrace();
				return false;
			}
		} else {// file exist before
			oldData = readFile(fileName);
			newData = oldData + "\n";
		}
		for (int i = 0; i < updates.size(); i++)
			newData += updates.get(i).getData() + "\n";
		newData = newData.substring(0, newData.length() - 1);

		// write new data
		try {
			FileWriter writer = new FileWriter(f);
			writer.write(newData);
			writer.flush();
			writer.close();
			return true;
		} catch (Exception e) {
			System.err.println("Error in actual write method");
			e.printStackTrace();
			return false;
		}

	}

	@Override
	public boolean abort(long txnID) throws RemoteException {
		// get the file name of that tid;
		String fileName = tempWrites.get(txnID).get(0).getFileName();
		File f = new File(".\\Files\\" + fileName);
		if (!f.exists()) {
			// so it is a new file and won't be committed--remove its meta-data
			// from server.
			try {
				MasterReplicaInterface stub = (MasterReplicaInterface) Naming
						.lookup("//" + masterAddress + ":" + masterPort
								+ "/MasterServer");
				stub.removeEntry(fileName);
			} catch (Exception e) {
				System.err.println("Error in removing entry");
				e.printStackTrace();
				return false;
			}
		}
		tempWrites.remove(txnID);
		return true;

	}

	@Override
	public FileContent read(String fileName) throws FileNotFoundException,
			IOException, RemoteException {

		FileContent c = new FileContent();
		c.setFileName(fileName);
		String data = "Can't Read UnCommitted File";
		// check if file is hold by another client for writing
		// if (isCommitted(fileName))
		data = readFile(fileName);
		c.setData(data);
		return c;
	}

	@Override
	public String[] getFileNames() throws RemoteException {
		File directory = new File(dir);
		return directory.list();
	}

	/*
	 * Return the file content as string
	 */
	private String readFile(String fileName) {
		String data;

		File f = new File(dir + fileName);
		if (f.exists()) {
			data = "";
			try {
				Scanner sc = new Scanner(f);
				while (sc.hasNextLine())
					data += sc.nextLine() + "\n";
				data = data.substring(0, data.length() - 1);
				sc.close();
			} catch (Exception e) {
				System.err.println("Error in reading a file");
			}
			return data;
		}
		return "notfound-1";
	}

	@Override
	public boolean propagate(String fileName, ArrayList<FileContent> updates)
			throws RemoteException {
		boolean res = actualWrite(fileName, updates);
		if (!res)
			return false;
		tempWrites.remove(fileName);
		return true;
	}

	/*
	 * Method that take the file name, and check if there is uncommitted
	 * transactions concerning to that file
	 */
	private boolean isCommitted(String fileName) {
		Set<Long> keys = tempWrites.keySet();
		for (long tid : keys) {
			if (fileName.equals(tempWrites.get(tid).get(0).getFileName()))
				return false;
		}
		return true;
	}

	/*
	 * Method that take the tid and check if that client can commit(no other
	 * clients writing to the same file and have lower time stamp value)
	 */
	private boolean canCommit(long tid, String fileName) {
		Set<Long> keys = tempWrites.keySet();
		for (long i : keys) {
			if (fileName.equals(tempWrites.get(i).get(0).getFileName())
					&& i < tid)
				return false;
		}
		return true;
	}

	
}
