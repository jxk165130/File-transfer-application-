

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author jin
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.io.Serializable;
import java.util.Comparator;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;



public class helper
{
	
	private static long starttime = 0;
        private static InputStream input_stream;
	private static InputStreamReader input_stream_reader;
        private static ObjectOutputStream object_output_stream;
	private static OutputStream output_stream;
	private static OutputStreamWriter output_stream_writer;
	private static BufferedReader buffer_reader;
	private static BufferedWriter buffer_writer;
        private static BufferedInputStream Buffer_Input_s;
        private static FileInputStream file_input_stream;
        private static final int packetsize = 1000;
	private static final long time_over = 500000000;

        /*This is send function  read file neme send file to the exist socket*/
	public static void send(String filename, Socket socket)
			throws IOException, NoSuchAlgorithmException
	{
		File file = new File(filename);
		output_stream = socket.getOutputStream();
		object_output_stream = new ObjectOutputStream(output_stream);
		input_stream = socket.getInputStream();
		input_stream_reader = new InputStreamReader(input_stream);
		buffer_reader = new BufferedReader(input_stream_reader);
		file_input_stream = new FileInputStream(file);
		Buffer_Input_s = new BufferedInputStream(file_input_stream);

		ArrayList<header> buffer = new ArrayList<header>();
		Random rand = new Random();
		new Gen_Checksum();

		String message = null;
		String checksum = Gen_Checksum.getChecksum(filename);
		int size = 0;
		long filelen = file.length();
		long sequence = (long) rand.nextInt(Integer.SIZE - 1) + 1;
		long endsequence = sequence + filelen;
		starttime = System.nanoTime();

		System.out.println("Sending " + filename);

		while (sequence != endsequence)
		{
			size = packetsize;
			if (endsequence - sequence >= size)
				sequence += size;
			else
			{
				size = (int) (endsequence - sequence);
				sequence = endsequence;
			}
			byte[] packet = new byte[size];
			Buffer_Input_s.read(packet, 0, size);
			header testheader = new header(packet, sequence, checksum);

			buffer.add(testheader);

			System.out.println("Sending packet #" + testheader.num);
			object_output_stream.writeObject(testheader);
			if ((message = buffer_reader.readLine()) != null)
			{
				System.out.println(message);
				if ((System.nanoTime() - starttime) > time_over)
				{
					timeout(message, testheader.num, buffer, object_output_stream, socket);
				}
			}
		}
		System.out.println("\"" + filename + "\" sent successfully to "
				+ socket.getRemoteSocketAddress());
		object_output_stream.writeObject(null);
		if ((message = buffer_reader.readLine()) != null)
			System.out.println("Recepient says: \"" + message + "\"");
		Buffer_Input_s.close();
		buffer.clear();
	}


	public static void receive(String filename, Socket socket)
			throws IOException, ClassNotFoundException, NoSuchAlgorithmException
	{
		System.out.println("Start Downloading");
		FileOutputStream fout = new FileOutputStream("download " + filename);
		BufferedOutputStream bout = new BufferedOutputStream(fout);
		InputStream is = socket.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(is);
		OutputStream os = socket.getOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(os);
		BufferedWriter bw = new BufferedWriter(osw);

		header testheader = null;
		ArrayList<header> buffer = new ArrayList<header>();
		String testchecksum = null;

		int drop = 0;
		long ack = 0;
		while ((testheader = (header) ois.readObject()) != null)
		{
			drop++;

			if (drop != 3)
			{
				System.out.println("Received packet #" + testheader.num);
				buffer.add(testheader);
				Collections.sort(buffer, header.compareheader);

				if (testheader.num == ack || ack == 0)
				{
					ack = buffer.get(buffer.size() - 1).num + packetsize;
				}
				System.out.println("Sending ACK #" + ack);
			}
			bw.write("ACK " + ack + "\n");
			bw.flush();

			testchecksum = testheader.checksum;
		}
		for (header tempheader : buffer)
			bout.write(tempheader.payload, 0, tempheader.payload.length);
		bout.close();
		buffer.clear();

		new Gen_Checksum();
		String checksum = Gen_Checksum.getChecksum("download " + filename);
                System.out.println("Receving Checksum");
		System.out.println("Testing Checksum");
		if (checksum.equals(testchecksum))
		{
			System.out.println("Client: Checksum is same");
			bw.write("Checksum is same");
			bw.flush();
		}
		else
		{
			System.out.println("Client: Checksum doesn't match.");
			bw.write("Server: Checksum doesn't match.");
			bw.flush();
		}
		socket.close();
		System.out.println("\"download " + filename + "\" saved successfully from "
						+ socket.getRemoteSocketAddress());
                System.out.println("Client: Closing program.");
		bw.write("Server: End Server.");
		bout.close();
	}


        /*
         This is timeout function  at every time out first reset the timer and
         reads the latest ack that received, if ack received is less than the sequence number
         of the last packet serch the buffer to find the packet
         */
	public static void timeout(String message, long seq,
			ArrayList<header> buffer, ObjectOutputStream oos, Socket socket)
			throws IOException
	{
		InputStream is = socket.getInputStream();
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader br = new BufferedReader(isr);

		System.out.println("\t\t\tTIMEOUT\n");
		starttime = System.nanoTime();

		String[] temp = message.split(" ", 2);
		long ack = Long.parseLong(temp[1]);
		if ((ack - seq) < 0)
		{
			for (header t : buffer)
			{
				if (t.num == ack)
				{
					System.out.println("Retransmitting packet #" + t.num);
					oos.writeObject(t);
					if ((message = br.readLine()) != null)
					{
						System.out.println("\t\t\"" + message + "\"");
					}
				}
			}
		}
	}
}

 class header implements Serializable
{
	private static final long serialVersionUID = 1L;

	byte[] payload = null;
	long num = 0;
	String checksum = null;

	public header(byte[] data, long sequence, String chksm)
	{
		payload = data;
		num = sequence;
		checksum = chksm;
	}

	/*
	 header object compares to other header object
	 */
	 static Comparator<header> compareheader = new Comparator<header>()
	{
		public int compare(header h1, header h2)
		{
			int num1 = (int) h1.num;
			int num2 = (int) h2.num;

			return (num1 - num2);
		}
	};
}

 //This is a checksum function
  class Gen_Checksum
{
	public static String getChecksum(String filename)
			throws NoSuchAlgorithmException, IOException
	{
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		MessageDigest md5Digest = MessageDigest.getInstance("MD5");
		byte[] byteArray = new byte[1024];
		int byteCount = 0;

		while ((byteCount = fis.read(byteArray)) != -1)
		{
			md5Digest.update(byteArray, 0, byteCount);
		}
		fis.close();
		byte[] bytes = md5Digest.digest();
		StringBuilder sb = new StringBuilder();

		for(int i=0; i<bytes.length; i++)
		{
			sb.append(Integer.toString((bytes[i]&0xff)+0x100, 16).substring(1));
		}

		return sb.toString();
	}
}