//package ipa.assignment.chat;

//import ipa.assignment.echo.*;

import java.io.*;
import java.net.*;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

/**
 * JUnit test case for the chat server.
 *  
 * @author stsydow, parzy
 */
public class ChatServerTest {
	
	// test fixture -----------------------------------------------------------
	// ------------------------------------------------------------------------
	
	/**
	 * Timeout quantum used for tests. It should represent the time im 
	 * milliseconds needed to establish a TCP connection that is accepted and
	 * processed by the server. On slow computers you may want to increase this
	 * value. However, when doing so, the tests will take longer to get 
	 * completed.
	 */
	public static final int TIMEOUT = 100;

	/**
	 * Custom socket factory to be able to close all subsequently created
	 * <code>ServerSocket</code>.
	 */
	private static IpaSocketImplFactory factory = new IpaSocketImplFactory();

	/**
	 * Reference to standard out in order to restore it later,
	 */
    private static PrintStream stdout = System.out;                                       

    /**
     * Thread running the server.
     */
	private Server server;
    
	/**
	 * Sets our custom <code>SocketFactory</code> to obtain a reference to all 
	 * subsequently created <code>ServerSockets</code>.  Thus, we are able to 
	 * run each test method with a new server instance by forcibly closing the
	 * old one.
	 * @throws IOException if something goes horribly wrong
	 */
	@BeforeClass
	public static void initialize() throws IOException {
		ServerSocket.setSocketFactory(factory);
	}

	/**
	 * Creates a new server thread instance that is not yet started.
	 */
	@Before
	public void setUp() {
		server = new Server();
	}
	
	/**
	 * Restores standard out, signals all test threads to finish, and forcibly 
	 * closes all <code>ServerSocket</code>s.  The latter may cause exceptions
	 * thrown by your server implementation (as closing the socket you are 
	 * using is not that polite).
	 * @throws InterruptedException when interrupted by another thread
	 */
	@After
	public void tearDown() throws InterruptedException {
		done = true; // signal threads to stop
		System.setOut(stdout); // restore system out
		server.kill(); server = null; // kill server thread
	}
	
	
	// tests ------------------------------------------------------------------
	// ------------------------------------------------------------------------
	
	/**
	 * Starts a default server instance as well as two clients sending and 
	 * receiving a message, respectively.
	 */
	@Test(timeout=10*TIMEOUT)
	public void testDefault() throws IOException {
		// start a default sever instance and two clients
		server.start();
		Client a = new Client("localhost", 5678);
		Client b = new Client("localhost", 5678);
		sleep(TIMEOUT); // time needed by server to register new connections  
		
		// send and receive a message
		a.send("hello B");
		assertThat(b.receive(), containsString("hello B"));

		// disconnect clients
		a.close(); b.close();
	}
	
	/**
	 * Starts a server instance on port 6789 as well as two clients sending and 
	 * receiving a message, respectively.
	 */
	@Test(timeout=10*TIMEOUT)
	public void testCustomPort() throws IOException {
		// start a sever instance on a custom port and two clients
		server.start("6789");
		Client a = new Client("localhost", 6789);
		Client b = new Client("localhost", 6789);
		sleep(TIMEOUT); // time needed by server to register new connections  
		
		// send and receive a message
		a.send("hello B");
		b.assertReceive("hello B");
		
		// disconnect clients
		a.close(); b.close();
	}	
	
	/**
	 * Starts a server instance on port 5678 as well as three clients sending 
	 * and receiving messages. The detailed test scenario runs as follows:
	 * <code>
	 * 	 start Server
	 *   start Client A
	 * 	 start Client B
	 *	 talk
	 * 	 stop Client A
	 * 	 start Client C
	 * 	 talk
	 * 	 stop Client C
	 *   stop Client B
	 *   start Client A
	 * 	 start Client B
	 *	 talk
	 *   stop Client A
	 *   stop Client B
	 * </code>
	 */
	@Test(timeout=20*TIMEOUT)
	public void testReconnect() throws IOException {
		
		// start a server instance
		server.start("5678");
		
		// start two clients first and let them chat
		Client a = new Client("localhost", 5678);
		Client b = new Client("localhost", 5678);
		sleep(TIMEOUT);
		a.send("hello B");
		b.send("hi A");
		b.assertReceive("hello B");
		a.assertReceive("hi A");

		// disconnect a client, start another, and chat
		a.close();
		Client c = new Client("localhost", 5678);
		sleep(2*TIMEOUT);
		c.send("hi B");
		b.assertReceive("hi B");
		b.send("hi C");
		c.assertReceive("hi C");

		// disconnect all clients, start yet another pair, and chat again
		c.close();
		b.close();
		a = new Client("localhost", 5678);
		b = new Client("localhost", 5678);
		sleep(TIMEOUT);
		a.send("hello B");
		b.send("hi A");
		a.assertReceive("hi A");
		b.assertReceive("hello B");

		// diconnect all clients again
		a.close();
		b.close();
	}
	
	/**
	 * Starts a server instance on port 5678 as well as three clients.
	 * Two of them are sending a very long message each, which are interleaved
	 * in several chunks.  The third client should recive both messages as
	 * in whole.
	 */
	@Test(timeout=20*TIMEOUT)
	public void testLongMessages() throws IOException {
		// start a server, 2 client producers, and 1 client consumer
		server.start("5678");
		Client a = new Client("localhost", 5678);
		Client b = new Client("localhost", 5678);
		Client receiver = new Client("localhost", 5678);
		sleep(TIMEOUT);

		// send chunks of As and Bs and finally a newline
		String msgA, msgB; msgA=msgB=""; 
		for( String s="AAAAAAAA", t="BBBBBBBBBB"; 
		     msgA.length()<1000; 
			 msgA+=s, msgB+=t)
		{
			a.write(s); b.write(t);
			if (msgA.length()%(10*s.length())==0) Thread.yield();
		}
		a.send(""); b.send("");
		
		// receive messages and check their content
		String s = receiver.receive(); String t = receiver.receive();
		assertThat(s, either(containsString(msgA)).or(containsString(msgB)));
		assertThat(t, either(containsString(msgA)).or(containsString(msgB)));

		// disconnect clients
		a.close();
		b.close();
		receiver.close();
	}
	
	/**
	 * Flag indicating whether all messages were received in order.
	 */
	private boolean inOrder = true;
	
	/**
	 * Starts a server instance on port 5678 as well as three clients.
	 * The first client sends even numbers while the second produces odd 
	 * numbers.  The third client receives both number streams which should
	 * be in ascending order each.
	 */
	@Test(timeout=20*TIMEOUT)
	public void testMessageOrder() throws IOException  {

		// start server and 2 clients producing even and odd numbers, respectively
		server.start("5678");
		Client odd = new Client("localhost", 5678);
		Client even = new Client("localhost", 5678);

		// start a receiving thread that sorts received numbers into an even and
		// odd bin and, thereby, checks producer fifo order
		Thread receiver = new Thread() {
			@Override
			public void run() {
				try {
					Client client = new Client("localhost", 5678);
					int[] bins = new int[2];
					for (int i; bins[0]<500 || bins[1]<500; bins[i%2]++)
					{
						i = Integer.parseInt(client.receive());
						inOrder &= (bins[i%2]==i/2); // check order
					}
					client.close();

				} catch (Exception e) {
					e.printStackTrace();
					inOrder = false; // signal exception
				}
			}
		};
		receiver.start();
		sleep(TIMEOUT);

		// produce even and odd numbers and join the receiving thread
		for(int i = 0; i < 500; i++) {
			even.send("" + (i*2));
			odd.send("" + (i*2 + 1));
			if(i%50 == 0) Thread.yield();
		}
		sleep(TIMEOUT);
		even.close();
		odd.close();
		try { receiver.join(); } catch (InterruptedException e) {}
		
		// check assertion
		assertTrue(inOrder);
	}
	
	/**
	 * Flag signalling threads responsible for background traffic to finish.
	 */
	private boolean done = false;
	
	/**
	 * Flag indicating whether the stress test was passed.
	 */
	private boolean survived = true;
	/**
	 * Starts a server instance on port 5678 as well as two clients producing
	 * background traffic: one client sends numbers and the other should receive
	 * them in order.  Meanwhile, other clients connect to and subsequently 
	 * disconnect from the server.  As they do it quite fast, your server may
	 * try to write to an already closed socket and, thus, throw exceptions. 
	 * You are expected to handle these exceptions appropriately as 
	 * (dis)connecting should not cause harm to server.
	 * 
	 * @throws IOException because of a number of reasons 
	 *         (primarily, the server hang up).
	 */
	@Test(timeout=300*TIMEOUT)
	public void testConnectStress() throws IOException  {
		// start server
		server.start("5678");
		
		// start a sending and receiving thread that produce background traffic
		Thread sender = new Thread() {
			@Override
			public void run(){
				try {
					// start counting numbers
					Client c = new Client("localhost", 5678);
					for (int i=0; !done; i++)
					{
						c.send(""+i);
						Thread.yield();
					}
					c.close();
				} catch (IOException e) {
					e.printStackTrace();
					survived = false;
					done = true;
				}
			}
		};
		Thread receiver = new Thread() {
			@Override
			public void run(){
				try {
					// receive and compare numbers to those expected
					Client c = new Client("localhost", 5678);
					for (int i=0; !done; i++)
					{
						if( i != Integer.parseInt(c.receive()) ) {
							survived = false;
							done = true;
						}
					}
					c.close();
				} catch (IOException e) {
					e.printStackTrace();
					survived = false;
					done = true;
				}
			}
		};
		done = false;
		receiver.start();
		sleep(TIMEOUT);
		sender.start();
		
		// create 200 clients connecting to and disconnecting from the server
		Client[] clients = new Client[5];
		for (int i=0; i<200; i++)
		{
			int j = i%clients.length;
			if (clients[j]!=null) clients[j].close();
			clients[j] = new Client("localhost", 5678);
			Thread.yield();
		}
		for (int i=0; i<clients.length; clients[i++].close());

		// wait for sender and receiver
		done = true;
		try { sender.join(); } catch (InterruptedException e) {}
		try { receiver.join(); } catch (InterruptedException e) {}
		
		assertTrue(survived);
	}
	
	/**
	 * Starts a server instance on port 5678 in debug mode as well as a client.
	 * The message send by the client should appear on stdout.
	 * @throws IOException
	 */
	@Test(timeout=10*TIMEOUT) 
	public void testDebugFlag() throws IOException 
	{	
		// start server in debug mode and a client
		server.start("-debug","5678");
		Client a = new Client("localhost", 5678);
		
		// redirect server's output, send a message, and inspect debug output 
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		System.setOut(new PrintStream(buffer));
		a.send("hello");
		sleep(3*TIMEOUT);
		assertThat(""+buffer, containsString("hello"));
		
		// disconnect
		a.close();
	}
	
	
	// helper methods and private inner classes -------------------------------
	// ------------------------------------------------------------------------
	
	private void sleep(long millis)
	{
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) { /* nothing to worry */ }
	}
	
	/**
	 * A test client object which wraps a socket to simplify its handling 
	 * while testing. Additionally, input reader and output writer are provided
	 * for convenience.
	 */
	private class Client
	{			
		private Socket socket;
		private PrintStream out;
		private BufferedReader in;
	
		public Client(String host, int port) throws IOException {
			socket = new Socket(host, port);
			out = new PrintStream(socket.getOutputStream());
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		}
		public void send(String s) {
			out.println(s);
		}
		public void write(String s){
			out.print(s);
		}
		public String receive() throws IOException {
			return in.readLine();
		}
		public void assertReceive(String msg) throws IOException {
			assertThat(receive(), containsString(msg));
		}
		public void close() throws IOException {
			in.close();
			out.close();
			socket.close();
		}
	}

	/**
	 * Thread running the server under test. Ensures that the server has 
	 * enough time initialize when started and makes shure that the server
	 * is stopped by interrupting it and forcibly closing its socket.
	 */
	private class Server extends Thread
	{
		private String args[] = new String[0];

		@Override
		public void start() {
			super.start();
			ChatServerTest.this.sleep(TIMEOUT);
		}
		public void start(String... args) {
			this.args = args;
			start();
		}
		public void run() {
			ChatServer.main(args);
		}
		public void kill() throws InterruptedException {
			interrupt();
			factory.closeAll();
			join();
		}
	}	  
}
