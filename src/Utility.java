import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;


public class Utility {

	static int WORK_UNIT=(int)Math.pow(62,4);	// WORK UNIT SIZE=62^4. LENGTH OF STRINGS=6; e.g. aa**** are the strings in one work unit
	
	static byte[] getBytes(String str){
		byte[] arr=null;
		try {
			arr= str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return arr;
	}
	static byte[] getBytes(int n){
		byte[] n1=ByteBuffer.allocate(4).putInt(n).array();
		return n1;
	}
	static String hash(String str){
		MessageDigest md=null;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] mdbytes=md.digest(str.getBytes()); 

		//convert the byte to hex format method 2
		StringBuffer hexString = new StringBuffer();
		for (int i=0;i<mdbytes.length;i++) {
			String hex=Integer.toHexString(0xff & mdbytes[i]);
			if(hex.length()==1) hexString.append('0');
			hexString.append(hex);
		}
		//    	System.out.println("Digest(in hex format):: " + hexString.toString());
		return hexString.toString();
	}
	
	static public String getNextString(String str, int pos, int length){
		if (str.charAt(pos)!='9'){
			char[] chars = str.toCharArray();
			if (chars[pos]=='z'){
				chars[pos]='A';
			}else if (chars[pos]=='Z'){
				chars[pos]='0';
			}else{
				chars[pos]=(char) (chars[pos]+1);
			}
			for (int i=pos+1;i<length;i++)
				chars[i]='a';
			str=String.valueOf(chars);
		} else {
			str=getNextString(str,pos-1,length);
		}
		return str;
		
	}
	
	static public String getLast(String str, int n){		// OLD METHOD
		for (int i=0; i<n;i++){
			str=getNextString(str,str.length()-1,str.length());
		}
		return str;
	}
	
	static public String getNth(String str,int n){			// NEW METHOD
		char[] chars = str.toCharArray();
		for (int i=0; i<str.length() ; i++){
			int d=(int)Math.pow(62, str.length()-i-1);
			int q=n/d;
			if (q>0){
				for (int j=0; j<q; j++)
					chars[i]=getNext(chars[i]);
				if (n%d==0) break;
				else n=n%d;
			}
		}
		return new String(chars);
	}
	
	static public char getNext(char c){
		if (c=='z'){
			return 'A';
		}else if (c=='Z'){
			return '0';
		}else if (c=='9'){
			return 'a';
		} else {
			return (char) (c+1);
		}
	}
	
	static public String getRange(int number_of_work_units, String start){
		return getNth(start,number_of_work_units*WORK_UNIT);
	}
	/**
	 PROTOCOL
	 
	 Magic: unsigned int (4 bytes) 
	 Client_ID: unsigned int (4 bytes)
	 Command: int (4 bytes)
	 Key_Range_Start: Char[6]
	 Key_Range_End: Char[6]
	 HASH: Char[32]
	 TOTAL CAPACITY=56;
	 **/
	public static byte[] createMessage(int magic,int id, int command,String start, String end, String hash){
		byte[] magicB=ByteBuffer.allocate(4).putInt(magic).array();
		byte[] idB=ByteBuffer.allocate(4).putInt(id).array();
		byte[] commandB=ByteBuffer.allocate(4).putInt(command).array();
		byte[] startB=start.getBytes();
		byte[] endB=end.getBytes();
		byte[] hashB=hash.getBytes();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			outputStream.write(magicB);
			outputStream.write(idB);
			outputStream.write(commandB);
			outputStream.write(startB);
			outputStream.write(endB);
			outputStream.write(hashB);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		byte[] array=outputStream.toByteArray();
		try {
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return array;
	}
	/**
	 PROTOCOL
	 
	 Magic: unsigned int (4 bytes) 
	 Client_ID: unsigned int (4 bytes)
	 Command: int (4 bytes)
	 Key_Range_Start: Char[6]
	 Key_Range_End: Char[6]
	 HASH: Char[32]
	 TOTAL CAPACITY=56;
	 **/
	public static CMessage openMessage(byte[] data){
		ByteBuffer buf=ByteBuffer.wrap(data);
		int magic=buf.getInt();
		int id=buf.getInt();
		int command=buf.getInt();
		byte[] startB=Arrays.copyOfRange(data, 12, 18);
		byte[] endB=Arrays.copyOfRange(data, 18, 24);
		byte[] hashB=Arrays.copyOfRange(data, 24, 56);
		return new CMessage(magic,id,command,new String(startB),new String(endB),new String(hashB));
	}
}
