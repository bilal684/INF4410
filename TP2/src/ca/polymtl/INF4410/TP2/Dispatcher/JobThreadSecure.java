package ca.polymtl.INF4410.TP2.Dispatcher;

import java.util.List;

import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Pair;

/**
 * Classe representant le travail qui sera effectuer par un thread en mode securise.
 * @author Bilal Itani & Mohameth Alassane Ndiaye
 *
 */
public class JobThreadSecure implements Runnable {
	private Integer jobId;
	private Integer result;
	private Pair<String, IServer> serverStub;
	private volatile List<Pair<String, Integer>> operations;

	/**
	 * Constructeur par parametre de la classe.
	 * @param serverStub represente le serveur avec lequel le thread communiquera
	 * @param operations represente les operations qui seront envoye au serveur de calcul.
	 * @param jobId identifiant du thread.
	 */
	public JobThreadSecure(Pair<String, IServer> serverStub, List<Pair<String, Integer>> operations, Integer jobId) {
		this.serverStub = serverStub;
		this.operations = operations;
		this.result = 0;
		this.jobId = jobId;
	}

	/**
	 * Methode contenant le code qu'aura a execute le thread.
	 * */
	public synchronized void run() {
		// TODO Auto-generated method stub
		try {
			while (true) {
				Dispatcher.sems.get(jobId).getKey().acquire();
				result = serverStub.getValue().processOperations(operations);
				Dispatcher.sems.get(jobId).getValue().release();
			}
		}
		catch(Exception e)
		{
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
	 * Getter sur la variable serverStub
	 * @return serverStub une paire qui represent le stub du serveur.
	 */
	public Pair<String, IServer> getServerStub() {
		return serverStub;
	}

	/**
	 * Getter sur la variable operations
	 * @return operations qui represente la liste d'operations executees par le thread.
	 */
	public List<Pair<String, Integer>> getOperations() {
		return operations;
	}

	/**
	 * Setter sur la variable operations
	 * @param operations qui represente la liste d'operations a execute par le thread.
	 */
	public void setOperations(List<Pair<String, Integer>> operations) {
		this.operations = operations;
	}

	/**
	 * Setter sur la variable result
	 * @param result qui represente le resultat.
	 */
	public void setResult(Integer result) {
		this.result = result;
	}

	/**
	 * Getter sur la variable jobId
	 * @return JobId qui represente l'identifiant d'une job.
	 */
	public Integer getJobId() {
		return jobId;
	}

}
