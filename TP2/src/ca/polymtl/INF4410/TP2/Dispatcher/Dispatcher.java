package ca.polymtl.INF4410.TP2.Dispatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Pair;

public class Dispatcher {

	private static List<Pair<String, Integer>> operations;
	private static IServer serverStub = null;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		String serverIpaddress = null;
		if (args.length > 0) {
			if(args[0].split("\\.").length != 4 && args.length < 3)
			{
				throw new IllegalArgumentException();
			}
			serverIpaddress = args[0];
		}
		serverStub = loadServerStub(serverIpaddress);
		operations = readOperations(args[1]);
		List<Pair<String, Integer>> ls = new ArrayList<Pair<String, Integer>>(operations.subList(0, serverStub.getCapacity()));
		Integer ret = serverStub.processOperations(ls);
		System.out.println(ret);
	}
	
	private static IServer loadServerStub(String hostname) {
		IServer stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (IServer) registry.lookup("server");
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
	
	private static List<Pair<String, Integer>> readOperations(String filePath) throws IOException
	{
		File file;
		FileReader fileReader = null;
		List<Pair<String, Integer>> listOfOperations;
		try
		{
			file = new File(filePath);
			fileReader = new FileReader(file);
			listOfOperations = new ArrayList<Pair<String, Integer>>();
			BufferedReader br = new BufferedReader(fileReader);
			String line;
			while((line = br.readLine()) != null)
			{
				String[] vals = line.split("\\s+");
				listOfOperations.add(new Pair<String, Integer>(vals[0], Integer.parseInt(vals[1])));
			}
		}
		finally
		{
			if(fileReader != null)
			{
				fileReader.close();
			}
		}
		return listOfOperations;
	}

}
