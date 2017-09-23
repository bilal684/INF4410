package ca.polymtl.inf4410.tp1.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import ca.polymtl.inf4410.tp1.shared.Pair;
import ca.polymtl.inf4410.tp1.shared.ServerFile;
import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.Utils;

public class Client {
	private String clientId;
	private static final String CLIENTCONFIGDIRECTORY = "ClientConfig";
	private static final String CLIENTFILESFOLDER = "ClientFilesFolder";
	private static final String SEPARATOR = "/";
	private static final String CLIENTIDFILE = "clientId.txt";
	
	public static void main(String[] args) throws RemoteException, IOException, NoSuchAlgorithmException {
		try {
			String serverIpaddress = null;

			if (args.length > 0) {
				if(args[0].split("\\.").length != 4)
				{
					throw new IllegalArgumentException();
				}
				serverIpaddress = args[0];
			}

			Client client = new Client(serverIpaddress);
			switch(args[1].toLowerCase())
			{
				case "create":
					client.create(args[2]);
					break;
				case "list":
					client.list();
					break;
				case "synclocaldir":
					client.syncLocalDir();
					break;
				case "get":
					client.get(args[2]);
					break;
				case "lock":
					client.lock(args[2]);
					break;
			}
		} catch(IllegalArgumentException e)
		{
			//print usage.
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
		}

	}

	private ServerInterface serverStub = null;

	public Client(String distantServerHostname) throws NumberFormatException, IOException {
		super();
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		serverStub = loadServerStub(distantServerHostname);
		if(!loadClientId())
		{
			clientId = serverStub.generateClientId();
			saveClientId();
		}
	}

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}

		return stub;
	}

	private void create(String nomFichier) throws RemoteException, NoSuchAlgorithmException, IOException	{
		String result = serverStub.create(nomFichier);
		System.out.println(result);
	}
	
	private void list() throws RemoteException
	{
		String result = serverStub.list();
		System.out.println(result);
	}
	
	private void syncLocalDir() throws IOException
	{
		List<ServerFile> files = new ArrayList<>();
		files = serverStub.syncLocalDir();
		StringBuilder synchronizedFiles = new StringBuilder();
		for(ServerFile file : files)
		{
			writeFileToDisk(file);
			if(file == files.get(files.size() - 1))
			{
				synchronizedFiles.append(file.getFileName());
			}
			else
			{
				synchronizedFiles.append(file.getFileName() + ", ");
			}
		}
		System.out.println(synchronizedFiles.toString() + " synchronise.");
	}
	
	private void get(String fileName) throws NoSuchAlgorithmException, IOException
	{
		String checksum = getFileChecksum(fileName);
		Pair<String,ServerFile> pair = (Pair<String, ServerFile>) serverStub.get(fileName, checksum);
		if(pair.getKey().equals(""))
		{
			writeFileToDisk(pair.getValue());
			System.out.println(fileName + " synchronise");
		}
		else
		{
			System.out.println(pair.getKey());
		}
	}
	
	private void lock(String fileName) throws NoSuchAlgorithmException, IOException
	{
		String checksum =  getFileChecksum(fileName);
		Pair<String, ServerFile> pair = (Pair<String, ServerFile>) serverStub.lock(fileName, clientId, checksum);
		if(pair.getValue() != null)
		{
			writeFileToDisk(pair.getValue());
			System.out.println(fileName + " synchronise");
		}
		System.out.println(pair.getKey());
	}
	
	private void writeFileToDisk(ServerFile file) throws IOException
	{
		Utils.createDirectory(CLIENTFILESFOLDER);
		FileOutputStream outputStream = new FileOutputStream(new File(CLIENTFILESFOLDER + SEPARATOR + file.getFileName()));
		outputStream.write(file.getContent());
		outputStream.close();
	}
	
	private String getFileChecksum(String fileName) throws NoSuchAlgorithmException, IOException
	{
		File localFile = new File(CLIENTFILESFOLDER + SEPARATOR + fileName);
		String checksum = null;
		if(localFile.exists())
		{
			checksum = Utils.getMD5Checksum(CLIENTFILESFOLDER + SEPARATOR + fileName);
		}
		else
		{
			checksum = "-1";
		}
		return checksum;
	}
	private void saveClientId() throws IOException
	{
		File latestIdFile = new File(CLIENTCONFIGDIRECTORY + SEPARATOR + CLIENTIDFILE);
		if(!latestIdFile.exists())
		{
			Utils.createDirectory(CLIENTCONFIGDIRECTORY);
			latestIdFile.createNewFile();
			FileWriter writer = new FileWriter(latestIdFile, false);
			try
			{
				writer.write(clientId);
			} finally
			{
				writer.close();
			}
		}
	}
	
	private boolean loadClientId() throws NumberFormatException, IOException
	{
		File file = new File(CLIENTCONFIGDIRECTORY + SEPARATOR + CLIENTIDFILE);
		if(file.exists())
		{
			FileReader fileReader = new FileReader(CLIENTCONFIGDIRECTORY + SEPARATOR + CLIENTIDFILE);
			BufferedReader textReader = new BufferedReader(fileReader);
			try
			{
				clientId = textReader.readLine();	
			} 
			finally
			{
				textReader.close();
			}
			return true;
		}
		return false;
	}

}
