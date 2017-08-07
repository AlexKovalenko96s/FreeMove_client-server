package ua.kas.main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {

		ServerSocket serverSocket = null;
		Socket socket = null;

		System.out.println("SERVER START!");

		serverSocket = new ServerSocket(9152);

		while (true) {
			try {
				socket = serverSocket.accept();
			} catch (IOException e) {
				System.out.println("I/O error: " + e);
			}
			new EchoThread(socket).start();
		}
	}
}