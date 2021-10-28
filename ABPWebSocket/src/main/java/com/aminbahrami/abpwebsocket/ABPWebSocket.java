package com.aminbahrami.abpwebsocket;

import android.util.Log;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;


public class ABPWebSocket
{
	private String url="";
	
	private boolean connected=false;
	
	private IOnConnectListener iOnConnectListener=null;
	private IOnDisconnectListener iOnDisconnectListener=null;
	private IOnMessageListener iOnMessageListener=null;
	private IOnErrorListener iOnErrorListener=null;
	
	private int timeout=3000;
	
	private boolean debug=true;
	
	private io.socket.client.Socket socket;
	
	public void connect()
	{
		try
		{
			TrustManager[] trustAllCerts=new TrustManager[]{new X509TrustManager()
			{
				@Override
				public void checkClientTrusted(X509Certificate[] x509Certificates,String s) throws CertificateException
				{
					
				}
				
				@Override
				public void checkServerTrusted(X509Certificate[] x509Certificates,String s) throws CertificateException
				{
					
				}
				
				@Override
				public X509Certificate[] getAcceptedIssuers()
				{
					
					return new X509Certificate[]{};
				}
			}};
			
			HostnameVerifier myHostnameVerifier=new HostnameVerifier()
			{
				@Override
				public boolean verify(String s,SSLSession sslSession)
				{
					return true;
				}
			};
			
			SSLContext mySSlContext=SSLContext.getInstance("TLS");
			mySSlContext.init(null,trustAllCerts,null);
			
			
			OkHttpClient okHttpClient=new OkHttpClient.Builder()
					.hostnameVerifier(myHostnameVerifier)
					.sslSocketFactory(mySSlContext.getSocketFactory(),new X509TrustManager()
					{
						@Override
						public void checkClientTrusted(X509Certificate[] x509Certificates,String s) throws CertificateException
						{
							
						}
						
						@Override
						public void checkServerTrusted(X509Certificate[] x509Certificates,String s) throws CertificateException
						{
							
						}
						
						@Override
						public X509Certificate[] getAcceptedIssuers()
						{
							return new X509Certificate[]{};
							//return new X509Certificate[0];
						}
					})
					.build();
			
			// default settings for all sockets
			IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
			IO.setDefaultOkHttpCallFactory(okHttpClient);
			
			
			IO.Options options=new IO.Options();
			options.forceNew=true;
			options.reconnection=true;
			options.reconnectionDelay=getTimeout();
			
			options.timeout=getTimeout();
			
			//SSL[
			options.callFactory=okHttpClient;
			options.webSocketFactory=okHttpClient;
			//SSL]
			
			
			URI uri=new URI(this.url);
			
			
			if(socket!=null)
			{
				socket.off();
				socket.close();
				socket.disconnect();
			}
			
			socket=IO.socket(uri,options);
			
			socket.on(Socket.EVENT_CONNECT,new Emitter.Listener()
			{
				
				@Override
				public void call(Object... args)
				{
					log("onOpen");
					
					connected=true;
					
					if(iOnConnectListener!=null)
					{
						iOnConnectListener.onConnect();
					}
				}
			}).on(Socket.EVENT_DISCONNECT,new Emitter.Listener()
			{
				
				@Override
				public void call(Object... args)
				{
					log("ABPWebSocket: "+"onClose");
					
					connected=false;
					
					if(iOnDisconnectListener!=null)
					{
						iOnDisconnectListener.onDisconnect();
					}
				}
			}).on("message",new Emitter.Listener()
			{
				
				@Override
				public void call(final Object... args)
				{
					log("ABPWebSocket: "+"onMessage: "+args[0]);
					
					if(iOnMessageListener!=null)
					{
						if(args[0]!=null)
						{
							iOnMessageListener.onMessage(args[0].toString());
						}
					}
				}
			}).on(Socket.EVENT_CONNECT_ERROR,new Emitter.Listener()
			{
				
				@Override
				public void call(final Object... args)
				{
					log("ABPWebSocket: "+"onError: "+args[0]);
					
					if(iOnErrorListener!=null)
					{
						iOnErrorListener.onError(args[0].toString());
					}
				}
			}).on(Socket.EVENT_CONNECT_ERROR,new Emitter.Listener()
			{
				@Override
				public void call(Object... args)
				{
					log("ABPWebSocket: Reconnect Failed");
					
					if(!socket.connected())
					{
						socket.connect();
					}
				}
			});
			
			if(!socket.connected())
			{
				socket.connect();
			}
		}
		catch(URISyntaxException|KeyManagementException|NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}
	}
	
	public void disconnect()
	{
		disconnect(true);
	}
	
	public void disconnect(boolean callListener)
	{
		if(socket!=null)
		{
			socket.off();
			socket.close();
			socket.disconnect();
		}
		
		if(callListener)
		{
			if(iOnDisconnectListener!=null)
			{
				iOnDisconnectListener.onDisconnect();
			}
		}
	}
	
	public ABPWebSocket setOnConnectListener(IOnConnectListener iOnConnectListener)
	{
		this.iOnConnectListener=iOnConnectListener;
		
		return this;
	}
	
	public ABPWebSocket setOnDisconnectListener(IOnDisconnectListener iOnDisconnectListener)
	{
		this.iOnDisconnectListener=iOnDisconnectListener;
		
		return this;
	}
	
	public ABPWebSocket setOnMessageListener(IOnMessageListener iOnMessageListener)
	{
		this.iOnMessageListener=iOnMessageListener;
		
		return this;
	}
	
	public ABPWebSocket setOnErrorListener(IOnErrorListener iOnErrorListener)
	{
		this.iOnErrorListener=iOnErrorListener;
		
		return this;
	}
	
	public boolean isConnected()
	{
		return connected;
	}
	
	public ABPWebSocket setUrl(String url)
	{
		this.url=url;
		
		return this;
	}
	
	public void sendMessage(String name,String message)
	{
		log("Request JSON: "+message);
		
		if(isConnected())
		{
			this.socket.emit(name,message);
		}
	}
	
	public void sendMessage(String message)
	{
		log("Request JSON: "+message);
		
		if(isConnected())
		{
			this.socket.emit("message",message);
		}
	}
	
	public int getTimeout()
	{
		return timeout;
	}
	
	public ABPWebSocket setTimeout(int timeout)
	{
		this.timeout=timeout;
		
		return this;
	}
	
	public boolean isDebug()
	{
		return debug;
	}
	
	public ABPWebSocket setDebug(boolean debug)
	{
		this.debug=debug;
		
		return this;
	}
	
	public void log(String input)
	{
		if(debug)
		{
			Log.i("ABPWebSocket",input);
		}
	}
}
