import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Vector;


public class ChatServer extends Thread {

	private Socket socket;				//the socket the thread belongs to
	private Vector<Socket> sockets;		//list of connected sockets (Vector for Thread-Safety)
	private boolean debug;

	public ChatServer(Socket socket, Vector<Socket> sockets,boolean debug){
		this.socket = socket;
		this.sockets = sockets;
		this.debug = debug;
	}
	
	public void run(){
		try{
		BufferedReader	in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), "US-ASCII");
		
		while(true){
		String s = in.readLine();
		
		if(s == null){
			sockets.remove(socket);
			break;
		}
		
		if(debug == true){
			System.out.print(s);
		}
		
		try{
			distributeMessage(s);
		} catch(IOException e){
			e.printStackTrace();
		}
		
		
		//System.out.println("message echoed!");
		
		}
		
		//System.out.println("thread stopped!");
			
		} catch(IOException e){}
	}
	
	public synchronized void distributeMessage(String s) throws IOException{
		try{
			for(int i = 0;i < sockets.size();i++){
				if(sockets.get(i) != socket){
					OutputStreamWriter outTemp = new OutputStreamWriter(sockets.get(i).getOutputStream(), "US-ASCII");
					outTemp.write(s + "\n");
					outTemp.flush();
				}
			}
		} catch(SocketException e){}
		
		/*for(Socket socketTemp : sockets){
			if(socketTemp != socket){
				OutputStreamWriter outTemp = new OutputStreamWriter(socketTemp.getOutputStream(), "US-ASCII");
				outTemp.write(s + "\n");
				outTemp.flush();
			}
		}
		
		*/
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int port = 5678;
		boolean debug = false;
		Vector<Socket> sockets = new Vector<>();
		
		if (args.length == 1){
			if(args[0].equals("-debug")){
				debug = true;
			}
			else {
			port = Integer.parseInt(args[0]);
			}
		}
		if (args.length == 2){
			if(args[0].equals("-debug")){
				debug = true;
				port = Integer.parseInt(args[1]);
			}
			
		}

		//System.out.println(port);
		
		try{
		ServerSocket server = new ServerSocket(port);
		//System.out.println("server created!");
		
		while(true){
		Socket socket = server.accept();
		sockets.add(socket);
		//System.out.println("connection!");
		new ChatServer(socket,sockets,debug).start();
		}

		} catch(IOException e) {/*System.out.println("IOException!"); */}
		
		
	}


}
