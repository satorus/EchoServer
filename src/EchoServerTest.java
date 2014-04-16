//package ipa.assignment.echo;

import java.io.*;
import java.net.*;

import org.junit.*;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;

/**
 * JUnit test case for the echo server.
 *  
 * @author stsydow, parzy
 */
public class EchoServerTest {

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
		System.setOut(stdout); // restore system out
		server.kill(); server = null; // kill server thread
	}
	
  
	// tests ------------------------------------------------------------------
	// ------------------------------------------------------------------------
	
	/**
	 * Starts a default server instance as well as a client sending and 
	 * receiving a message.
	 */
	@Test(timeout=10*TIMEOUT)
	public void testDefault() throws IOException {
		// start server and client
		server.start();
		Client a = new Client("localhost", 5678);
		
		// send and receive a message
		a.send("hello world");
		a.assertReceive("hello world");   
		a.close();	
	}
	
	/**
	 * Starts a server instance on port 6789 as well as a client sending and 
	 * receiving a message.
	 */
	@Test(timeout=10*TIMEOUT)
	public void testCustomPort() throws IOException {
		// start server and client
		server.start("6789");
		Client a = new Client("localhost", 6789);
		
		// send and receive a message
		a.send("hello");
		a.assertReceive("hello");   
		a.close();
	}	
	
	/**
	 * Starts a default server instance on port 5678 as well as two clients 
	 * one after the other that send and receive a message, each. 
	 */
	@Test(timeout=20*TIMEOUT)
	public void testReconnect() throws UnknownHostException, IOException {
		// start server
		server.start("5678");
		
		// connect first client, send and receive a message, disconnect
		Client a = new Client("localhost", 5678);
		a.send("hello");
		a.assertReceive("hello");   
		a.close();
		
		// connect second client, send and receive a message, disconnect
		Client b = new Client("localhost", 5678);
		b.send("world");
		b.assertReceive("world");   
		b.close();
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
			EchoServerTest.this.sleep(TIMEOUT);
		}
		public void start(String... args) {
			this.args = args;
			start();
		}
		public void run() {
			EchoServer.main(args);
		}
		public void kill() throws InterruptedException {
			interrupt();
			factory.closeAll();
			join();
		}
	}	 
}
