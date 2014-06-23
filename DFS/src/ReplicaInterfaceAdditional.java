import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ReplicaInterfaceAdditional extends Remote {

	public String[] getFileNames() throws RemoteException;

	public boolean propagate(String fileName, ArrayList<FileContent> updates) throws RemoteException;
}
