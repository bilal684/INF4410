package ca.polymtl.INF4410.TP2.Server;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Random;

import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Operations;
import ca.polymtl.INF4410.TP2.Shared.Pair;

public class Server implements IServer {
	/**
	 * @param args
	 */

	private Integer capacity;
	private Integer lieRate;
	private Integer port;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Server server = null;
		if (args.length == 2) {
			server = new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
			server.run();
		} else if (args.length == 3) {
			server = new Server(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]));
			server.run();
		} else {
			throw new IllegalArgumentException(
					"Server can either take one argument (capacity) or two arguments (capacity and fail rate).");
		}
	}

	public Server(Integer capacity, Integer port) {
		this(capacity, port, null);
	}

	public Server(Integer capacity, Integer port, Integer lieRate) {
		this.capacity = capacity;
		this.port = port;
		this.lieRate = lieRate;
	}

	public void run() {
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			IServer stub = (IServer) UnicastRemoteObject.exportObject(this, 0);
			Registry registry = LocateRegistry.getRegistry(port);
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
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

	public Integer processOperations(List<Pair<String, Integer>> listOfOperations) throws RemoteException {
		System.out.println("Received request with size: " + listOfOperations.size());
		Integer result = 0;
		if (hasEnoughRessources(listOfOperations.size())) {
			if (feelsLikeLying()) {
				result = randomAnswer();
			} else {
				for (int i = 0; i < listOfOperations.size(); i++) {
					if (listOfOperations.get(i).getKey().toLowerCase().equals("prime")) {
						result = processSum(Operations.prime(listOfOperations.get(i).getValue()), result);
					} else if (listOfOperations.get(i).getKey().toLowerCase().equals("pell")) {
						result = processSum(Operations.pell(listOfOperations.get(i).getValue()), result);
					}
				}
			}
		} else {
			result = -1;
		}
		System.out.println("Result is : " + result);
		return result;
	}

	private boolean hasEnoughRessources(Integer operationsSize) {
		Integer refusalRate = (int) Math.round(
				(((operationsSize.doubleValue() - capacity.doubleValue()) / (5.0 * capacity.doubleValue())) * 100.0));
		Random r = new Random();
		int randomVal = r.nextInt(101);
		// If randomVal is <= refusalRate --> return false (not enough ressources).
		// Else, return true.
		return (randomVal > refusalRate);
	}

	private Integer randomAnswer() {
		Random r = new Random();
		return r.nextInt(Integer.MAX_VALUE);
	}

	private boolean feelsLikeLying() {
		if (lieRate != null) {
			Random r = new Random();
			int randomVal = r.nextInt(101);
			return (randomVal <= lieRate);
		}
		return false;
	}

	private Integer processSum(Integer operationValue, Integer oldResult) {
		return (oldResult + operationValue) % 4000;
	}
}
