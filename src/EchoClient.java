import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.net.Socket;

/*
 * 
 * 	An EchoClient to use with EchoServer
 * 
 * 	start with java EchoClient [ip]:[port]
 * 
 */


public class EchoClient {

	/**
	 * @param args
	 */
	public static void main(String[] args){
		// TODO Auto-generated method stub
		
		int port = 5678;
		String ip = "127.0.0.1";
		
		
		//-- parse args[]
		if(args.length > 0){
			String[] argsSplit = args[0].split(":");
		
			if(argsSplit.length > 0 && !argsSplit[0].isEmpty()){
				ip = argsSplit[0];
			}
		
			if(argsSplit.length > 1 && !argsSplit[1].isEmpty()){
				port = Integer.parseInt(argsSplit[1]);
			}
		}
		
		try{
			//-- establish new connection
			Socket socket = new Socket(ip,port);
		
			//-- get In/Out Streams
			InputStream in =socket.getInputStream();
			OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), "US-ASCII");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));		//bufferedReader for console inputs
			BufferedReader br2 = new BufferedReader(new InputStreamReader(in));
		
			while(true){
				// read string from console
				String s = br.readLine();
				if (s.equals("quit")){
					//close socket when "quit" was issued
					in.close();
					out.close();
					socket.close();
					break;
				}
			
				//-- send message to server
				out.write(s +"\n");
				out.flush();
		
				//-- receive message from server and print it
				String s2 = br2.readLine();
				System.out.println(s2);
			}
		
		} catch(IOException e){System.out.println("connecting not possible. Maybe the ip/port is wrong?");};
		
		
	}

}
