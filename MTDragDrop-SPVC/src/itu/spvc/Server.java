package itu.spvc;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {
	ServerSocket serverSocket = null;
	Socket sock = null;

	// ObjectOutputStream out;
	DataInputStream inputStream;
	DataOutputStream out;

	DragDropScene scene;

	/**
	 * Create the server and start the thread listening
	 * 
	 * @param ddscene
	 *            the scene that will receive image and events
	 */
	public Server(DragDropScene ddscene) {
		super("socketListener");
		this.scene = ddscene;
		this.start();
	}

	public void run() {
		try {
			// we open the connection and
			// get ready to listen to incoming information
			serverSocket = new ServerSocket(50543);
			sock = serverSocket.accept();
			inputStream = new DataInputStream(sock.getInputStream());
			out = new DataOutputStream(sock.getOutputStream());
			System.out.println("Accepted connection : " + sock);

			sendMessage("Connection successful");
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			while (true) {
				// System.out.println("Waiting...");

				// listening for messages
				if (inputStream.available() > 0) {
					byte head = inputStream.readByte();
					System.out.println(head);
					// We check the message header
					// 1 means image coming
					// 2 means coordinates
					// anything else means stop communication
					if (head == 1) {
						// We get the size of the image,
						// to know how much we have to read from the socket
						int size = inputStream.readInt();
						System.out.println(size);
						byte[] imgBytes = new byte[size];
						int soFar = 0;

						// "read" doesn't make sure that all the "size" bytes
						// are read!
						// So while we did not get the full image we keep
						// reading.
						while (soFar < size) {
							int readNow = inputStream.read(imgBytes, soFar,
									(size - soFar));
							if (readNow > 0) {
								soFar += readNow;
							}
							System.out.println("read " + readNow
									+ ", missing: " + (size - soFar));
						}
						// System.out.println(inputStream.read(imgBytes, 0,
						// size));

						// Once we got the all the byte of the image we rebuild
						// it.
						Image awtImage = Toolkit.getDefaultToolkit()
								.createImage(imgBytes);
						// And add it to the scene
						scene.addImage(awtImage);
					} else if (head == 2) {
						// TODO:
						// We get x and y coordinates from the move commands
						// We move the latest image added to the server
						// scene.moveImage(x,y);
					} else
						break;
				}
			}
			System.out.println("Closing");

			// out.close();
			serverSocket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	void sendMessage(String msg) {
		try {
			out.writeBytes(msg);
			out.flush();
			System.out.println("server>" + msg);
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}
}
