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

/**
 * Classe representant un server de clacul.
 * 
 * @author Bilal Itani & Mohameth Alassane Ndiaye
 *
 */
public class Server implements IServer {

	private Integer capacity;
	private Integer lieRate;
	private Integer port;

	/**
	 * Fonction principale du programme execute par les serveurs de calcul
	 * 
	 * @param args
	 *            Arguments passe au programme lors de son lancement.
	 */
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

	/**
	 * Constructeur par parametre de la classe.
	 * 
	 * @param capacity
	 *            Capacite de calcul du serveur.
	 * @param port
	 *            Port sur lequel le serveur tourne.
	 */
	public Server(Integer capacity, Integer port) {
		this(capacity, port, null);
	}

	/**
	 * Constructeur par parametre principal de la classe.
	 * 
	 * @param capacity
	 *            capacite de calcul du serveur
	 * @param port
	 *            Port sur lequel le serveur tourne.
	 * @param lieRate
	 *            Pourcentage representant la frequence de mensonge du serveur.
	 */
	public Server(Integer capacity, Integer port, Integer lieRate) {
		this.capacity = capacity;
		this.port = port;
		this.lieRate = lieRate;
	}

	/**
	 * Methode permettant de lier le RMIRegistry avec un objet de cette classe.
	 */
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

	/**
	 * Getter sur la variable capacity qui represente la capacite d'un serveur de
	 * calcul
	 */
	public Integer getCapacity() throws RemoteException {
		return capacity;
	}

	/**
	 * Methode permettant de calculer le resultat des operations qui ont ete envoye
	 * au serveur.
	 */
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

	/**
	 * Methode permettant de determiner si le serveur a suffisamment de ressources
	 * pour effectuer le calcul
	 * 
	 * @param operationsSize
	 *            taille de la liste des operations envoye au serveur de calcul
	 * @return True s'il y a suffisamment de ressource, false autrement.
	 */
	private boolean hasEnoughRessources(Integer operationsSize) {
		Integer refusalRate = (int) Math.round(
				(((operationsSize.doubleValue() - capacity.doubleValue()) / (5.0 * capacity.doubleValue())) * 100.0));
		Random r = new Random();
		int randomVal = r.nextInt(101);
		// If randomVal is <= refusalRate --> return false (not enough ressources).
		// Else, return true.
		return (randomVal > refusalRate);
	}

	/**
	 * Methode permettant de generer un entier aleatoire dans l'intervalle [0,
	 * Integer.MAX_VALUE[.
	 * 
	 * @return Un entier aleatoire dans l'intervalle [0, Integer.MAX_VALUE[
	 */
	private Integer randomAnswer() {
		Random r = new Random();
		return r.nextInt(Integer.MAX_VALUE);
	}

	/**
	 * Methode permettant de determiner si un serveur, en mode non securise doit
	 * mentir ou envoye une reponse valide aux operations qui lui ont ete fourni
	 * 
	 * @return True s'il doit mentir, false autrement.
	 */
	private boolean feelsLikeLying() {
		if (lieRate != null) {
			Random r = new Random();
			int randomVal = r.nextInt(101);
			return (randomVal <= lieRate);
		}
		return false;
	}

	/**
	 * Methode permettant d'effectuer la somme entre le resultat d'une operation et
	 * les resultats anterieurs d'une meme serie d'operations
	 * 
	 * @param operationValue
	 *            Resultat d'une operation
	 * @param oldResult
	 *            Somme des resultat des operations anterieur d'une meme serie
	 *            d'operations
	 * @return Resultat de la somme
	 */
	private Integer processSum(Integer operationValue, Integer oldResult) {
		return (oldResult + operationValue) % 4000;
	}
}
