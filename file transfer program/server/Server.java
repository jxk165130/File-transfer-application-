
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jin
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

public class Server extends Socket
{
	private static ServerSocket listen = null;
	private static Socket server = null;
	private static InetAddress address = null;
	private static Scanner sc = new Scanner(System.in);
	private static String message = null;
	private static String filename = null;
	private static String command = null;
	private static String[] command_array = null;
        private static InputStream input_stream;
	private static InputStreamReader input_stream_reader;
	private static OutputStream output_stream;
	private static OutputStreamWriter output_stream_writer;
	private static BufferedReader buffer_reader;
	private static BufferedWriter buffer_writer;
        private static int port;


        /*
         This is server function and using the user port number
         print out of the servers ip address. serversocket listen for connection
         by a client at the assigned port then socket object is returned when listener
         accept connection
         */
	public Server(int port) throws IOException
	{
            //print out the server ip address
		address = InetAddress.getLocalHost();
		System.out.println("IP Address: " + address);

		while (true) // wait for connect
		{
			listen = new ServerSocket(port);
			System.out.println("SERVER: Listening for clients on port "
					+ listen.getLocalPort());
			server = listen.accept();

			break;
		}
                // print that server, client connect to server
		System.out.println("SERVER: connected to " + server.getRemoteSocketAddress());
		input_stream = server.getInputStream();
		input_stream_reader = new InputStreamReader(input_stream);
		buffer_reader = new BufferedReader(input_stream_reader);
		output_stream = server.getOutputStream();
		output_stream_writer = new OutputStreamWriter(output_stream);
		buffer_writer = new BufferedWriter(output_stream_writer);
	}


        /*
         This function will communicate between server and client.
         First wait for the client to initiate communication after which each side takes
         turns writing replies.
         */
	public void communicate() throws IOException
	{
		while (true)
		{
			if ((message = buffer_reader.readLine()) != null)
			{
				if (message.startsWith("send"))
				{
					File file = new File((message.split(" ", 2))[1]);
					if (!file.exists())
					{
						System.out.println("Client: \"" + message + "\"");
						System.out.println("SERVER: Client file does not exist.");
						buffer_writer.write("File does not exist. Try again\n");
						buffer_writer.flush();
						continue;
					}
					else
					{
						command = message;
						buffer_writer.write("end\n");
						buffer_writer.flush();
						break;
					}
				}
				System.out.println("SERVER: Client said \"" + message + "\"");
				if (message.equals("end")) break;
			}
			message = sc.nextLine();
			if (message.startsWith("sendTCP"))
			{
				String filename = (message.split(" ", 2))[1];
				command = "receiveTCP " + filename;
			}

			buffer_writer.write(message + "\n");
			buffer_writer.flush();
			if (message.equals("end")) break;
		}
	}



        /* This is a main function, it request port number from user and creates a server object
         server will immediately goes into communicate. it will check and determine
         what we need to send  or receive.*/
	public static void main(String args[])
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException
	{
		System.out.println("SERVER: Enter Port #:");
		port = Integer.parseInt(sc.nextLine());
		Server testserver = new Server(port);

		testserver.communicate();

		if (command != null)
		{
			command_array = command.split(" ", 2);
			filename = command_array[1];

			if ((command_array[0]).equals("receiveTCP"))
			{
				System.out.println("SERVER: Going to receive file.");

				if ((command_array[0]).equals("receiveTCP"))
				{
					new helper();
					helper.receive(filename, server);
					testserver.close();
					return;
				}
			}
			if ((command_array[0]).equals("sendTCP"))
			{
				File file = new File(filename);
				if (!file.exists())
				{
					System.out.println("SERVER: " + filename + " does not exist.");
					buffer_writer.write(filename + " does not exist.");
					buffer_writer.flush();

					testserver.close();
					return;
				}
				System.out.println("SERVER: Got command to " + command);

				if ((command_array[0]).equals("sendTCP"))
				{
					new helper();
					helper.send(filename, server);
					testserver.close();
					return;
				}
			}
		}
		testserver.close();
	}
}