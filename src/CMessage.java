
public class CMessage {
	static final int PING=0;
	static final int REQUEST_TO_JOIN=1;
	static final int JOB=2;
	static final int ACK_JOB=3;
	static final int DONE_NOT_FOUND=4;
	static final int DONE_FOUND=5;
	static final int NOT_DONE=6;
	static final int CANCEL_JOB=7;
	static final int HASH=8;
	static final int ACK_JOIN=9;
	static final int IDLE=10;
	static final int MAGIC=15440;
	
	public int magic,client_id,command;
	public String start,end,hash;
	
	public CMessage(int _magic,int _client_id,int _command,String _start, String _end, String _hash){
		magic=_magic;
		client_id=_client_id;
		command=_command;
		start=_start;
		end=_end;
		hash=_hash;
	}
	
}
