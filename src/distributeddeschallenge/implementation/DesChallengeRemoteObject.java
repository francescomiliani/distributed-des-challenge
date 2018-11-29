package distributeddeschallenge.implementation;

import distributeddeschallenge.CommunicationRemoteInterface;
import distributeddeschallenge.KeyBlock;
import distributeddeschallenge.server.MasterServerThread;
import static distributeddeschallenge.server.MasterServerThread.*;
import distributeddeschallenge.server.VolunteerDescriptor;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Francesco Miliani & Gabriele Lagani
 * 
*/
public class DesChallengeRemoteObject extends UnicastRemoteObject implements CommunicationRemoteInterface {
   
    public DesChallengeRemoteObject() throws RemoteException {
        super();
    }
    
	/**
	 * Joining to DES Challenge to MasterServer who send back the volunteer id
	 * @return Volunteer ID
	 * @throws RemoteException
	 */
	@Override
	public int joinChallenge( ) throws RemoteException  {
		if( key_found ) return -1;//Return -1 because the challenge is finished.
		//System.out.println("Invoke: joinChallenge");
		int vn = volunteer_number.incrementAndGet();
		VolunteerDescriptor vd = new VolunteerDescriptor( vn );
		volunteer_list_lock.lock();
		try{
			volunteer_list.add(vd);
		} finally {
			volunteer_list_lock.unlock();
		}
		System.out.println("Invoke: joinChallenge - volunteer id: " + vn );
		return vn;//Post increment
	}

	/**
	 * Require another block to the MasterServer
	 * @param volunteer ID
	 * @return KeyBlock to be analyzed
	 * @throws RemoteException
	 */
	@Override
	public KeyBlock getAnotherKeyBlock( int volunteer ) throws RemoteException {
		if( key_found ) return null;//Not return any other block because the challenge is finished.
		
		//Checking either presence or not in volunteer_list
		VolunteerDescriptor vd;
		if( (vd = MasterServerThread.searchVolunteerByID( volunteer ) ) == null) return null;
		KeyBlock k = null;
		key_manager_lock.lock();
		try{
			k = key_manager.getNextBlock( volunteer );
		} finally {
			key_manager_lock.unlock();
		}
		vd.setKey_block_id( k.getId() );
		System.out.println("Invoke: getAnotherKeyBlock - volunteer id: " + volunteer + " - block id: " + k.getId());
		
		return k;
	}
	
	/**
	 * @return Plaintext
	 * @throws RemoteException
	 */
	@Override
	public String getPlaintext() throws RemoteException {
		System.out.println("Invoke: getPlaintext");
		return PLAINTEXT;
	}
	
	/**
	 * @return Ciphertext
	 * @throws RemoteException
	 */
	@Override
	public String getCiphertext(  )throws RemoteException  {
		System.out.println("Invoke: getCiphertext");
		return ciphertext;
	}
	
	/**
	 * Send back the result of the analyzed key block
	 * @param id_key_block
	 * @param result: true, key found; false, not found
	 * @param key : if result is true, it will be equals to the found key!, null otherwise 
	 * @throws java.rmi.RemoteException 
	 */
	@Override
	public void sendKeyBlockResult( int id_key_block, boolean result, byte[] key ) throws RemoteException {
		System.out.println("Invoke: sendKeyBlockResult - block id: " + id_key_block + " - result: " + result );
		if( result ) {
			System.err.println( "\n\n\n*************  Found key!:\t " + DatatypeConverter.printHexBinary( key ) + "  ********\n\n");
			key_found = true;
			//When KillerThread will analyze this variable, will end the program!
		}
		key_manager_lock.lock();
		try{
			key_manager.setBlock( id_key_block );// Key not found
		} finally {
			key_manager_lock.unlock();
		}
	}

    /**
	 * Comunicate to MasterServer Volunteer is leaving the challenge. Vol returns keyblock what it was analyzing
	 * @param volunteer
	 * @param k
	 * @throws RemoteException 
	 */
	@Override
	public void leaveChallenge( int volunteer, KeyBlock k ) throws RemoteException  {
		//Checking either presence or not in volunteer_list
		VolunteerDescriptor vd;
		if( (vd = MasterServerThread.searchVolunteerByID( volunteer ) ) == null) return;
		
		System.out.println("Invoke: leaveChallenge" );
		System.out.println( "Volunteer:  " + volunteer + " leaves the challenge!" );
		System.out.println( "Returns " + ( k.getLimite() - k.getBase() ) + " keys..." );
		
		key_manager_lock.lock();
		try{
			key_manager.setBlock( k );
		} finally {
			key_manager_lock.unlock();
		}
		
		volunteer_list_lock.lock();
		try{
			volunteer_list.remove( vd );
		} finally {
			volunteer_list_lock.unlock();
		}
	}
	
	/**
	 * Ping to MasterServer to know it Volunteer is still alive
	 * @param volunteer id
	 * @throws RemoteException 
	 */
	@Override
	public void ping( int volunteer ) throws RemoteException  {
		VolunteerDescriptor vd;
		//Checking either presence or not in volunteer_list
		if( (vd = MasterServerThread.searchVolunteerByID( volunteer ) ) == null) return;
		
		System.out.println("Invoke: ping from volunteer id: " + volunteer );
		
		volunteer_list_lock.lock();
		try{
			vd.setLive( true );
		} finally {
			volunteer_list_lock.unlock();
		}
	}
}
