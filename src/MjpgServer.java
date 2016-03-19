import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Timestamp;
import java.util.Date;

import javax.imageio.ImageIO;



public class MjpgServer extends Thread {
	ServerSocket server;
	BufferedImage frame;
	boolean streaming = false;
	
	public MjpgServer() {
		try {
			server = new ServerSocket(8080);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void handleConnection(Socket socket) {
		System.out.println("New connection from: " + socket.getInetAddress().getHostAddress());
		try {
			BufferedOutputStream outBuffer = new BufferedOutputStream(socket.getOutputStream());
			outBuffer.write(("HTTP/1.0 200 OK\r\n" +
	                "Server: TMMVisionServer\r\n" +
	                "Connection: close\r\n" +
	                "Max-Age: 0\r\n" +
	                "Expires: 0\r\n" +
	                "Cache-Control: no-store, no-cache, must-revalidate, pre-check=0, post-check=0, max-age=0\r\n" +
	                "Pragma: no-cache\r\n" + 
	                "Content-Type: multipart/x-mixed-replace; " +
	                "boundary=" + "myboundary" + "\r\n" +
	                "\r\n" +
	                "--" + "myboundary" + "\r\n").getBytes());	
			while(true) {
				if(frame != null) {
					ByteArrayOutputStream buffer = new ByteArrayOutputStream();
					ImageIO.write(frame, "JPEG", buffer);
					outBuffer.write(("Content-type: image/jpeg\r\n" +
		                      "Content-Length: " + buffer.size() + "\r\n" +
		                      "X-Timestamp:" + new Timestamp(new Date().getTime()) + "\r\n" +
		                      "\r\n").getBytes());
					outBuffer.write(buffer.toByteArray());
					outBuffer.write(("\r\n--" + "myboundary" + "\r\n").getBytes());
					outBuffer.flush();
				}
			}
		} catch(Exception e) {
			System.out.println("Client Disconnected or Write Failure");
		}
	}
	
	@Override
	public void run() {
		try {
			while(true) {
				Socket socket = server.accept();
				Thread t = new Thread(new Runnable() {
					
					@Override
					public void run() {
						handleConnection(socket);
					}
				});
				t.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sendFrame(BufferedImage i) {
		frame = i;
		if(!streaming) {
			streaming = true;
			start();
		}
	}
	
}
