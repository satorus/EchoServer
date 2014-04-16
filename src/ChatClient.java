import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;


public class ChatClient extends Thread {
	
	private Socket socket;
	private String nick;
	private static boolean shutdown = false;
	
	public ChatClient(Socket socket,String nick){
		this.socket = socket;
		this.nick = nick;
	}
	
	public void run(){
		try{
			OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), "US-ASCII");
			BufferedReader sysIn = new BufferedReader(new InputStreamReader(System.in));
			InputStream in =socket.getInputStream();
		
			while(true){
				String s = sysIn.readLine();
				if (s.equals("quit")){
					shutdown = true;
					in.close();
					out.close();
					socket.close();
					break;
				}
				out.write(nick + ":" + s +"\n");
				out.flush();
		}
		
		} catch(IOException e){}
	}
	
	


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		String nick ="";
		String host = "127.0.0.1";
		int port = 5678;
		
		for (int i = 0; i < args.length; i++) {
			if (!isNumeric(args[i])) {
				if (nick.equals("")){
					nick = args[i];
				}
				else{
					host = args[i];
				}
			}
			if (isNumeric(args[i])) {
				port = Integer.parseInt(args[i]);
			}
		}
		
		try{
		Socket socket = new Socket(host,port);
		
		new ChatClient(socket,nick).start();
		
		InputStream in =socket.getInputStream();
		BufferedReader socketIn = new BufferedReader(new InputStreamReader(in));
		
		while(!shutdown){
			String chatMessage = socketIn.readLine();
			System.out.println(chatMessage);
		}
		
		} catch(IOException e){}
	}
	
	public static boolean isNumeric(String str) {
		try {
			int d = Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

}
