
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
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;


public class Client extends Socket
{
	private static Socket client = null;
	private static InetAddress address = null;
	private static int port;
	private static String message = null;
	private static String filename = null;
	private static String serveraddress = null;
	private static String command = null;
	private static String[] command_array = null;
        private static Scanner sc = new Scanner(System.in);
        private static InputStream input_stream;
	private static InputStreamReader input_stream_reader;
	private static OutputStream output_stream;
	private static OutputStreamWriter output_stream_writer;
	private static BufferedReader buffer_reader;
	private static BufferedWriter buffer_writer;


        /*
         This is Client function and using the user port number (almost same as Server function but client part)
         print out of the servers ip address. serversocket listen for connection
         by a client at the assigned port then socket object is returned when listener
         accept connection
         */
	public Client(String serveraddress, int port) throws IOException
	{
            //get ipaddress
		address = InetAddress.getLocalHost();
                //print out ipaddress
		System.out.println("IP Address: " + address);
		System.out.println("CLIENT: connecting to " + serveraddress + " on port " + port);
		client = new Socket(serveraddress, port); // connecting to server
		System.out.println("CLIENT: connected to " + client.getRemoteSocketAddress());
		input_stream = client.getInputStream();
		input_stream_reader = new InputStreamReader(input_stream);
		buffer_reader = new BufferedReader(input_stream_reader);
		output_stream = client.getOutputStream();
		output_stream_writer = new OutputStreamWriter(output_stream);
		buffer_writer = new BufferedWriter(output_stream_writer);
	}

        /*
         This function will communicate between server and client.
         First wait for the client to initiate communication after which each side takes
         turns writing replies.
         */
	private void communicate() throws IOException
	{
		boolean DNE = false;

		while (true)
		{
			if (!DNE)
			{
				message = sc.nextLine();
				if (message.startsWith("sendTCP"))
				{
					String filename = (message.split(" ", 2))[1];
					command = "receiveTCP " + filename;
				}

				buffer_writer.write(message + "\n");
				buffer_writer.flush();
			}
			DNE = false;
			if (message.equals("end")) break;

			if ((message = buffer_reader.readLine()) != null)
			{
				if (message.startsWith("send"))
				{
					File file = new File((message.split(" ", 2))[1]);
					if (!file.exists())
					{
						System.out.println("CLIENT: Server said \"" + message + "\"");
						System.out.println("CLIENT: Telling Server file does not exist.");
						buffer_writer.write("File does not exist. Try again.\n");
						buffer_writer.flush();
						DNE = true;
					}
					else
					{
						command = message;
						buffer_writer.write("end\n");
						buffer_writer.flush();
						break;
					}
				}
				if (!DNE)
					System.out.println("CLIENT: Server said \"" + message + "\"");
				if (message.equals("end")) break;
			}
		}
	}


        /*
         This is mainfunction which request port and server address from client.
         After user input, check the command string to determine whether we need to
         send or receive
         */
	public static void main(String args[])
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException
	{

                // user to enter server address and port number
		System.out.println("CLIENT: Enter Server Address:");
		serveraddress = sc.nextLine();
		System.out.println("CLIENT: Enter Port #:");
		port = Integer.parseInt(sc.nextLine());

		Client testclient = new Client(serveraddress, port);
                //into communicate mode
		testclient.communicate();
                //determine messesage
		if (command != null)
		{
			command_array = command.split(" ", 2);
			filename = command_array[1];
                        //if
			if ((command_array[0]).equals("receiveTCP"))
					
			{
				System.out.println("CLIENT: Going to receive file.");

				if ((command_array[0]).equals("receiveTCP"))
				{
					new helper();
					helper.receive(filename, client);
					testclient.close();
					return;
				}
			}
			if ((command_array[0]).equals("sendTCP"))
			{
				File file = new File(filename);
				if (!file.exists())
				{
					System.out.println("CLIENT: " + filename + " does not exist.");
					buffer_writer.write(filename + " does not exist.");
					buffer_writer.flush();
					testclient.close();
					return;
				}
				System.out.println("CLIENT: Got command to " + command);


				if ((command_array[0]).equals("sendTCP"))
				{
					new helper();
					helper.send(filename, client);
					testclient.close();
					return;
				}
			}
			testclient.close();
		}
	}
}