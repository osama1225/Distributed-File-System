import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Scanner;

public class MasterServer extends UnicastRemoteObject implements
		MasterServerClientInterface, MasterReplicaInterface {

	private Hashtable<String, ArrayList<ReplicaLoc>> fileReplicaMap;
	private ArrayList<ReplicaLoc> repLocations;
	private long tId;

	protected MasterServer(String serverAddress, int portNo)
			throws RemoteException {
		super();
		// some ini
		fileReplicaMap = new Hashtable<String, ArrayList<ReplicaLoc>>();
		tId = 0;
		// set up the server
		try {
			// LocateRegistry.createRegistry(portNo);/*
			// Naming.rebind("//" + serverAddress + "/MasterServer"*/, this);
			Registry registry = LocateRegistry.createRegistry(portNo);
			registry.rebind("MasterServer", this);
			System.out.println("Server Started...");
			// initialize Mapping
			iniFileReplicasMap();
		} catch (Exception e) {
			System.out.println("fail to setup the server");
			e.printStackTrace();
		}

	}

	@Override
	public WriteMsg write(FileContent data) throws RemoteException, IOException {
		// synchronization
		synchronized (this) {
			tId++;
			long timeStamp = System.currentTimeMillis();
			ReplicaLoc loc = null;
			if (!fileReplicaMap.containsKey(data.getFileName())) {
				/*
				 * create new file and initiate replicas for it assumption--put
				 * the new file on the first 3 replicas
				 */
				ArrayList<ReplicaLoc> newReplicas = new ArrayList<ReplicaLoc>(3);
				// choose first 3 servers as replicas for the new file
				for (int i = 0; i < repLocations.size() && i < 3; i++)
					newReplicas.add(repLocations.get(i));
				fileReplicaMap.put(data.getFileName(), newReplicas);
			}
			loc = fileReplicaMap.get(data.getFileName()).get(0);
			WriteMsg info = new WriteMsg(tId, timeStamp, loc);
			writeLog("Writing " + data.getData() + " to file "
					+ data.getFileName() + " by transaction with id " + tId);
			return info;
		}
	}

	@Override
	public ReplicaLoc[] read(String fileName) throws FileNotFoundException,
			IOException, RemoteException {
		synchronized (this) {

			if (!fileReplicaMap.containsKey(fileName))
				return null;
			ReplicaLoc[] answer = new ReplicaLoc[fileReplicaMap.get(fileName)
					.size()];
			fileReplicaMap.get(fileName).toArray(answer);
			writeLog("Reading file " + fileName);
			return answer;
		}
	}

	private void iniFileReplicasMap() {
		File repServers = new File("repServers.txt");
		if (repServers.exists()) {
			try {
				Scanner sc = new Scanner(repServers);
				repLocations = new ArrayList<ReplicaLoc>();
				while (sc.hasNextLine()) {
					String[] replica = sc.nextLine().trim().split(" ");
					ReplicaLoc loc = new ReplicaLoc(replica[0],
							Integer.parseInt(replica[1]));
					repLocations.add(loc);
				}
				sc.close();
				// get file names at each replicaServer
				for (int i = 0; i < repLocations.size(); i++) {
					ReplicaLoc loc = repLocations.get(i);
					ReplicaInterfaceAdditional rep = null;
					// if (System.getSecurityManager() == null)
					// System.setSecurityManager(new SecurityManager());
					rep = (ReplicaInterfaceAdditional) Naming.lookup("//"
							+ loc.getAddress() + ":" + loc.getPort()
							+ "/ReplicaServer");
					String[] names = rep.getFileNames();
					for (int j = 0; j < names.length; j++) {
						if (!fileReplicaMap.containsKey(names[j]))
							fileReplicaMap.put(names[j],
									new ArrayList<ReplicaLoc>());
						fileReplicaMap.get(names[j]).add(loc);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("error in iniFileReplicasMap");
			}
		} else
			System.err.println("repServers File not exist");

	}

	@Override
	public void removeEntry(String fileName) throws RemoteException {
		synchronized (this) {
			fileReplicaMap.remove(fileName);
		}
	}

	private void writeLog(String data) {
		File file = new File("Log.txt");
		try {
			String oldData = "";
			if (file.exists()) {
				oldData = readFile("Log.txt");
			} else {
				file.createNewFile();
			}
			FileWriter wr = new FileWriter(file);

			String finalData = oldData + data;
			wr.write(finalData);
			wr.flush();
			wr.close();

		} catch (Exception e) {
			System.err.println("Error in creating log");
		}

	}

	/*
	 * Return the file content as string
	 */
	private String readFile(String fileName) {
		String data;

		File f = new File(fileName);
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
}
