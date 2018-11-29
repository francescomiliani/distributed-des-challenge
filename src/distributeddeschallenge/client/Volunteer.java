package distributeddeschallenge.client;

import distributeddeschallenge.CommunicationRemoteInterface;
import distributeddeschallenge.DesEncrypter;
import distributeddeschallenge.KeyBlock;
import static distributeddeschallenge.DesEncrypter.addParity;
import java.net.MalformedURLException;
import java.nio.ByteBuffer;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.UnmarshalException;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Francesco Miliani & Gabriele Lagani
 * Join to MasterServer, get Key Block to try, can leave...
 * Have a PingingThread 
 */
public class Volunteer extends Thread {

	private String plaintext;
	private String ciphertext;
	private byte[] key56 = { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00 };
	private byte[] key64;//Extended to 64 Encryption Key for DES
	
	private int id = -1;
	private KeyBlock current_block;
	
	private boolean leaver = false;
	private PingingThread my_ping_thread;
	private int MAX_KEY_BEFORE_LEAVING = 0;
	
	//RMI parameters
	private static final int REGISTRY_PORT = 1099;//default 1099
    private static final String REGISTRY_HOST = "127.0.0.1";
	private final String volunteer_RMI_URL = "//"+REGISTRY_HOST+":"+Integer.toString( REGISTRY_PORT )+"/MasterServer";
	private CommunicationRemoteInterface cri_stub;
	
	/**
	 * Constructor, call bernoulli function to be either a leaver or not, and make RMI Connection
	 */
	public Volunteer() {
		this.leaver = bernoulli( 0.1 );//Very unlike to obtain true
		if( leaver )//Generates ramdomly a max key before leaves the challenge
			MAX_KEY_BEFORE_LEAVING = (int)( Math.random()*( 1 << 10 ) + 1 );
		
		rmiConnection();
	}
	
	public Volunteer( boolean leaver ) {
		this.leaver = leaver;
		if( leaver )//Generates ramdomly a max key before leaves the challenge
			MAX_KEY_BEFORE_LEAVING = (int)( Math.random()*( 1 << 10 ) + 1 );
		
		rmiConnection();
	}
	
	/**
	 * Make rmi connection, do Naming.lookop with the Server Url, and obtain a remote interface
	 */
	private void rmiConnection() {
		try {
			this.cri_stub = (CommunicationRemoteInterface) Naming.lookup( volunteer_RMI_URL ); 
			
        } catch (MalformedURLException murle) {
            System.out.println();
            System.out.println("MalformedURLException"); 
            System.out.println(murle);
        } catch (RemoteException re) {
            System.out.println();
            System.out.println("RemoteException"); 
            System.out.println(re);
        } catch (NotBoundException nbe) {
            System.out.println();
            System.out.println("NotBoundException"); 
            System.out.println(nbe);
        }
	}
	
	public void setId(int id ){
		this.id = id;
	}
	
	public int getVolunteerId() {
		return this.id;
	}
	
	public KeyBlock getCurrentBlock() {
		return this.current_block;
	}
	/**
	 *	Call Remote Object method joinChallenge
	 *	MasterServer assigns first block key
	*/
	public void joinDesChallenge() { 
        try   { 
			this.id = cri_stub.joinChallenge();
			if ( this.id == -1 ) return;
			
			System.out.println("Volunteer: " + this.id + " join DES Challenge with success!"); 
			if( leaver )	System.out.println("Volunteer: " + this.id + "I'm a LEAVER! I'll leave after " + MAX_KEY_BEFORE_LEAVING + " keys");
			else System.out.println("Volunteer: " + this.id + "I'm NOT a leaver! I'll never leave u <3 ");
			
			getPlaintextAndCiphertext();
        }catch( Exception e ) { 
			System.err.println("Client exception:  " + e.toString()); 
        } 
	}
	
	/**
	 * Call Remote Object method getAnotherKeyBlock
	*/
	public void getNewBlock() {
        try   { 
			this.current_block = cri_stub.getAnotherKeyBlock( this.id );
			System.out.println("Volunteer: " + this.id + " got another key block: " + this.current_block.getId() + "\n" + this.current_block );
        }catch( NullPointerException npe ){
			System.out.println("\nVolunteer: " + this.id + " quits because received a NULL block *****");
			my_ping_thread.killPingThread();//Kill my pinging thread
			return;
		}catch( Exception e)   { 
            System.err.println("Client exception:  " + e.toString()); 
        } 
		
		byte[] base = longToBytes( this.current_block.getBase() );
		//Java.util.Arrays.copyOfRange(short[] original, int from, int to)
		base = Arrays.copyOfRange(base, 1, base.length);
		for( int i = 0; i < key56.length; i++ )
			key56[i] = base[i];//OR equal
	}
	
	/**
	 *	Call Remote Object method ping
	*/
	public void ping()  {
        try   { 
			cri_stub.ping( this.id );
			System.out.println("Volunteer: " + this.id + " PING"); 
        }catch( Exception e)   { 
            System.err.println("Client exception:  " + e.toString()); 
        } 
	}
	
	public void getPlaintextAndCiphertext()  {
        try   { 
			this.plaintext = cri_stub.getPlaintext();
			this.ciphertext = cri_stub.getCiphertext();
			System.out.println("Volunteer: " + this.id + " got plaintext and ciphertext!" );
        }catch( Exception e)   { 
            System.err.println("Client exception:  " + e.toString()); 
        } 
	}
	
	/**
	 * Try all the possible keys in the block, from base to limite
	 * @return true per key found, false otherwise
	 */
	public boolean makeComputations()  {
		SecretKey key;
		DesEncrypter test_encrypter;
		String temp_decrypted;
		long block_size, final_time, starting_time = System.currentTimeMillis();
		boolean key_found = false;
				
		if( leaver ) block_size = MAX_KEY_BEFORE_LEAVING;
		else block_size = ( this.getCurrentBlock().getLimite() - this.getCurrentBlock().getBase() ) + 1;
		
		for( int i = 0; i < block_size; i++ ) {
			key64 = addParity( key56 );
			
			//Building the key of the cipher
			key = new SecretKeySpec( key64, "DES" );
			try {
				test_encrypter = new DesEncrypter( key );
				temp_decrypted = test_encrypter.decrypt( this.ciphertext );
				if( temp_decrypted.equals( this.plaintext ) ) {
					key_found = true;
					break;
				}
			}
			catch( Exception e ){
				// wrong key!
			}
			DesEncrypter.incrementKey( key56 );//because key NOT found
			this.current_block.incrementBase();//Updating block
		} //end for

		final_time = System.currentTimeMillis();
		System.out.println( "Volunteer: " + this.id + " finished the keys in:\t " + (final_time - starting_time) + " ms");		

		if( key_found )
			System.err.println( "\n\n\n************* Volunteer: " + id + " Found key!:\t " + DatatypeConverter.printHexBinary( key56 ) + "  ********\n\n");
		else
			System.out.println( "Volunteer: " + this.id + ".. Key NOT found in block " + this.current_block + "!" );
		
		if( leaver ) leave();
		else sendResult( key_found );
		
		return key_found;

	}
	
	/**
	 * Call Remote Object method sendKeyBlockResult
	 * @param result 
	 */
	public void sendResult( boolean result ) {
		try {
			if( result )
				cri_stub.sendKeyBlockResult( this.current_block.getId(), true, key56);
			else
				cri_stub.sendKeyBlockResult( this.current_block.getId(), false, null);
		}catch( UnmarshalException ue)   { 
			System.out.println("Volunteer: " + this.id + " finds server unreachable and quits.." );
			return;
		}catch( Exception e)   { 
			System.err.println("Client exception:  " + e.toString()); 
			e.printStackTrace(); 
		} 
	}
	
	/**
	 *	Call Remote Object method leaveChallenge
	 *	Send back the (unterminated) key block to the server
	*/
	public void leave() {
		//Update state of block
        try   { 
			cri_stub.leaveChallenge( this.id, this.current_block );
			System.out.println("Volunteer: " + this.id + " leaves!" ); 
			
			my_ping_thread.killPingThread();//Swith off the pinging thread
        }catch( Exception   e)   { 
            System.err.println("Client exception:  " + e.toString()); 
            e.printStackTrace(); 
        } 
	}

	public byte[] longToBytes(long x) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.putLong(x);
		return buffer.array();
	}

	public long bytesToLong(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
		buffer.put(bytes);
		buffer.flip();//need flip 
		return buffer.getLong();
	}
	
	/**
     * Return a boolean, which is true with probability p, and false otherwise.
	 * @param p probability
	 * @return true or false just like a bernoullian random variable
     */
    public static boolean bernoulli( double p ) {
        return Math.random() < p;
    }
	
	@Override
	public void run(){
			joinDesChallenge();//1) join to the challenge
			
			my_ping_thread = new PingingThread( 3000 );
			my_ping_thread.start();//2) Start PingingThread

			if( leaver ) {
				getNewBlock();//3)
				makeComputations();//4)
				leave();//pingingThread has killed within leave function
			}else {
				while( true ) {
					getNewBlock();//3)
					if( this.current_block == null)//null block mean server has found the key!!
						return;//pingingThread has killed within getNewBlock function when a null block occurs
					else 
						makeComputations();
					/* TEST CRASHING ------ SUCCESS
					if( this.getVolunteerId() == 1 )
						try {
							my_ping_thread.killPingThread();
							Thread.sleep( 3000 );
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
					*/
				}//while
			}//else NOT Leaver
	}
	
	/* TEST MAIN */
	public static void main(String[] argv) throws InterruptedException {
		System.out.println("________________TEST MAIN_______________________" );

		//Volunteer v4 = new Volunteer( false );//Randomly
		//v4.start();
		Volunteer v;
		for( int i = 0; i < 10 ; i++) {
			v = new Volunteer();
			v.start();
		}
	}
	
	/**
	 * Periodically pings MasterServer to know it that this Volunteer is still alive
	 */
	class PingingThread extends Thread {
		public boolean run_state = true;
		public long sleep_time = 3000;
		public void killPingThread() { run_state = false; }
		
		//Constructor
		public PingingThread( long sleep_time ) {
			this.sleep_time = sleep_time;
		}
		
		@Override
		public void run() {
			while( run_state ) {
				try {
					ping();
					Thread.sleep( sleep_time );
				}catch( Exception e ){	}
			}
		}
	}
}
