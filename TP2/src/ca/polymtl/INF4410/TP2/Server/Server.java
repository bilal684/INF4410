package ca.polymtl.INF4410.TP2.Server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Operations;
import ca.polymtl.INF4410.TP2.Shared.Pair;


public class Server implements IServer{

	/**
	 * @param args
	 */
	
	private Integer capacity;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Server server = new Server(Integer.parseInt(args[0]));
		server.run();
	}
	
	public Server(Integer capacity)
	{
		this.capacity = capacity;			
	}
	
	public void run()
	{
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			IServer stub = (IServer) UnicastRemoteObject.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
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

	public Integer getCapacity() throws RemoteException {
		// TODO Auto-generated method stub
		return capacity;
	}
	
	public Integer processOperations(List<Pair<String, Integer>> listOfOperations) throws RemoteException
	{
		Integer result = 0;
		for(int i = 0; i < listOfOperations.size(); i++)
		{
			if(listOfOperations.get(i).getKey().toLowerCase().equals("prime"))
			{
				result = processSum(Operations.prime(listOfOperations.get(i).getValue()), result);
			}
			else if(listOfOperations.get(i).getKey().toLowerCase().equals("pell"))
			{
				result = processSum(Operations.pell(listOfOperations.get(i).getValue()), result);
			}
		}
		return result;
	}
	
	private Integer processSum(Integer operationValue, Integer oldResult)
	{
		return (oldResult + operationValue) % 4000;
	}
	
	

}
