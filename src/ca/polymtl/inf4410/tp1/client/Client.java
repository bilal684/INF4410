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

import ca.polymtl.inf4410.tp1.shared.ServerFile;
import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.Utils;

public class Client {
	private int clientId;
	private static final String CLIENTCONFIGDIRECTORY = "ClientConfig";
	private static final String CLIENTFILESFOLDER = "ClientFilesFolder";
	private static final String SEPARATOR = "/";
	private static final String CLIENTIDFILE = "clientId.txt";
	
	public static void main(String[] args) throws RemoteException, IOException, NoSuchAlgorithmException {
		String distantHostname = null;
		int argumentIndex = 0;

		/*if (args.length > 0) {
			distantHostname = args[0];
		}*/
		
		//On cherche a ce connecter sur le serveur distant.
		if(args[0].split(".").length > 0)
		{
			argumentIndex = 1; //argumentIndex starts at 1.
		}

		Client client = new Client(distantHostname);
		switch(args[argumentIndex].toLowerCase())
		{
			case "create":
				client.create(args[argumentIndex + 1]);
				break;
			case "list":
				client.list();
				break;
			case "synclocaldir":
				client.syncLocalDir();
				break;
				
		}
	}

	/*FakeServer localServer = null; // Pour tester la latence d'un appel de
									// fonction normal.*/
	private ServerInterface localServerStub = null;
	//private ServerInterface distantServerStub = null;

	public Client(String distantServerHostname) throws NumberFormatException, IOException {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		localServerStub = loadServerStub("127.0.0.1");
		if(!loadClientId())
		{
			clientId = Integer.parseInt(localServerStub.generateClientId());
			saveClientId();
		}
		/*if (distantServerHostname != null) {
			distantServerStub = loadServerStub(distantServerHostname);
		}*/
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
		String result = localServerStub.create(nomFichier);
		System.out.println(result);
	}
	
	private void list() throws RemoteException
	{
		String result = localServerStub.list();
		System.out.println(result);
	}
	
	private void syncLocalDir() throws IOException
	{
		List<ServerFile> files = new ArrayList<>();
		files = localServerStub.syncLocalDir();
		for(ServerFile file : files)
		{
			Utils.createDirectory(CLIENTFILESFOLDER);
			FileOutputStream outputStream = new FileOutputStream(new File(CLIENTFILESFOLDER + SEPARATOR + file.getFileName()));
			outputStream.write(file.getContent());
			outputStream.close();
		}
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
				writer.write(Integer.toString(clientId));
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
				clientId = Integer.parseInt(textReader.readLine());	
			} 
			finally
			{
				textReader.close();
			}
			return true;
		}
		return false;
	}
	
	
	
	

	/*private void appelNormal() {
		long start = System.nanoTime();
		int result = localServer.execute(4, 7);
		long end = System.nanoTime();

		System.out.println("Temps écoulé appel normal: " + (end - start)
				+ " ns");
		System.out.println("Résultat appel normal: " + result);
	}

	private void appelRMILocal() {
		try {
			long start = System.nanoTime();
			int result = localServerStub.create();
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI local: " + (end - start)
					+ " ns");
			System.out.println("Résultat appel RMI local: " + result);
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}

	private void appelRMIDistant() {
		try {
			long start = System.nanoTime();
			int result = distantServerStub.execute(4, 7);
			long end = System.nanoTime();

			System.out.println("Temps écoulé appel RMI distant: "
					+ (end - start) + " ns");
			System.out.println("Résultat appel RMI distant: " + result);
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
	}*/
}
