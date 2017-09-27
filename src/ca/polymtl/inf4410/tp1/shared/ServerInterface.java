package ca.polymtl.inf4410.tp1.shared;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * Interface contenant les methodes offerte par le serveur et qui seront utilisable par le client.
 * */
public interface ServerInterface extends Remote {
	public String generateClientId() throws RemoteException;
	public String create(String nomFichier) throws RemoteException, IOException, NoSuchAlgorithmException;
	public String list() throws RemoteException;
	public List<ServerFile> syncLocalDir() throws RemoteException, IOException;
	public Map.Entry<String, ServerFile> get(String fileName, String checksum) throws RemoteException, IOException;
	public Map.Entry<String, ServerFile> lock(String fileName, String clientId, String checksum) throws RemoteException, IOException;
	public String push(ServerFile file, String clientId) throws RemoteException, IOException, NoSuchAlgorithmException;
}
