import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class WorkerClient {
	public static void main(String[] arg){
		int serverPort=10000+128*20+1;
		InetAddress serverAddr=null;
		int listnPort;
		if (arg[0]!=null)
			listnPort=Integer.valueOf(arg[0]);
		else listnPort=88;
		try {
			serverAddr=InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WorkerClient client=new WorkerClient(serverPort,serverAddr,listnPort);
		client.start();
	}
	
	int listeningPort;
	DatagramSocket socket=null;
	
	InetAddress serverAddress=null;
	int serverPort=0;
	
	int myId=0;
	boolean running;
	int currentState=CMessage.IDLE;
	
	boolean toldTheServer=false;
	
	boolean searching=false;
	String found="xxxxxx";
	
	public WorkerClient(int server, InetAddress serv,int listnPort){
		serverPort=server;
		serverAddress=serv;
		running=true;
		listeningPort=10000+128*listnPort+1;
		try {
			socket=new DatagramSocket(listeningPort);
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			System.out.println("Unable to create socket exception");
		}
	}
	
	public void start(){
		byte[] msg=new byte[256];
		byte[] arr=Utility.createMessage(CMessage.MAGIC, myId, CMessage.REQUEST_TO_JOIN, found, found, Utility.hash(found));
		sendMessageToServer(arr);
		DatagramPacket packet=new DatagramPacket(msg,msg.length);
		try{
			while (running){
				socket.receive(packet);
				byte[] data=packet.getData();
				CMessage mesg=Utility.openMessage(data);
				if (mesg.magic==CMessage.MAGIC){					
					(new Killer(mesg,this)).start();
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		finally{
			socket.close();
		}
	}

	public void breakHash(String start,String end,String hash) {
		// TODO Auto-generated method stub
		searching=true;
		currentState=CMessage.NOT_DONE;
		toldTheServer=false;
		String str=start;
		int i=0;
		while (searching){
			if (i++%500000==0) System.out.println("Searching... "+str);
			if (Utility.hash(str).equals(hash)){
				found=str;
				searching=false;
				currentState=CMessage.DONE_FOUND;
			} else {
				str=Utility.getNextString(str, str.length()-1, str.length());
			}
			if (str.equals(end)){
				searching=false;
				currentState=CMessage.DONE_NOT_FOUND;
			}
		}
	}
	
	public void sendMessageToServer(byte[] msg){
		DatagramPacket pckt=new DatagramPacket(msg,msg.length,serverAddress,serverPort);
		try {
			socket.send(pckt);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error sending packet.");
		}
	}

	public void setid(int client_id) {
		// TODO Auto-generated method stub
		myId=client_id;
	}

	public void cancel() {
		// TODO Auto-generated method stub
		searching=false;
		currentState=CMessage.IDLE;
	}

	public void reply() {
		// TODO Auto-generated method stub
		byte[] msg=Utility.createMessage(CMessage.MAGIC, myId, currentState, found, found, Utility.hash(found));
		sendMessageToServer(msg);
		if (!toldTheServer && (currentState==CMessage.DONE_NOT_FOUND || currentState==CMessage.DONE_FOUND)){
			toldTheServer=true;
			currentState=CMessage.IDLE;
		}
	}
	
	public void sendAck(String start){
		byte[] msg=Utility.createMessage(CMessage.MAGIC, myId, CMessage.ACK_JOB, start, found, Utility.hash(found));
		sendMessageToServer(msg);
	}
	
}

class Killer extends Thread{
	CMessage msg=null;
	WorkerClient client=null;
	public Killer(CMessage m, WorkerClient cl){
		msg=m;
		client=cl;
	}
	
	public void run(){
		switch (msg.command){
		case CMessage.ACK_JOIN:
			client.setid(msg.client_id);
			break;
		case CMessage.JOB:
			client.setid(msg.client_id);
			client.sendAck(msg.start);
			client.breakHash(msg.start,msg.end,msg.hash);
			break;
		case CMessage.CANCEL_JOB:
			client.cancel();
			break;
		case CMessage.PING:
			client.reply();
			break;
		default:
			break;
		}
	}
}
