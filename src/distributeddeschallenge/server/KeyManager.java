package distributeddeschallenge.server;

import distributeddeschallenge.KeyBlock;
import distributeddeschallenge.StateEnum;
import java.util.ArrayList;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author Francesco
 * @version     1.0		( current version number of program )
 * @since       1.2		( the version of the package this class was first added to )
 */
public class KeyManager {
	private final long FIRST_KEY = Long.parseLong("00FFFFFFFF000000", 16);//16 is the formattation base
	private final long LAST_KEY = Long.parseLong( "00FFFFFFFFFFFFFF", 16);
	
	private ArrayList<KeyBlock> key_array;
	private final int MAX_KEY_SIZE = 10;//1024 keys, that is 10 bit
	private final int KEY_SPACE = 24;//24 bit of key space
	private final int KEY_BLOCK_NUMBER; 
	private int already_tried = 0;
	private KeyBlock last_send;
	/*
		Constructor
	*/
	public KeyManager( int key_space ) {
		this.key_array = new ArrayList<KeyBlock>();
		long base = FIRST_KEY;
		long limite = base + (1 << MAX_KEY_SIZE ) - 1;//it is equal to pow( 2, MAX_KEY_SIZE )
		KeyBlock k;
		//it is equal to make a ratio( pow(2,KEY_SPACE) / pow(2, MAX_KEY_SIZE)
		this.KEY_BLOCK_NUMBER = 1 << ( KEY_SPACE - MAX_KEY_SIZE );
		for( int i = 0; i < KEY_BLOCK_NUMBER; i++ ) {
			k = new KeyBlock( base, limite );
			key_array.add( k);
			base = limite + 1;
			limite += (1 << MAX_KEY_SIZE );//Already decrease, just one time
		}
	}
	
	/*
	*	Copy Constructor
	* @param  volunteer: the first {@code int} to compare
	* @return the value {@code 0} if {@code x == y};
	*         a value less than {@code 0} if {@code x < y}; and
	*         a value greater than {@code 0} if {@code x > y}
	* @since 1.0
	*/
	public KeyBlock getNextBlock( int volunteer ) {
		KeyBlock k = null;
		//Cycle starts from alreay_tried because when a sequence of consecutive block have been tried all,
		//It need not to access it anymore.
		for( int i = already_tried; i < this.KEY_BLOCK_NUMBER ; i++ ) {
			if( key_array.get(i).getState()  == StateEnum.UNEXPLORED ){
				//Setting of the block as ASSIGNED to volunteer
				key_array.get(i).setState( StateEnum.ASSIGNED );
				key_array.get(i).setVolunteer( volunteer );
				k = new KeyBlock ( key_array.get(i) );//Copy of the block
				last_send = k;
				return k;
			}
		}
		return k;
	}
	
	/**
	 * Update the KeyBlock k
	 * @param k 
	 */
	public void setBlock( KeyBlock k ) {
		key_array.get( k.getId() ).setBase( k.getBase() );//Update base, limite does not change
		key_array.get( k.getId() ).setState( StateEnum.UNEXPLORED );
		key_array.get( k.getId() ).setVolunteer( -1 );
		
	}
	
	/**
	 * KEY NOT FOUND
	 * @param key_block_id
	*/
	public void setBlock( int key_block_id ) {
		key_array.get( key_block_id ).setState( StateEnum.EXPLORED );
		
		updateAlreadyTried( key_block_id );//Update if it is possible
	}
	
	/**
	 * Reset to UNEXPLORED a given block because a certain volunteer has been terminated or crashed
	 * @param key_block_id
	*/
	public void releaseBlock( int key_block_id ) {
		key_array.get( key_block_id ).setState( StateEnum.UNEXPLORED );
		key_array.get( key_block_id ).setVolunteer( -1 );
	}
	
	/**
	 * Update AlreadyTried if all previous blocks have been explored
	 * @param key_block_id 
	 */
	private void updateAlreadyTried( int key_block_id ) {
		boolean ok = true;
		for( int i = already_tried; (i < key_block_id) && ( ok ); i++ ) {
			if( key_array.get(i).getState() != StateEnum.EXPLORED )
				ok = false;
		}
		
		if( ok )	already_tried = key_block_id;
		//else not update
	}

	public KeyBlock getLastSend() {
		return last_send;
	}
	
	public int getAlreadyTried(){
		return already_tried;
	}
	
	/**
	 * @return Completion Percentage of the overall blocks
	 */
	public double getCompletionPercentage() {
		return (double)already_tried/(double)KEY_BLOCK_NUMBER;
	}
	
	@Override
	public String toString() {
		return "KeyManager{\n" +
				"\nFIRST_KEY=" + DatatypeConverter.printHexBinary( KeyBlock.longToByteArray( FIRST_KEY ) ) + 
				"\nLAST_KEY=" + DatatypeConverter.printHexBinary( KeyBlock.longToByteArray( LAST_KEY ) ) + 
				"\nkey_array=" + key_array + 
				"\nMAX_KEY_SIZE=" + MAX_KEY_SIZE + 
				"\nKEY_SPACE=" + KEY_SPACE + 
				"\nkey_block_number=" + KEY_BLOCK_NUMBER + 
				"\nalready_tried=" + already_tried + 
			'}';
	}
	
	//Testing Main
	public static void main( String argv[]) {
		KeyManager km = new KeyManager( 0xFFFF );//16 bit
		System.out.println( km );
	}
}
