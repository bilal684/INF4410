package ca.polymtl.inf4410.tp1.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ca.polymtl.inf4410.tp1.shared.Pair;
import ca.polymtl.inf4410.tp1.shared.ServerFile;
import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.Utils;

public class Server implements ServerInterface {


	private static final String FILESDIRECTORY = "ServerFilesFolder";
	private static final String SEPARATOR = "/";
	private static final String SERVERCONFIGDIRECTORY = "ServerConfig";
	private static final String LATESTUNUSEDIDFILE = "latestUnusedId.txt";
	private Map<String,ServerFileInfo> fileMap;
	private int latestId;

	public static void main(String[] args) throws NumberFormatException, IOException {
		Server server = new Server();
		server.run();
	}

	public Server() throws NumberFormatException, IOException {
		super();
		fileMap = new HashMap<>();
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
			populateFileMap();
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
			fileMap.put(nomFichier, fileInfo);
			return nomFichier + " ajoute.";
		}
		return "Le fichier " + nomFichier + " existe deja.";
	}
	
	public String list()
	{
		StringBuilder allFiles = new StringBuilder();
		for(ServerFileInfo fileInfo : fileMap.values())
		{
			allFiles.append("* " + fileInfo.getName() + ((fileInfo.getLocked()) ? " verrouille par client " + fileInfo.getOwner() : " non verrouille") + "\n");
		}
		allFiles.append(fileMap.size() + " fichier(s)");
		return allFiles.toString();
	}
	
	public List<ServerFile> syncLocalDir() throws IOException
	{
		List<ServerFile> files = new ArrayList<>();
		for(ServerFileInfo fileInfo : fileMap.values())
		{
			File physicalServerFile = new File(FILESDIRECTORY + SEPARATOR + fileInfo.getName());
			ServerFile file = Utils.serializeFile(fileInfo.getName(), physicalServerFile);
			files.add(file);
		}
		return files;
	}
	
	public Map.Entry<String, ServerFile> get(String fileName, String checksum) throws IOException {
		Pair<String, ServerFile> pair = null;
		if(fileMap.containsKey(fileName))
		{
			if(checksum.equals("-1") || !checksum.equals(fileMap.get(fileName).getChecksum()))
			{
				File physicalServerFile = new File(FILESDIRECTORY + SEPARATOR + fileName);
				ServerFile fileToSend = Utils.serializeFile(fileName, physicalServerFile);
				pair = new Pair<String, ServerFile>("", fileToSend);
			}
			else
			{
				//Fichier deja a jour.
				pair = new Pair<String, ServerFile>("Le fichier " + fileName + " est deja a jour.", null);
			}
		}
		else
		{
			pair = new Pair<String, ServerFile>("Le fichier " + fileName + " n'existe pas au niveau du serveur.", null);
		}
		//si null --> Le fichier n'existe pas au niveau du serveur.
		return pair;
	}
	
	public Map.Entry<String, ServerFile> lock(String fileName, String clientId, String checksum) throws IOException
	{
		Pair<String, ServerFile> pair = null;
		if(fileMap.containsKey(fileName))
		{
			if(!fileMap.get(fileName).getLocked())
			{
				fileMap.get(fileName).setLocked(true);
				fileMap.get(fileName).setOwner(clientId);
				pair = new Pair<String, ServerFile>("Fichier " + fileName + " verrouille.", get(fileName, checksum).getValue());
			}
			else
			{
				pair = new Pair<String, ServerFile>(fileName + " est deja verrouille par client " + fileMap.get(fileName).getOwner(), null);
			}
		}
		else
		{
			pair = new Pair<String, ServerFile>("Le fichier " + fileName + " n'existe pas au niveau du serveur.", null);
		}
		return pair;
	}
	
	public String push(ServerFile file, String clientId) throws IOException, NoSuchAlgorithmException {
		String message = null;
		if(fileMap.get(file.getFileName()).getLocked() && fileMap.get(file.getFileName()).getOwner().equals(clientId))
		{
			writeFileToServerDisk(file);
			fileMap.get(file.getFileName()).setLocked(false);
			fileMap.get(file.getFileName()).setOwner(null);
			fileMap.get(file.getFileName()).updateChecksum();
			message = file.getFileName() + " a ete envoye au serveur.";
		}
		else
		{
			message = "Operation refusee : vous devez verrouiller le fichier avant d'executer cette operation.";
		}
		return message;
	}
	
	private void writeFileToServerDisk(ServerFile file) throws IOException
	{
		Utils.createDirectory(FILESDIRECTORY);
		FileOutputStream outputStream = new FileOutputStream(new File(FILESDIRECTORY + SEPARATOR + file.getFileName()));
		outputStream.write(file.getContent());
		outputStream.close();
	}
	
	private void populateFileMap() throws NoSuchAlgorithmException, IOException
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
					fileMap.put(fileInfo.getName(), fileInfo);
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
