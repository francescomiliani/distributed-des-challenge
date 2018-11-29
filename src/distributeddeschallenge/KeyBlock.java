package distributeddeschallenge;

//import static distributeddeschallenge.StateEnum.*;
import java.io.Serializable;
import java.nio.ByteBuffer;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * @author franc
 */
//enum StateEnum { ASSIGNED, EXPLORED, UNEXPLORED }

/**
 * La classe implementa Serializable.
 * Altrimenti non può essere trasmessa come parametro o valore di ritorno
 */
public class KeyBlock implements Serializable {
	private static int incremental_id = -1;//
	private int id;//Personal ID
	private long base;
	private long limite;
	private StateEnum state;
	private int volunteer; //ID of trialer

	public KeyBlock( long base, long limite ) {
		this.incremental_id++;
		this. id = incremental_id;
		this.base = base;
		this.limite = limite;
		this.state = StateEnum.UNEXPLORED;
		this.volunteer = -1;// Not Assigned to nobody
	}

	/*
	*	Copy Constructor
	* @param  x the first {@code int} to compare
	* @return the value {@code 0} if {@code x == y};
	*         a value less than {@code 0} if {@code x < y}; and
	*         a value greater than {@code 0} if {@code x > y}
	* @since 1.0
	*/
	public KeyBlock ( KeyBlock k ){
		this.id = k.getId();
		this.base = k.getBase();
		this.limite = k.getLimite();
		this.state = k.getState();
		this.volunteer = k.getVolunteer();// Not Assigned to nobody
	}
	
	public int getIncrementalID (){
		return incremental_id;
	}
	
	public int getId() {
		return id;
	}	
	
	public long getBase() {
		return base;
	}

	public void incrementBase() {
		base++;
	}
	public void setBase(long base) {
		this.base = base;
	}

	public long getLimite() {
		return limite;
	}

	public void setLimite(long limite) {
		this.limite = limite;
	}

	public StateEnum getState() {
		return state;
	}

	public void setState(StateEnum state) {
		this.state = state;
	}

	public int getVolunteer() {
		return volunteer;
	}

	public void setVolunteer(int volunteer) {
		this.volunteer = volunteer;
	}

	public byte[] integerToByteArray( int n ) {
		byte[] bytes = ByteBuffer.allocate(4).putInt(n).array();
		/*
		for (byte b : bytes) {
		   System.out.format("0x%x ", b);
		}
		*/
		return bytes;
	}
	
	public static byte[] longToByteArray( long n ) {
		byte[] bytes = ByteBuffer.allocate( 8 ).putLong( n ).array();
		/*
		for (byte b : bytes) {
		   System.out.format("0x%x ", b);
		}
		*/
		return bytes;
	}
		
	@Override
	public String toString() {
		return "KeyBlock {\n" +
				"id=" + id + 
				"\nbase=" + DatatypeConverter.printHexBinary( longToByteArray( base ) ) + 
				"\nlimite=" + DatatypeConverter.printHexBinary( longToByteArray( limite ) ) + 
				"\nstate=" + state + 
				"\nvolunteer=" + volunteer + '}';
	}

	public static void main( String argv[]) {
		KeyBlock k0 = new KeyBlock( 0, 10 );//16 bit
		KeyBlock k1 = new KeyBlock( 11, 20 );//16 bit
		System.out.println( k0 );
		System.out.println( k1 );
	}
}
