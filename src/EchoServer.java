
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/*
 * 
 * 	An EchoServer implementation
 * 
 * 	start with java EchoServer [port]
 * 
 */

public class EchoServer extends Thread {
	
	private Socket socket;
	private boolean debug;

	public EchoServer(Socket socket,boolean debug){
		this.socket = socket;
		this.debug = debug;
	}
	
	public void run(){
		try{
			
		//-- get In/Out Streams
		BufferedReader	in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), "US-ASCII");
		
		while(true){
		//-- get message from Input stream
		String s = in.readLine();
		
		if(s == null){
			//terminate thread when Client issued "quit" (sends null)
			break;
		}
		
		if(debug == true){
			System.out.print(s);
		}
		
		//-- send message back to Client
		out.write("Echo:" + s + "\n");
		out.flush();
		
		//System.out.println("message echoed!");
		
		}
		
		//System.out.println("thread stopped!");
			
		} catch(IOException e){}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int port = 5678;
		boolean debug = false;
		
		
		//--parse args[]
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
			
		//-- start ne server
		ServerSocket server = new ServerSocket(port);
		//System.out.println("server created!");
		
		while(true){
		
		//-- wait for connection and start new thread to handle the Client
		Socket socket = server.accept();
		//System.out.println("connection!");
		new EchoServer(socket,debug).start();
		}

		} catch(IOException e) {/*System.out.println("IOException!"); */}
		
		
	}

}
