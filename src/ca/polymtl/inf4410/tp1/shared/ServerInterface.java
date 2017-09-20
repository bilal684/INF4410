package ca.polymtl.inf4410.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.IOException;
public interface ServerInterface extends Remote {
	public String generateClientId() throws RemoteException;
	public String create(String nomFichier) throws RemoteException, IOException, NoSuchAlgorithmException;
	public String list() throws RemoteException;
	public List<ServerFile> syncLocalDir() throws RemoteException, FileNotFoundException, IOException;
}
