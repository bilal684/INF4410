package ca.polymtl.INF4410.TP2.Dispatcher;

import java.util.List;

import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Pair;

/**
 * Classe representant le travail qui sera effectuer par un thread en mode non-securise.
 * @author Bilal Itani & Mohameth Alassane Ndiaye
 *
 */
public class JobThreadUnsecure implements Runnable {

	private Integer result;
	private Pair<String, IServer> serverStub;
	private List<Pair<String, Integer>> operations;

	/**
	 * Constructeur par parametre de la classe.
	 * @param serverStub represente le serveur avec lequel le thread communiquera
	 * @param operations represente les operations qui seront envoye au serveur de calcul.
	 */
	public JobThreadUnsecure(Pair<String, IServer> serverStub, List<Pair<String, Integer>> operations) {
		this.serverStub = serverStub;
		this.operations = operations;
		this.result = 0;
	}

	/**
	 * Methode contenant le code qu'aura a execute le thread.
	 * */
	public synchronized void run() {
		// TODO Auto-generated method stub
		try {
			result = serverStub.getValue().processOperations(operations);
		} catch (Exception e) {
			result = 0;
			return;
		}
	}

	/**
	 * Getter sur la variable result
	 * @return result representant le resultat de l'operation
	 */
	public Integer getResult() {
		return result;
	}

	/**
	 * Getter sur la variable operations
	 * @return operations qui represente la liste d'operations executees par le thread.
	 */
	public List<Pair<String, Integer>> getOperations() {
		return operations;
	}

}
