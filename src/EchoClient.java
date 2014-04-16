import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.net.Socket;


public class EchoClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		int port = 5678;
		String ip = "127.0.0.1";
		
		if(args.length == 1){
			if(args[0].startsWith(":")){
				port = Integer.parseInt(args[0].substring(1));
			}
			else{
				ip = args[0];
			}
		}
		
		if(args.length == 2){
			ip = args[0];
			port = Integer.parseInt(args[1]);
		}
		
		Socket socket = new Socket(ip,port);
		
		InputStream in =socket.getInputStream();
		OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream(), "US-ASCII");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		BufferedReader br2 = new BufferedReader(new InputStreamReader(in));
		
		while(true){
			String s = br.readLine();
			if (s.equals("quit")){
				in.close();
				out.close();
				socket.close();
				break;
			}
			out.write(s +"\n");
			out.flush();
		
		
			String s2 = br2.readLine();
			System.out.println(s2);
		}
		
		
	}

}
