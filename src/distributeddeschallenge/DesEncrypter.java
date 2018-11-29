package distributeddeschallenge;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.util.Base64;;

/**
 * 
 * Plain Text:			The unknown message is: many hands make light work.[5 x padding]
 * Known Plain Text:		The unknown message is: ********************************
 * Key:					0x FF FF FF FF 77 77 77
 * Cypher Text:			
 * Key Space to try:		0x FF FF FF FF 00 00 00 : 0x FF FF FF FF FF FF FF ( 24 bit space )
//DES uses the last bit of the key for PARITY, therefore if the key is odd the found key is 1 less, otherwise 
//it is the correct one.
*/

public class DesEncrypter {
	private Cipher ecipher;
	private Cipher dcipher;
	
	public DesEncrypter( SecretKey key ) throws Exception {
		ecipher = Cipher.getInstance("DES");
		dcipher = Cipher.getInstance("DES");
		ecipher.init(Cipher.ENCRYPT_MODE, key);
		dcipher.init(Cipher.DECRYPT_MODE, key);
	}

	public String encrypt( String str ) throws Exception {
		// Encode the string into bytes using utf-8
		byte[] utf8 = str.getBytes("UTF8");

		// Encrypt
		byte[] encodedBytes = ecipher.doFinal(utf8);
		return new String ( Base64.getEncoder().encode( encodedBytes ) );
	}

	public String decrypt( String str ) throws Exception {
		// Decode base64 to get bytes
		byte[] decodedBytes = Base64.getDecoder().decode( str );
		byte[] utf8 = dcipher.doFinal(decodedBytes);

		// Decode using utf-8
		return new String(utf8, "UTF8");
	}

	/*
		Convert 56 bit key to 64 bit key for DES
		DES aggiunge un bit di parità in più su ogni byte della chiave, quindi i bit di ogni byte
		sono shiftati a sx opportunamente
    */
	public static byte[] addParity( byte[] key56 ) {
		
		byte[] key64 = new byte[8];
		// --- create 64 bit key from 56 bit key
		// least significant bit can have any value
		key64[0] = (byte) (key56[0] & 0xFE); // << 0
		key64[1] = (byte) ((key56[0] << 7) | ((key56[1] & 0xFF) >>> 1));
		key64[2] = (byte) ((key56[1] << 6) | ((key56[2] & 0xFF) >>> 2));
		key64[3] = (byte) ((key56[2] << 5) | ((key56[3] & 0xFF) >>> 3));
		key64[4] = (byte) ((key56[3] << 4) | ((key56[4] & 0xFF) >>> 4));
		key64[5] = (byte) ((key56[4] << 3) | ((key56[5] & 0xFF) >>> 5));
		key64[6] = (byte) ((key56[5] << 2) | ((key56[6] & 0xFF) >>> 6));
		key64[7] = (byte) (key56[6] << 1);

		// --- set parity in time independent of the values within key64
		for ( int i = 0; i < key64.length; i++) {
			// if even # bits, make uneven, take last bit of count so XOR with 1
			// for uneven # bits, make even, take last bit of count so XOR with 0  
			key64[i] ^= Integer.bitCount(key64[i] ^ 1) & 1;
		}
		return key64;
    }
	
	/*
		Incrementa la chiave, se c'è un riporto in un byte lo propaga a quello dopo!
	*/
	public static void incrementKey( byte[] byteKey ) {
		for(int i = byteKey.length - 1; i >= 0 ; i--){
			byteKey[ i ]++;
			if( byteKey[i] != (byte) 0) //no carry
				break;
		}
	}
	
//____________________________ MAIN __________________________________________________________________
	
  public static void main(String[] argv) throws Exception {
	//final String message = "The unknown message is: many hands make light work.";
	
	//String to SecretKey:
	//(byte) è scritto per fare la conversione esplicita che altrimenti considererebbe il bit + significativo come segno

	//56 Bit Key - Encryption Key
	//final byte[] key56 = { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x45, (byte)0x44 };
	//final byte[] key64 = addParity( key56 );//Extended to 64 Encryption Key for DES
	
	//Printing of the message
	//System.out.println( "__________________Working Encryption TEST Trial__________________________________________");
	//System.out.println( "Message:\t " + message);
	
	//Building the key of the cipher
	//SecretKey sk = new SecretKeySpec( key64, "DES" );
	//System.out.println( "Secret Key: " + sk.toString().split("@")[1] );
    
	//Create an object for encryption and decryption 
	//DesEncrypter encrypter = new DesEncrypter( sk );
	
    //String encrypted = encrypter.encrypt( message ); //Message Encryption
    //System.out.println( "Encrypted Message:\t " + encrypted);
	
	//Decommentare se si vuol vedere che funziona la decifratura tramite la chiave originale
	//decrypted = encrypter.decrypt( encrypted );
	//System.out.println( "Decrypted Message:\t " + decrypted);
	
	/* Note teoriche
		Test ricerca chiave, basta usare una chiave pari per evitare il problema del bit di parità
		Cioè l'ultimo bit è ignorato, una chiave che finisce con 0 o 1 per lui è la stessa... PER OGNI BYTE
		Remember that: 0, 1, 2 3, 4, 5, 6, 7, 8 elements
		Lo stampa effettivamente così! cioè dal pù significativo a sinistra al meno significativo a destra
		BIG ENDIAN
		DES AGGIUNGE 1 BIT = 1 COME BIT DI PARITà in ogni byte
	*/
/*
	SecretKey temp_secret_key;
	DesEncrypter test_encrypter;
	String temp_decrypted;
	
	Long starting_time = System.currentTimeMillis();

	//Chiave a 24 bit messa da Gabri
	//byte[] trialKey56 = { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x00 };
	//Chiave di prova a 16bit messa da Fra
	byte[] trialKey56 = { (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00 };
	byte[] trialKey64;
	System.out.println( "Starting Key:\t " + DatatypeConverter.printHexBinary( trialKey56 ) );

	boolean keyFound = false;
	//int blockSize = 2 << 24;//Gabri
	int blockSize = 2 << 16;//Fra
	for( int i = 0; i < blockSize; i++ ){

		//DES uses the last bit of the key for PARITY, therefore if the key is odd the found key is 1 less, otherwise 
		//it is the correct one.
		trialKey64 = addParity(trialKey56);
		temp_secret_key = new SecretKeySpec( trialKey64, "DES" );
		test_encrypter = new DesEncrypter( temp_secret_key );
		
		try {
			temp_decrypted = test_encrypter.decrypt( encrypted );
			if( temp_decrypted.equals( message ) ) {
				keyFound = true;
				break;
			}
		}
		catch( Exception e ){}
		//System.out.println( "Tried key:\t " + DatatypeConverter.printHexBinary( trialKey56 ));
		incrementKey( trialKey56 );
	} //end for
	
	if( keyFound ) 
		System.out.println( "Found key!:\t " + DatatypeConverter.printHexBinary( trialKey56 ));
	else 
		System.out.println( "Key NOT found!" );
	
	Long final_time = System.currentTimeMillis();
	Long calculation_time = final_time - starting_time;
	System.out.println( "Calculation time:\t " + calculation_time + " ms");	
    */
  }//End Main
}