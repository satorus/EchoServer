//package ipa.assignment.echo;

import java.lang.reflect.*;
import java.net.*;

/**
 * Custom socket factory enabling us to keep a reference to all subsequently 
 * created sockets.  Thus, we are able to forcibly close them despite of 
 * uninterruptable system calls.
 * 
 * @author parzy, stsydow
 */
public class IpaSocketImplFactory implements SocketImplFactory
{
	/**
	 * Lastly created server socket implementation.
	 */
	private SocketImpl instance;
	
	/**
	 * Allows the instantiation of one socket implementation and keeps a
	 * reference to it.
	 */
	@Override
	synchronized public SocketImpl createSocketImpl() {
		// allow just one server socket instance
		if(instance != null){
			throw new RuntimeException("One ServerSocket should be enough.");
		}
		// instantiate default socket implementation via reflection as class
		// is not visible outside its package and keep a reference to it
		try {
			Class<?> clazz = Class.forName("java.net.SocksSocketImpl");
			Constructor<?> con = clazz.getDeclaredConstructor((Class<?>[])null);
			con.setAccessible(true);
			return (instance = (SocketImpl)con.newInstance((Object[])null));			
		} 
		// rethrow all exceptions as runtime exceptions that need no checking
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Closes the lastly created server socket.
	 */
	synchronized public void closeAll(){
		// call close on socket implementation via reflection
		try {
			Class<?> clazz = SocketImpl.class;
			Method close = clazz.getDeclaredMethod("close", (Class<?>[])null);
			close.setAccessible(true);
			close.invoke(instance, (Object[])null);
			instance = null;
		} 
		// rethrow all exceptions as runtime exceptions that need no checking
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}