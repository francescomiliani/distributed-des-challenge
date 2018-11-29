package distributeddeschallenge;

/**
 *
 * @author Francesco Miliani & Gabriele Lagani
 * Remote Interface, methods to implement in MasterServer
 */
import java.rmi.Remote; 
import java.rmi.RemoteException; 
public interface CommunicationRemoteInterface extends Remote { 
	//public String sayHello() throws RemoteException;
    public int joinChallenge( ) throws RemoteException;
	public void sendKeyBlockResult( int id_key_block, boolean result, byte[] key) throws RemoteException;
    public KeyBlock getAnotherKeyBlock( int volunteer ) throws RemoteException;
    public void leaveChallenge( int volunteer, KeyBlock k ) throws RemoteException;
	public String getPlaintext( ) throws RemoteException;
	public String getCiphertext( ) throws RemoteException;
	public void ping( int volunteer ) throws RemoteException;
}