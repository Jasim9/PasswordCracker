import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class Cracker {
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int roll=128;
		int port=10000+roll*20;
		new Cracker(port);
//		String a="daaaaa";
		
	//	System.out.println(Utility.getRange(1,a));
		
	}
	
	private DatagramSocket socket=null;
//	private DatagramSocket listenerSocket=null;
	Map<String,JobStatus> job_track=null;
	Map<Integer,Boolean> client_track=null;		// True BUSY, False IDLE
	Map<Integer,InetAddress> client_address=null;
	Map<Integer,Integer> client_port=null;
	Map<Integer,Long> client_lastPing=null;
	
	Map<Integer,String> client_job_track=null;
	
	InetAddress request_client_address=null;
	int request_client_port=0;
	int request_client_id;
	long request_client_lastPing;
	boolean request_client_alive=false;
	
	String hash=null;
	
	boolean working=false;
	boolean found=false;
	
	Random generator=new Random(Math.round(Math.random()));
	
	public Cracker(int port){
		try {
			socket=new DatagramSocket(port+1);
//			listenerSocket=new DatagramSocket(port+1);
		} catch (SocketException e) {
			e.printStackTrace();
			System.out.println("Error creating socket");
			System.exit(0);
		}
		
		client_track=new HashMap<Integer,Boolean>();
		client_address=new HashMap<Integer,InetAddress>();
		client_port=new HashMap<Integer,Integer>();
		client_lastPing=new HashMap<Integer,Long>();
	
		client_job_track=new HashMap<Integer,String>();
		
		job_track=new HashMap<String,JobStatus>();
		/** Initialize the job track **/
		String a="aa";
		while (!a.equals("99")){
			job_track.put(a, JobStatus.NOT_ASSIGNED);
			a=Utility.getNextString(a, a.length()-1, a.length());
		}
		job_track.put(a, JobStatus.NOT_ASSIGNED);
		if (socket!=null){
			start();
		}
	}
	
	public void start(){
		byte[] message=new byte[256];
		DatagramPacket packet=new DatagramPacket(message,message.length);
		System.out.println("Server Started.");
		(new Dead(this)).start();
		(new Pinger(this)).start();
		while(true){
			try {
				socket.receive(packet);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}			
			Thread t=new Worker(this, packet);
			t.start();
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
		
	public void notifyDone(String pass,String hash){
		working=false;
		found=true;
		cancelAll();
		byte[] msg=Utility.createMessage(CMessage.MAGIC, request_client_id, CMessage.DONE_FOUND, pass, pass, hash);
		sendMessage(msg,request_client_address,request_client_port);
		System.out.println("Client notified of job completion.");
	}

	public void updateWork(String id, JobStatus status,int client_id){
		synchronized (job_track){
			job_track.put(id,status);
		}
		synchronized(client_track){
			if (status==JobStatus.IN_PROGRESS){
				client_track.put(client_id, true); 		// the worker client is now busy
			} else client_track.put(client_id, false);
			System.out.println("Client status changed..."+status);
		}
		synchronized (client_job_track){
			if (status==JobStatus.IN_PROGRESS){
				client_job_track.put(client_id, id);
			}
		}
	}
	
	synchronized public void addClient(InetAddress adr,int port){
		int id=generator.nextInt();
		client_address.put(id,adr);
		client_port.put(id,port);
		client_track.put(id, false);
		client_lastPing.put(id, System.currentTimeMillis());
		sendMessage(Utility.createMessage(CMessage.MAGIC, id, CMessage.ACK_JOIN, "abcdef", "abcdef", Utility.hash("a")), adr, port);
		System.out.println("Worker Client joined the server, IP:"+adr+" , PORT:"+port+" , Assigned ID:"+id);
	}
	
	public void cancelAll() {
		// TODO Auto-generated method stub
		for (Map.Entry<Integer, InetAddress> client: client_address.entrySet()){
			byte[] msg=Utility.createMessage(CMessage.MAGIC, 0, CMessage.CANCEL_JOB, "aaaaaa", "aaaaaa", Utility.hash("aa"));
			sendMessage(msg,client.getValue(),client_port.get(client.getKey()));
		}
	}

	/** %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% */
	/** NEW JOB JOB JoB***/
	public void newJob(InetAddress clientAddress, int clientPort, String _hash) {
		// TODO Auto-generated method stub
		this.request_client_address=clientAddress;
		this.request_client_port=clientPort;
		request_client_id=generator.nextInt();
		this.hash=_hash;
		byte[] masg=Utility.createMessage(CMessage.MAGIC, request_client_id, CMessage.ACK_JOB, "aaaaaa", "aaaaaa",hash);
		sendMessage(masg,clientAddress,clientPort);
		working=true;
		while (working){
			int workerid=findWorker();
			System.out.println("Worker found:"+workerid);
			String jobid=findWork();
			System.out.println("Work found:"+jobid);
			if (workerid!=0)
				assignWork(workerid,jobid);
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (!found && request_client_alive){
			byte[] masag=Utility.createMessage(CMessage.MAGIC, request_client_id, CMessage.DONE_NOT_FOUND, "NOTFND", "aaaaaa",hash);
			sendMessage(masag,clientAddress,clientPort);
		}
		resetState();
	}
	synchronized private void resetState() {
		System.out.println("System resetting.");
		found=false;
		String a="aa";
		while (!a.equals("99")){
			job_track.put(a, JobStatus.NOT_ASSIGNED);
			a=Utility.getNextString(a, a.length()-1, a.length());
		}
		job_track.put(a, JobStatus.NOT_ASSIGNED);
		for (Map.Entry<Integer, Boolean> client: client_track.entrySet()){
			client_track.put(client.getKey(), false);
		}
	}

	/** $$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ */

	private void assignWork(int workerid, String jobid) {
		// TODO Auto-generated method stub
		String jobStart=jobid+"aaaa";
		String jobEnd=Utility.getRange(1, jobStart);
		byte[] msg=Utility.createMessage(CMessage.MAGIC, workerid, CMessage.JOB, jobStart, jobEnd, hash);
		sendMessage(msg,client_address.get(workerid),client_port.get(workerid));
		System.out.println("Work '"+jobid+"' assigned to worker:"+workerid+".");
	}

	private void sendMessage(byte[] msg, InetAddress inetAddress,
			Integer port) {
		// TODO Auto-generated method stub
		DatagramPacket pckt=new DatagramPacket(msg, msg.length, inetAddress, port);
		try {
			socket.send(pckt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String findWork() {
		// TODO Auto-generated method stub
		synchronized (job_track){
			for (Map.Entry<String, JobStatus> job: job_track.entrySet()){
				if (job.getValue()==JobStatus.NOT_ASSIGNED){
					return job.getKey();
				}
			}
		}
		return null;
	}

	private int findWorker() {
		// TODO Auto-generated method stub
		synchronized(client_track){
			for (Map.Entry<Integer,Boolean> worker: client_track.entrySet()){
				if (!worker.getValue()){	// True BUSY, False IDLE
					return worker.getKey();
				}
			}
		}
		return 0;
	}
	
	public void sendPingToAll(){
		for (Map.Entry<Integer, InetAddress> client: client_address.entrySet()){
			byte[] msg=Utility.createMessage(CMessage.MAGIC, 0, CMessage.PING, "aaaaaa", "aaaaaa", Utility.hash("aa"));
			sendMessage(msg,client.getValue(),client_port.get(client.getKey()));
		}
	}
	
	public void checkClients(){
		long currentTime=System.currentTimeMillis();
		if (request_client_alive && currentTime-request_client_lastPing>15000){
			request_client_alive=false;
			System.out.println("Request client is now dead");
			stopWork();
			cancelAll();
		}
		ArrayList<Integer> toRemove=new ArrayList<Integer>();
		synchronized(client_track){
			for (Map.Entry<Integer,Long> workerPing: client_lastPing.entrySet()){
				if (currentTime-workerPing.getValue()>15000){
					client_track.remove(workerPing.getKey());
					client_address.remove(workerPing.getKey());
					client_port.remove(workerPing.getKey());
					toRemove.add(workerPing.getKey());
				}
			}
			for (int i=0; i<toRemove.size();i++){
				client_lastPing.remove(toRemove.get(i));
				System.out.println("A Worker client is now dead. of ID:"+toRemove.get(i));
			}
		}
		synchronized (job_track){
			for (int i=0; i<toRemove.size();i++){ // Array of dead worker clients 
				//get the id of the Job on which this worker was working.
				String id=client_job_track.get(toRemove.get(i));
				// change the status of this job to NOT_ASSIGNED.
				if (id!=null)
					job_track.put(id, JobStatus.NOT_ASSIGNED);
			}			
		}
	}
	
	public void stopWork() {
		working=false;
	}

	
	public void updateRequestClient(){
		request_client_alive=true;
		request_client_lastPing=System.currentTimeMillis();
		System.out.println("Ping from request client...");
	}
	
	public void updatePing(int clientId){
		synchronized(client_lastPing){
			System.out.println("Ping received for worker client id:"+clientId);
			client_lastPing.put(clientId, System.currentTimeMillis());
		}
	}
	
	/* INNER CLASS PINGER */
	class Pinger extends Thread {
		Cracker cracker;
		boolean tr=true;
		public Pinger(Cracker cr){
			cracker=cr;
		}
		public void run(){
			while (tr){
				try {
					Thread.sleep(4000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				cracker.sendPingToAll();
			}
		}
		public void off(){
			tr=false;
		}
	}
	/* INNER CLASS DEAD CHECKER */
	class Dead extends Thread{
		Cracker cracker;
		public Dead(Cracker ck){
			cracker=ck;
		}
		public void run(){
			while (true){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				cracker.checkClients();
			}
		}
	}	
}

/** WORKER CLASS **/

class Worker extends Thread{
	Cracker cracker=null;
	DatagramPacket packet=null;
	InetAddress clientAddress=null;
	int clientPort;
	
	public Worker (Cracker parent,DatagramPacket pck){
		cracker=parent;
		packet=pck;
		clientAddress=pck.getAddress();
		clientPort=pck.getPort();
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
	public void run(){
		byte[] data=packet.getData();
		CMessage msg=Utility.openMessage(data);
		
		if (msg.magic==CMessage.MAGIC){ // only if this message is supposed to be for our application.
//			System.out.println("command:"+msg.command+", start:"+msg.start+", end:"+msg.end);
			
			switch (msg.command){
			case CMessage.PING:
				cracker.updateRequestClient();
				break;
			case CMessage.REQUEST_TO_JOIN:
				cracker.addClient(clientAddress, clientPort);
				break;
			case CMessage.JOB:
				// Server should not receive job messages.
				break;
			case CMessage.ACK_JOB:
				String m2=msg.start.substring(0, 2);
				System.out.println("ACK JOB received for job :"+m2);
				cracker.updateWork(m2,JobStatus.IN_PROGRESS,msg.client_id);
				break;
			case CMessage.DONE_NOT_FOUND:
				String m=msg.start.substring(0, 2);
				cracker.updateWork(m, JobStatus.DONE,msg.client_id);
				break;
			case CMessage.DONE_FOUND:
				String m1=msg.start.substring(0, 2);
				cracker.updateWork(m1, JobStatus.DONE,msg.client_id);
				cracker.notifyDone(msg.start,msg.hash);
				break;
			case CMessage.NOT_DONE:
				// Don't do anything
				cracker.updatePing(msg.client_id);
				break;
			case CMessage.IDLE:
				// Don't do anything
				cracker.updatePing(msg.client_id);
				break;
			case CMessage.CANCEL_JOB:
				cracker.stopWork();
				cracker.cancelAll();
				break;
			case CMessage.HASH:
				cracker.updateRequestClient();
				cracker.newJob(clientAddress,clientPort,msg.hash);
				break;
			}
		}
	}
}
