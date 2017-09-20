package ca.polymtl.inf4410.tp1.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ca.polymtl.inf4410.tp1.shared.ServerFile;
import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.Utils;

public class Server implements ServerInterface {


	private static final String FILESDIRECTORY = "FileVault";
	private static final String SEPARATOR = "/";
	private static final String SERVERCONFIGDIRECTORY = "ServerConfig";
	private static final String LATESTUNUSEDIDFILE = "latestUnusedId.txt";
	private Set<ServerFileInfo> filesInfo;
	private int latestId;

	public static void main(String[] args) throws NumberFormatException, IOException {
		Server server = new Server();
		server.run();
	}

	public Server() throws NumberFormatException, IOException {
		super();
		filesInfo = new HashSet<ServerFileInfo>();
		if(!loadLatestUnusedId())
		{
			latestId = 0;
		}
	}

	private void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			addProcessShutdownHook();
			populateFileSet();
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}
	
	public String generateClientId()
	{
		return Integer.toString(latestId++);
	}
	

	public String create(String nomFichier) throws RemoteException, IOException, NoSuchAlgorithmException {
		
		Utils.createDirectory(FILESDIRECTORY);
		File file = new File(FILESDIRECTORY + SEPARATOR + nomFichier);
		if(file.createNewFile())
		{
			ServerFileInfo fileInfo = new ServerFileInfo(FILESDIRECTORY + SEPARATOR, nomFichier);
			filesInfo.add(fileInfo);
			return nomFichier + " ajoute.";
		}
		return "Le fichier " + nomFichier + " existe deja.";
	}
	
	public String list()
	{
		StringBuilder allFiles = new StringBuilder();
		for(ServerFileInfo fileInfo : filesInfo)
		{
			allFiles.append("* " + fileInfo.getName() + ((fileInfo.getLocked()) ? " verrouille par client " + fileInfo.getOwner() : " non verrouille") + "\n");
		}
		allFiles.append(filesInfo.size() + " fichier(s)");
		return allFiles.toString();
	}
	
	public List<ServerFile> syncLocalDir() throws IOException
	{
		List<ServerFile> files = new ArrayList<>();
		for(ServerFileInfo fileInfo : filesInfo)
		{
			File physicalServerFile = new File(FILESDIRECTORY + SEPARATOR + fileInfo.getName());
			byte[] bytes = new byte[(int) physicalServerFile.length()];
			FileInputStream fileStream = new FileInputStream(physicalServerFile);
			fileStream.read(bytes);
			ServerFile file = new ServerFile(fileInfo.getName(), bytes);
			files.add(file);
			fileStream.close();
		}
		return files;
	}
	
	private void populateFileSet() throws NoSuchAlgorithmException, IOException
	{
		File folder = new File(FILESDIRECTORY);
		if(folder.exists())
		{
			File[] listOfAllFiles = folder.listFiles();
			for(int i = 0; i < listOfAllFiles.length; i++)
			{
				if(listOfAllFiles[i].isFile())
				{
					ServerFileInfo fileInfo = new ServerFileInfo(FILESDIRECTORY + SEPARATOR ,listOfAllFiles[i].getName());
					filesInfo.add(fileInfo);
				}
			}
		}
	}
	
	private void addProcessShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
		{

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					saveLatestGeneratedId();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}
		}));
	}
	
	private void saveLatestGeneratedId() throws IOException
	{
		File latestIdFile = new File(SERVERCONFIGDIRECTORY + SEPARATOR + LATESTUNUSEDIDFILE);
		if(!latestIdFile.exists())
		{
			Utils.createDirectory(SERVERCONFIGDIRECTORY);
			latestIdFile.createNewFile();
		}
		FileWriter writer = new FileWriter(latestIdFile, false);
		try
		{
			writer.write(Integer.toString(latestId));
		} finally
		{
			writer.close();
		}
	}
	
	private boolean loadLatestUnusedId() throws NumberFormatException, IOException
	{
		File file = new File(SERVERCONFIGDIRECTORY + SEPARATOR + LATESTUNUSEDIDFILE);
		if(file.exists())
		{
			FileReader fileReader = new FileReader(SERVERCONFIGDIRECTORY + SEPARATOR + LATESTUNUSEDIDFILE);
			BufferedReader textReader = new BufferedReader(fileReader);
			try
			{
				latestId = Integer.parseInt(textReader.readLine());	
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
