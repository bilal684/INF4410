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

/**
 * Cette classe represente le client. Elle contient l'implementation de toutes les methodes
 * que le client doit offrir.
 * */
public class Client {
	private String clientId;
	private static final String CLIENTCONFIGDIRECTORY = "ClientConfig";
	private static final String CLIENTFILESFOLDER = "ClientFilesFolder";
	private static final String SEPARATOR = "/";
	private static final String CLIENTIDFILE = "clientId.txt";
	
	/**
	 * Cette methode constitue le main du client.
	 * */
	public static void main(String[] args) throws RemoteException, IOException, NoSuchAlgorithmException {
		try {
			String serverIpaddress = null;

			if (args.length > 0) {
				if(args[0].split("\\.").length != 4 && args.length < 3)
				{
					throw new IllegalArgumentException();
				}
				serverIpaddress = args[0];
				
				Client client = new Client(serverIpaddress);
				if(args.length > 1)
				{
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
						case "push":
							client.push(args[2]);
							break;
						default:
							printUsage();
							break;
					}
				}
				else
				{
					printUsage();
				}
			}
			else
			{
				printUsage();
			}
		} catch(IllegalArgumentException e)
		{
			printUsage();
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
		}

	}

	private ServerInterface serverStub = null;

	/**
	 * Constructeur de la classe Client.
	 * */
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

	/**
	 * Permet l'appel de la methode create du serveur a partir du client.
	 * */
	private void create(String nomFichier) throws RemoteException, NoSuchAlgorithmException, IOException	{
		String result = serverStub.create(nomFichier);
		System.out.println(result);
	}
	
	/**
	 * Permet l'appel de la methode list du serveur a partir du client.
	 * */
	private void list() throws RemoteException
	{
		String result = serverStub.list();
		System.out.println(result);
	}
	
	/**
	 * Permet l'appel de la methode syncLocalDir du serveur a partir du client.
	 * */
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
		if(synchronizedFiles.length() > 0)
		{
			System.out.println(synchronizedFiles.toString() + " synchronise.");
		}
		else
		{
			System.out.println("Aucun fichier a synchronise.");
		}
	}
	
	/**
	 * Permet l'appel de la methode get du serveur a partir du client.
	 * */
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
	
	/**
	 * Permet l'appel de la methode lock du serveur a partir du client.
	 * */
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
	
	/**
	 * Permet l'appel de la methode push du serveur a partir du client.
	 * */
	private void push(String fileName) throws IOException, NoSuchAlgorithmException
	{
		File file = new File(CLIENTFILESFOLDER + SEPARATOR + fileName);
		if(file.exists())
		{
			ServerFile fileToSend = Utils.serializeFile(fileName, file);
			System.out.println(serverStub.push(fileToSend, clientId));
		}
		else
		{
			System.out.println("Le fichier " + fileName + " n'existe pas.");
		}
	}
	
	/**
	 * Permet l'ecriture d'un fichier sur le disque dur.
	 * */
	private void writeFileToDisk(ServerFile file) throws IOException
	{
		Utils.createDirectory(CLIENTFILESFOLDER);
		FileOutputStream outputStream = new FileOutputStream(new File(CLIENTFILESFOLDER + SEPARATOR + file.getFileName()));
		outputStream.write(file.getContent());
		outputStream.close();
	}
	
	/**
	 * Determine le checksum d'un fichier en utilisant une methode statique de la classe Utils.
	 * Dans le cas ou le fichier est inexistant, retourne -1
	 * */
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
	
	/**
	 * Permet la sauvegarde du ID unique du client dans un fichier texte.
	 * */
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
	
	/**
	 * Permet la lecture d'un fichier texte afin de trouver le ID du client, retourne true le cas echeant.
	 * Dans le cas ou le fichier est inexistant, la methode retourne false.
	 * */
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
	
	/**
	 * Methode permettant d'informer l'usager sur la facon dont on peu utiliser le client.
	 * */
	private static void printUsage()
	{
		System.out.println("*************************************************************************************************************");
		System.out.println("Usage : ");
		System.out.println("./client <ipadress> <operation>");
		System.out.println("Operations : ");
		System.out.println("create <fileName> : creates an empty file on the server.");
		System.out.println("list : lists all the files on the server as well as their lock status and owner.");
		System.out.println("synclocaldir : synchronizes all the files that are on the server inside the ClientFilesFolder directory.");
		System.out.println("get <fileName> : synchronizes a specific file from the server after a checksum verification.");
		System.out.println("lock <fileName> : locks a specific file on the server so no other client can modify it.");
		System.out.println("push <fileName> : pushes the file specified on the server if the client initially acquired the lock on it.");
		System.out.println("*************************************************************************************************************");
	}

}
