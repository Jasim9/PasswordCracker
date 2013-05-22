import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class RequestClient extends Thread{
	public int serverPort=10000+128*20+1;
	public InetAddress serverAddress=null;
	DatagramSocket socket=null;
	
	int listeningPort=10000+128*30;
	
	int myId=0;
	
	boolean running=true;
	boolean jobGiven=false;
	
	public RequestClient(){
		try {
			serverAddress=InetAddress.getByName("localhost");
			socket=new DatagramSocket(listeningPort);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run(){
		while (running){
			if (jobGiven){
				byte[] msg=Utility.createMessage(CMessage.MAGIC, myId, CMessage.PING, "abcdef", "abcdef", Utility.hash("abcdef"));
				DatagramPacket packet=new DatagramPacket(msg,msg.length,serverAddress,serverPort);
				try {
					socket.send(packet);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(4000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void work(){
		while (running){
			byte[] msg=new byte[512];
			DatagramPacket pack=new DatagramPacket(msg,msg.length);
			try {
				socket.receive(pack);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
				System.out.println("Socket was closed.");
				continue;
			}
			CMessage mesg=Utility.openMessage(pack.getData());
			if (mesg.command==CMessage.ACK_JOB){
				myId=mesg.client_id;
				jobGiven=true;
				System.out.println("Job Acknowledged for hash: "+mesg.hash);
			}else {
				System.out.println(mesg.command+": Estimated password: "+mesg.start+": Hash of this is: "+mesg.hash);
				jobGiven=false;
			}
		}
	}
	
	public static void main(String[] ar){
		RequestClient client=new RequestClient();
		client.start();
		(new RequestDispatcher(client)).start();
		client.work();
	}

	public void packUp() {
		// TODO Auto-generated method stub
		running=false;
		socket.close();
	}

	public void sendHash(String hash) {
		// TODO Auto-generated method stub
		byte[] msg=Utility.createMessage(CMessage.MAGIC, 0, CMessage.HASH, "NEWJOB", "NEWJOB", hash);
		DatagramPacket packet=new DatagramPacket(msg,msg.length,serverAddress,serverPort);
		try {
			socket.send(packet);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void sendCancel() {
		// TODO Auto-generated method stub
		byte[] msg=Utility.createMessage(CMessage.MAGIC, myId, CMessage.CANCEL_JOB, "aCANCEL", "aCANCEL", Utility.hash("a"));
		DatagramPacket packet=new DatagramPacket(msg,msg.length,serverAddress,serverPort);
		try {
			socket.send(packet);
			jobGiven=false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

class RequestDispatcher extends Thread{

	RequestClient client=null;
	
	public RequestDispatcher(RequestClient cl){
		client=cl;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		BufferedReader in=new BufferedReader(new InputStreamReader(System.in));
		while (true){
			System.out.println(" Press q to exit. \n Press h to send a hash of password to the server for cracking.\n Press p to enter a password, and send its hash to server for cracking.\n Press c to cancel a previous given job.");
			String input=null;
			try {
				input=in.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (input.equalsIgnoreCase("q")){
				System.out.println("EXiting");
				try {
					in.close();
					client.packUp();
					Thread.sleep(20);
				} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.exit(0);
			} else if (input.equalsIgnoreCase("h")){
				String hash=null;
				System.out.print("Enter the hash of password to be sent to server:");
				try {
					hash=in.readLine();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				client.sendHash(hash);
			} else if (input.equalsIgnoreCase("p")){
				String pass=null;
				System.out.print("Enter the password the hash of which is to be sent to server:");
				try {
					pass=in.readLine();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				client.sendHash(Utility.hash(pass));
			} else if(input.equalsIgnoreCase("c")) {
				client.sendCancel();
			} else System.out.println("Wrong input. \n");
		}
	}
	
}

