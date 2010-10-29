package dk.itu.spvc.dragdrop;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import android.util.Log;

public class Client {

	public static byte[] getBytesFromInputStream(InputStream is)
			throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		while (true) {
			int len = is.read(buffer);
			if (len < 0) {
				break;
			}
			bout.write(buffer, 0, len);
		}
		return bout.toByteArray();
	}

	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		if (length > Integer.MAX_VALUE) {
			System.out.println("File is to large");
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	Socket sock;
	DataOutputStream dos;
	DataInputStream dis;

	public Client(String serverIp, int port) throws IOException {
		this.sock = new Socket(serverIp, port);
		this.dos = new DataOutputStream(sock.getOutputStream());
		this.dis = new DataInputStream(sock.getInputStream());
	}
	
	/**
	 * Call this blocking method to read the next available command
	 * @return
	 * @throws IOException
	 */
	public byte nextCommand() throws IOException {
		return this.dis.readByte();
	}
	
	/**
	 * if nextCommand() returns 1, call this method
	 * to read the bytes of the sent image
	 * @return
	 * @throws IOException
	 */
	public byte[] readImage() throws IOException {
		//the len of the image
		int len = dis.readInt();
		byte[] out = new byte[len];
		
		int soFar = 0;
		
		while(len != soFar) {
			//got to read all the image bytes
			int read = dis.read(out, soFar, (len - soFar));
			if(read < 0) {
				Log.d("CLIENT","stream ended!!");
				if(len != soFar) {
					throw new IOException("Stream ended");
				}
			}
			soFar += read;
		}
		return out;
	}
	
	/**
	 * if nextCommand() returns 2, call this method
	 * to read the coordinates
	 * @return the normalized coordinates array: index 0 identifies the X axis, 1 identifies the Y axis
	 * @throws IOException
	 */
	public float[] readCoord() throws IOException {
		float[] out = new float[2];
		out[0] = dis.readFloat();
		out[1] = dis.readFloat();
		return out;
	}

	/**
	 * Send the x,y coordinates to the server
	 * @param x
	 * @param y
	 * @throws IOException
	 */
	public void sendCoordinates(float x, float y) throws IOException {
		//# implement me
		/*
		send the byte (NOT int) 2 first, as this will identify that we
		are sending a pair coordinate. Then write the float x first, followed
		by the float y.
		Finally, flush the output stream
		*/
		dos.writeByte(2);
		dos.writeFloat(x);
		dos.writeFloat(y);
		dos.flush();
	}

	/**
	 * Send the all bytes in the inputstream to the server. Must be an image
	 * @param is
	 * @throws IOException
	 */
	public void sendImage(InputStream is) throws IOException {
		//# implement me
		/*
		write the byte 1, then get all the bytes from the input stream
		passed as parameter. Write the length of the byte array, followed
		by the byte array itself. Finally flush the output stream
		*/
		dos.writeByte(1);
		byte[] bytes = null;
		dos.writeInt(is.read(bytes));
		dos.write(bytes);
		dos.flush();
	}

	/**
	 * Send the contents of the file to the server. Must be an image
	 * @param imgFile
	 * @throws IOException
	 */
	public void sendImage(File imgFile) throws IOException {
		if (imgFile.exists() && imgFile.canRead()) {
			byte[] bytes = getBytesFromFile(imgFile);
			dos.writeByte(1);
			dos.writeInt(bytes.length);
			dos.write(bytes);
			dos.flush();
		} else {
			throw new IOException("file " + imgFile.getName()
					+ " does not exists or is not readable");
		}
	}

	/**
	 * Close the connection to the server
	 * @throws IOException
	 */
	public void close() throws IOException {
		dos.writeByte(0);
		dos.flush();
		dos.close();
		sock.close();
	}
}
