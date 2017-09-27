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
import java.util.concurrent.atomic.AtomicInteger;

import ca.polymtl.inf4410.tp1.shared.Pair;
import ca.polymtl.inf4410.tp1.shared.ServerFile;
import ca.polymtl.inf4410.tp1.shared.ServerInterface;
import ca.polymtl.inf4410.tp1.shared.Utils;

/**
 * Cette classe represente le serveur. Elle contient l'implementation de toutes les methodes
 * que le serveur doit offrir.
 * */
public class Server implements ServerInterface {


	private static final String FILESDIRECTORY = "ServerFilesFolder";
	private static final String SEPARATOR = "/";
	private static final String SERVERCONFIGDIRECTORY = "ServerConfig";
	private static final String LATESTUNUSEDIDFILE = "latestUnusedId.txt";
	private Map<String,ServerFileInfo> fileMap;
	private AtomicInteger latestId;

	/**
	 * Cette fonction constitue le main du Server.
	 * */
	public static void main(String[] args) throws NumberFormatException, IOException {
		Server server = new Server();
		server.run();
	}
	
	/**
	 * Constructeur du server. Dans le cas ou il n'existe aucun fichier contenant le dernier ID auxquels s'est rendu le serveur,
	 * initialise le dernier ID donne a un client a 0.
	 * */
	public Server() throws NumberFormatException, IOException {
		super();
		fileMap = new HashMap<>();
		latestId = new AtomicInteger();
		if(!loadLatestUnusedId())
		{
			latestId.set(0);
		}
	}
	
	/**
	 * Methode permettant d'initialiser le serveur, notamment le RMIRegistry et la creation 
	 * en memoire de la map contenant les informations relatives aux fichiers.
	 * */
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
	
	/**
	 * Permet la generation d'un ID client unique chaque fois que cette methode est appelee.
	 * */
	public String generateClientId()
	{
		return Integer.toString(latestId.getAndIncrement());
	}
	
	/**
	 * Cree un fichier vide sur le serveur, dans un repertoire FILESDIRECTORY. Dans le cas ou un fichier
	 * avec le meme nom existe, l'operation echoue.
	 * */
	public String create(String nomFichier) throws RemoteException, IOException, NoSuchAlgorithmException {
		Utils.createDirectory(FILESDIRECTORY);
		File file = new File(FILESDIRECTORY + SEPARATOR + nomFichier);
		if(file.createNewFile())
		{
			ServerFileInfo fileInfo = new ServerFileInfo(FILESDIRECTORY + SEPARATOR, nomFichier);
			synchronized(fileMap)
			{
				fileMap.put(nomFichier, fileInfo);
			}
			return nomFichier + " ajoute.";
		}
		return "Le fichier " + nomFichier + " existe deja.";
	}
	
	/**
	 * Permet d'enumerer tous les fichiers present dans le serveur ainsi que le statut (verrouille ou non) du fichier.
	 * */
	public String list()
	{
		StringBuilder allFiles = new StringBuilder();
		synchronized(fileMap)
		{
			for(ServerFileInfo fileInfo : fileMap.values())
			{
				allFiles.append("* " + fileInfo.getName() + ((fileInfo.getLocked()) ? " verrouille par client " + fileInfo.getOwner() : " non verrouille") + "\n");
			}
			allFiles.append(fileMap.size() + " fichier(s)");
		}
		return allFiles.toString();
	}
	
	/**
	 * Permet de synchroniser tous les fichiers present au niveau du serveur et les transmets au client.
	 * */
	public List<ServerFile> syncLocalDir() throws IOException
	{
		List<ServerFile> files = new ArrayList<>();
		synchronized(fileMap)
		{
			for(ServerFileInfo fileInfo : fileMap.values())
			{
				File physicalServerFile = new File(FILESDIRECTORY + SEPARATOR + fileInfo.getName());
				ServerFile file = Utils.serializeFile(fileInfo.getName(), physicalServerFile);
				files.add(file);
			}
		}
		return files;
	}
	
	/**
	 * Methode permettant d'envoye un fichier precis au client. Si le checksum envoye par le client
	 * est le meme que celui calcule par le serveur, alors le fichier chez le client est identique a la version
	 * du serveur et aucun fichier n'est envoye. Dans le cas contraire, le serveur envoi la derniere version du
	 * fichier au client. Dans le cas ou le fichier est inexistant au niveau du serveur,
	 * cette methode retourne la valeur null.
	 * */
	public Map.Entry<String, ServerFile> get(String fileName, String checksum) throws IOException {
		Pair<String, ServerFile> pair = null;
		synchronized(fileMap)
		{
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
		}
		//si null --> Le fichier n'existe pas au niveau du serveur.
		return pair;
	}
	
	/**
	 * Permet de verrouille un fichier au niveau du serveur. Seul le client ayant verrouille le fichier
	 * pourra lui effectuer des changements. Au moment du verrouillage, le fichier du serveur est synchroniser
	 * chez le client suivant la logique de la methode 'get' decrite ci-haut.
	 * */
	public Map.Entry<String, ServerFile> lock(String fileName, String clientId, String checksum) throws IOException
	{
		Pair<String, ServerFile> pair = null;
		synchronized(fileMap)
		{
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
		}
		return pair;
	}
	
	/**
	 * Permet de mettre a jour un fichier au niveau du serveur. Le contenue a jour est envoye par le client.
	 * Apres la mise a jour du fichier, le verrou sur le fichier est retire et une reinitialisation du proprietaire
	 * est faite. Le checksum du fichier est aussi mis a jour.
	 * */
	public String push(ServerFile file, String clientId) throws IOException, NoSuchAlgorithmException {
		String message = null;
		synchronized(fileMap)
		{
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
		}
		return message;
	}
	
	/**
	 * Methode permettant d'ecrire un fichier sur le disque, dans le repertoire FILESDIRECTORY.
	 * */
	private void writeFileToServerDisk(ServerFile file) throws IOException
	{
		Utils.createDirectory(FILESDIRECTORY);
		FileOutputStream outputStream = new FileOutputStream(new File(FILESDIRECTORY + SEPARATOR + file.getFileName()));
		outputStream.write(file.getContent());
		outputStream.close();
	}
	
	/**
	 * Methode permettant d'initialiser la map en la populant avec les informations relatives aux fichiers
	 * present dans le repertoire FILESDIRECTORY.
	 * */
	private void populateFileMap() throws NoSuchAlgorithmException, IOException
	{
		File folder = new File(FILESDIRECTORY);
		synchronized(fileMap)
		{
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
	}
	
	/**
	 * Methode permettant de lancer un thread lorsque le serveur est arrete (CTRL+C). Ce thread permettra d'ouvrir
	 * un fichier et de sauvegarder le dernier ID qui n'a pas ete attribuer a un client. Cette information sera
	 * utile lors du relancement du serveur pour eviter des 'clash' de IDs.
	 * */
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
	
	/**
	 * Methode permettant la sauvegarde du dernier ID non attribue par le serveur dans un fichier sur le disque.
	 * */
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
			writer.write(latestId.toString());
		} finally
		{
			writer.close();
		}
	}
	
	/**
	 * Methode permettant de charge a partir d'un fichier sur le disque le dernier ID non attribue par le serveur.
	 * */
	private boolean loadLatestUnusedId() throws NumberFormatException, IOException
	{
		File file = new File(SERVERCONFIGDIRECTORY + SEPARATOR + LATESTUNUSEDIDFILE);
		if(file.exists())
		{
			FileReader fileReader = new FileReader(SERVERCONFIGDIRECTORY + SEPARATOR + LATESTUNUSEDIDFILE);
			BufferedReader textReader = new BufferedReader(fileReader);
			try
			{
				latestId.set(Integer.parseInt(textReader.readLine()));	
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
