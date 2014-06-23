import java.rmi.Remote;
import java.rmi.RemoteException;

public interface MasterReplicaInterface extends Remote {

	public void removeEntry(String fileName) throws RemoteException;
}
