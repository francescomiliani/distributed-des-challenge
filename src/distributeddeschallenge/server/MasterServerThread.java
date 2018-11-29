package distributeddeschallenge.server;

import distributeddeschallenge.CommunicationRemoteInterface;
import distributeddeschallenge.DesEncrypter;
import static distributeddeschallenge.KeyBlock.longToByteArray;
import distributeddeschallenge.implementation.DesChallengeRemoteObject;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.Registry; 
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.*;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Francesco Miliani & Gabriele Lagani
 * 
*/

public class MasterServerThread extends Thread {

	public boolean online = true;//When false GUI cannot receive request
	public static DesEncrypter des_enc;
	public static final String PLAINTEXT = "The unknown message is: many hands make light work.";
	public static String ciphertext;
	
	public static AtomicInteger volunteer_number = new AtomicInteger(-1);//Static because must be the same for all
	public static KeyManager key_manager;
	public static Lock key_manager_lock = new ReentrantLock();
	public static boolean key_found = false;
	public static ArrayList<VolunteerDescriptor> volunteer_list;
	public static Lock volunteer_list_lock = new ReentrantLock();
	
	//___RMI Parameters_____
	//the default port for rmiregistry is 1099;instead, by default connections to remote objects are on random ports
    //(otherwise specified in the export command)
    private static final int REGISTRY_PORT = 1099;
    //the "myRegistry" reference variable isn't actually used; for the sake of clarity, we just assign it the value returned 
    //by the registry creation method.
    private static Registry myRegistry;
	public CommunicationRemoteInterface cri_stub;
    private static final String REGISTRY_HOST = "127.0.0.1";
    private static final String SERVANT_NAME = "MasterServer";
	private static final String NAMING_STRING = "//"+REGISTRY_HOST+":"+Integer.toString(REGISTRY_PORT)+"/"+SERVANT_NAME;
	
	private static final byte[] KEY_56 = { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x20, (byte)0x00, (byte)0x00 };
	private final int ACTUAL_KEY_SPACE = 1 << 24;
	
	private Long starting_time;
	private Long final_time;
	
	private TerminatorThread term_t;

	public MasterServerThread() throws RemoteException {
		volunteer_list = new ArrayList<VolunteerDescriptor>();
		key_manager = new KeyManager( ACTUAL_KEY_SPACE );
	}
	
	public String getPlaintext() {
		System.out.println("COMMAND: PLAINTEXT");
		if( key_found )
			return PLAINTEXT;
		else
			return "How can I get the plaintext if I do not know the key yet ? ;-)";
	}
	
	public String getCiphertext() {
		System.out.println("COMMAND: CIPHERTEXT");
		return ciphertext;
	}
	
	/**
	 * Return the Volunteer Descriptor given the VolunteerID
	 * @param id of the volunteer
	 * @return pointer to desidered VolunteerDescripter within the list
	 *			Null: NOT found
	 */
	public static VolunteerDescriptor searchVolunteerByID( int id ){
		volunteer_list_lock.lock();
		try {
			VolunteerDescriptor vd;
			Iterator i = volunteer_list.iterator();
			while( i.hasNext() ) {
				vd = (VolunteerDescriptor)i.next();
				   if ( vd.volunteer_number == id )
					  return vd;
				}
		} finally {
			volunteer_list_lock.unlock();
		}
		return null;
	}
	
	/**
	 * @return volunteer number that is the volunteer_list size
	 */
	public String getVolunteerNumber() {
		System.out.println("COMMAND: VOLUNTEER NUMBER");
		return Integer.toString( volunteer_list.size() );
	}
	
	/**
	 * @return String composed by the following schema: <VolunteerID, BlockID>
	 */
	public String getVolunteerState() {
		System.out.println("COMMAND: VOLUNTEER LIST");
		String s = "Volunteer List \n";
		VolunteerDescriptor vd;
		if( volunteer_list.isEmpty() ) {
			s = "There are NOT volunteer yet... keep calm! ;-)";
			return s;
		}
		volunteer_list_lock.lock();//Start Mutual Exclusion
		Iterator i = volunteer_list.iterator();
		try {
			while( i.hasNext() ) {
				vd = (VolunteerDescriptor)i.next();
				s = s.concat( "Volu_ID: ");
				s = s.concat( Integer.toString( vd.getVolunteer_number() ) );
				s = s.concat( "; Block_ID: ");
				s = s.concat( Integer.toString( vd.getKey_block_id() ) + "\n");
			}
			s = s.concat( "\nTotal Volunteer Number: " + Integer.toString( volunteer_list.size() ) );
		}finally {
			volunteer_list_lock.unlock();//End Mutual Exclusion
		}
		return s;
	}
	
	/**
	 * @return String with KEY if the key has been found, KEY NOT FOUND otherwise
	 */
	public String getChallengeState() {
		System.out.println("COMMAND: CHALLENGE STATE");
		String s;
		if( key_found ) {
			s = "KEY FOUND!!! : " + DatatypeConverter.printHexBinary(KEY_56 );//Beacause we know it in advance
		}else {
			s = "KEY NOT FOUND YET...\n" +
					"Last given block: " + 
						"\n\tBlock ID: " + key_manager.getLastSend().getId() +
						"\n\tBase: " + DatatypeConverter.printHexBinary( longToByteArray( key_manager.getLastSend().getBase() ) ) +
						"\n\tLimite: " + DatatypeConverter.printHexBinary( longToByteArray(key_manager.getLastSend().getLimite() ) ) +
						"\n\tVolunteer: " + key_manager.getLastSend().getVolunteer() +
					"\nExplored Blocks Percentage: " + Double.toString( key_manager.getCompletionPercentage() ) + "%";
		}
		return s;
	}
	
	/**
	 * @return String with successful or not per switching off of the MasterServer
	 */
	public String switchOff() {
		String s ;
		System.out.println("COMMAND: SWITCH-OFF");
		if( key_found ) {
			shutdownServer();
			s = "OK! Server will stop yourself right now";
		}
		else {
			System.err.println("Key is not found yet! -_-! Don't stop me now! ;-)");
			s = "Key is not found yet! -_-! Don't stop me now! ;-)";
		}
		return s;
	}
	
	/**
	 * Terminate the server
	 * Remote Object unbinding, and unexporting
	 */
	public void shutdownServer() {
		term_t.killTerminatorThread();//Kill terminator thread
		System.out.println("Server will stop now...");
		online = false;//In this way GUI cannot sends request anymore
		try {
			UnicastRemoteObject.unexportObject(cri_stub, true);
			Naming.unbind( NAMING_STRING );
			UnicastRemoteObject.unexportObject( myRegistry, true);
		}catch( Exception e){
			e.printStackTrace();
		}
		final_time = System.currentTimeMillis();
		System.err.println( "******************************** Total computation time:\t " + ( final_time - starting_time) + " ms" );
		System.out.println("............. Server STOP ...................");
	}
	
	@Override
	public void run() {
		// Remember: RMI's class loader will download classes from remote locations only if a security manager has been set.
		if ( System.getSecurityManager() == null) {
            // java.lang.SecurityManager 
            System.setSecurityManager(new SecurityManager());
        }
		
		//the server is set up
		try {
            cri_stub = new DesChallengeRemoteObject();//a servant object is created
			myRegistry = LocateRegistry.createRegistry( REGISTRY_PORT );
			System.out.println("Registry created - port "+Integer.toString( REGISTRY_PORT ) );
			Naming.rebind( NAMING_STRING, cri_stub);
        } catch (Exception e) { 
            System.err.println("Trouble: " + e);
        }
		
		//Encryption for FIRST TIME
		//56 Bit Key - Encryption Key
		final byte[] key64 = DesEncrypter.addParity( KEY_56 );//Extended to 64 Encryption Key for DES

		//Printing of the message
		System.out.println("Message:\t " + PLAINTEXT );

		//Building the key of the cipher
		SecretKey sk = new SecretKeySpec( key64, "DES" );
		System.out.println("Secret Key:\t" + DatatypeConverter.printHexBinary(KEY_56 ) );
		//Create an object for encryption and decryption 
		DesEncrypter encrypter;
		try {
			encrypter = new DesEncrypter( sk );
			ciphertext = encrypter.encrypt( PLAINTEXT ); //Message Encryption
		}catch( Exception e){
			e.printStackTrace();
		}
		
		System.out.println( "Encrypted Message:\t " + ciphertext );
		
		//Starts TerminatorThread
		term_t = new TerminatorThread( 4000 );//4000 by default in constructor
		term_t.start();
		
		starting_time = System.currentTimeMillis();
		System.out.println( "____Timer starts!________" );
		System.out.println("........Server ready......."); 
        
	}
//**********************************************************************************************************
	/**
	 * TerminatorThread sleeps for a given time period and after calls checkLiveVolunteer
	 */
	class TerminatorThread extends Thread {
		
		private long sleep_time = 4000;//Default sleep time ms
		private boolean run_state = true;
		public TerminatorThread( long sleep_time ) {
			this.sleep_time = sleep_time;
		}
		
		/**
		* this function watch each Volunteer state, and if it true then it set to false, otherwise
		* if Volunteer state is false then it remove that VolunteerThread from the volunteerList
		*/
		private void checkLiveVoluteers() {
			volunteer_list_lock.lock();
			try {
				VolunteerDescriptor vd;
				if ( volunteer_list.isEmpty() ) return;

				for( int i = 0; i < volunteer_list.size(); i++ ) {
					vd = volunteer_list.get( i );
					if( !vd.isLive() ) {//Volunteer is not live anymore
						//Free the assigned block to the i-th volunteer
						key_manager.releaseBlock( vd.getKey_block_id() );
						System.err.println("***TerminatorThread:  Volunteer " + vd.getVolunteer_number() + "! you have been TERMINATED! **" ); 
						volunteer_list.remove( i );//Remove from the list

					}else{//Resetting
						vd.setLive( false );
					}
				}//for
			} finally {
				volunteer_list_lock.unlock();
			}
		}
		public void killTerminatorThread() { run_state = false;	}
		@Override
		public void run() {
			System.err.println("**** TerminatorThread is came from the future ****" );
			while( run_state ) {
				try {
					Thread.sleep( sleep_time );
					System.err.println("**** TerminatorThread: checking Live Volunteers! ****" );
					checkLiveVoluteers();
				}catch( InterruptedException ie ) {
					ie.printStackTrace();
				}
			}
			System.out.println("TerminatorThread killed...");
		}
	}

}
