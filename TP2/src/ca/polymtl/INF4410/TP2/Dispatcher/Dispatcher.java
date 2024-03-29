package ca.polymtl.INF4410.TP2.Dispatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.Thread.State;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import ca.polymtl.INF4410.TP2.Dispatcher.JobThreadSecure;
import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Pair;

/**
 * Classe representant le repartiteur du systeme.
 * 
 * @author Bilal Itani & Mohameth Alassane Ndiaye
 *
 */
public class Dispatcher {

	private static List<Pair<String, Integer>> operations;
	private static List<Integer> allServerCapacity;
	private static List<Pair<String, IServer>> serverStubs = null;
	public static List<Pair<Semaphore, Semaphore>> sems = null;
	private static List<Integer> semaphoreAttempts = null;
	private static Set<Integer> indexToSkip = null;
	private static Integer unsecureServerCapacity = -1;

	/**
	 * Fonction principale du repartiteur.
	 * 
	 * @param args
	 *            arguments d'entre du programme.
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		try {
			Config conf = Config.getConfig();
			if (args.length > 0) {
				operations = readOperations(args[0]);
				setupDispatcher(conf);
				if (conf.getIsSecured()) {
					System.out.println("Final result : " + processCalculationSecured());
				} else {
					System.out.println("Final result : " + processCalculationUnSecured());
				}
			} else {
				throw new IllegalArgumentException("Lire le readme pour usage.");
			}

		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Methode permettant de charger le stub du serveur
	 * 
	 * @param hostname
	 *            L'adresse IP du serveur
	 * @param port
	 *            - Le port sur lequel le serveur de calcul roule
	 * @return Une paire qui contenant l'adresse IP du serveur et sont port ainsi
	 *         que le stub en lui meme.
	 */
	private static Pair<String, IServer> loadServerStub(String hostname, Integer port) {
		Pair<String, IServer> stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname, port);
			stub = new Pair<String, IServer>(hostname + ":" + port.toString(), (IServer) registry.lookup("server"));
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage() + "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			System.out.println("Erreur: " + e.getMessage());
		}
		return stub;
	}

	/**
	 * Methode permettant d'initialiser le repartiteur. Cette methode va populer les
	 * differents stub et initialiser des variables. Elle va aussi creer des
	 * semaphores qui seront utilise pour le mode securise.
	 * 
	 * @param conf
	 *            soit la configuration du systeme.
	 */
	private static void setupDispatcher(Config conf) {
		serverStubs = new ArrayList<Pair<String, IServer>>();
		sems = new ArrayList<Pair<Semaphore, Semaphore>>();
		semaphoreAttempts = new ArrayList<Integer>();
		indexToSkip = new HashSet<Integer>();
		allServerCapacity = new ArrayList<Integer>();
		for (int i = 0; i < conf.getServers().size(); i++) {
			serverStubs.add(loadServerStub(conf.getServers().get(i).getKey(), conf.getServers().get(i).getValue()));
			// Key : Semaphore from dispatcher to thread
			// Value : Semaphore from thread to dispatcher
			sems.add(new Pair<Semaphore, Semaphore>(new Semaphore(1), new Semaphore(0)));
		}
	}

	/**
	 * Methode permettant de lire les operations contenu dans le fichier donne en
	 * argument au programme du repartiteur.
	 * 
	 * @param filePath
	 *            emplacement du fichier
	 * @return Liste contenant une paire ou chaque operation est stocker.
	 * @throws IOException
	 *             Si jamais le fichier n'existe pas.
	 */
	private static List<Pair<String, Integer>> readOperations(String filePath) throws IOException {
		File file;
		FileReader fileReader = null;
		List<Pair<String, Integer>> listOfOperations;
		try {
			file = new File(filePath);
			fileReader = new FileReader(file);
			listOfOperations = new ArrayList<Pair<String, Integer>>();
			BufferedReader br = new BufferedReader(fileReader);
			String line;
			while ((line = br.readLine()) != null) {
				String[] vals = line.split("\\s+");
				listOfOperations.add(new Pair<String, Integer>(vals[0], Integer.parseInt(vals[1])));
			}
			br.close();
		} finally {
			if (fileReader != null) {
				fileReader.close();
			}
		}
		return listOfOperations;
	}

	/**
	 * Methode permettant de communiquer avec chacun des serveurs de calcul et de
	 * repartir la tache de calcul sur chacun des serveurs. Cette methode est
	 * utilise pour le mode securise uniquement.
	 * 
	 * @return Un entier qui represente le resultat des operations
	 * @throws RemoteException
	 * @throws InterruptedException
	 */
	private static Integer processCalculationSecured() throws RemoteException, InterruptedException {
		Integer operationsIndex = 0;
		Integer result = 0;
		List<Thread> threads = new ArrayList<Thread>();
		List<JobThreadSecure> jobs = new ArrayList<JobThreadSecure>();
		boolean firstIterationIsDone = false;
		Integer threadsAlive = 0;
		while (operationsIndex < operations.size() || threadsAlive > 0) // tant que l'index de ou on est rendu est
																		// inferieur a la taille de la liste des
																		// operations ou que le nombre de threads en vie
																		// est > 0.
		{
			// Condition afin de tuer les threads en vie.
			if (operationsIndex.equals(operations.size()) && threadsAlive > 0
					&& threads.stream().filter(i -> i != null).allMatch(i -> (i.getState().equals(State.WAITING)))
					&& jobs.stream().allMatch(j -> j.getResult().equals(0))) {
				for (int i = 0; i < threads.size(); i++) {
					if (threads.get(i) != null) {
						threads.get(i).interrupt();
						threadsAlive--;
					}
				}
			}
			// pour chaque serveur.
			for (int i = 0; i < serverStubs.size() && !firstIterationIsDone; i++) {
				allServerCapacity.add(serverStubs.get(i).getValue().getCapacity());
				// Increment optimal afin qu'il y ait uniquement 50% de chance de refus.
				Integer increment = getOptimalJobCapacity(allServerCapacity.get(i), operationsIndex);
				List<Pair<String, Integer>> operationsToDo = new ArrayList<Pair<String, Integer>>(
						operations.subList(operationsIndex, operationsIndex + increment));
				if (operationsToDo.size() > 0) {
					JobThreadSecure job = new JobThreadSecure(serverStubs.get(i), operationsToDo, i);
					jobs.add(job);
					Thread th = new Thread(job);
					th.setPriority(Thread.MAX_PRIORITY);
					threads.add(th);
					th.start();
					threadsAlive++;
					operationsIndex += increment; // increment operations index.
					semaphoreAttempts.add(0); // liste qui va contenir les differents essaies pour les semaphores. Utile
												// pour savoir si un serveur est mort.
				}
			}
			firstIterationIsDone = true; // on veut plus creer de threads car ils sont deja en marche.
			for (int i = 0; i < threads.size(); i++) {
				if (indexToSkip.contains(i)) {
					continue;
				} else {
					if (sems.get(i).getValue().tryAcquire(1, 10, TimeUnit.MILLISECONDS)) {
						semaphoreAttempts.set(i, 0); // Reinitialise le compateur d'essai de semaphore comme on la eu.
						if (jobs.get(i).getResult().equals(-1)) {
							operations.addAll(jobs.get(i).getOperations());// on popule operations avec la liste des
																			// operations que le thread devait faire,
																			// car il a pas fait le travail...
																			// ces operations seront a refaire.
							jobs.get(i).getOperations().clear();// je clear ce que je vien de rajouter au niveau de la
																// job (protection)
						} else // il y a un resultat.
						{
							System.out.println("Received result : " + jobs.get(i).getResult());
							result = (result + jobs.get(i).getResult()) % 4000;
							jobs.get(i).setResult(0);
						}
						// Envoi de nouvelles operations aux threads.
						Integer increment = getOptimalJobCapacity(allServerCapacity.get(i), operationsIndex);
						List<Pair<String, Integer>> operationsToDo = new ArrayList<Pair<String, Integer>>(
								operations.subList(operationsIndex, operationsIndex + increment));
						if (operationsToDo.size() > 0) {
							jobs.get(i).setOperations(operationsToDo);
							operationsIndex += increment;
							sems.get(jobs.get(i).getJobId()).getKey().release();
						}
					} else {
						// Ici, on s'assure si un serveur est mort. Donc apres 30 essaies (chaque essaie
						// avec 10ms d'attente) soit
						// 300 ms, si aucune reponse, on suppose que le serveur est mort.
						if (semaphoreAttempts.get(i) >= 30) {
							indexToSkip.add(i);
							jobs.get(i).setResult(0);
							operations.addAll(jobs.get(i).getOperations());
							jobs.get(i).getOperations().clear();
							threads.set(i, null);
							threadsAlive--;
						} else {
							// Sinon, on increment le nombre d'essaie du semaphore.
							semaphoreAttempts.set(i, (semaphoreAttempts.get(i) + 1));
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * Methode permettant de communiquer avec chacun des serveurs de calcul et de
	 * repartir la tache de calcul sur chacun des serveurs. Cette methode est
	 * utilise pour le mode non-securise uniquement.
	 * 
	 * @return Un entier qui represente le resultat des operations
	 * @throws RemoteException
	 */
	private static Integer processCalculationUnSecured() throws RemoteException {
		Integer operationsIndex = 0;
		Integer result = 0;
		while (operationsIndex < operations.size()) {
			// Ici, on suppose que les serveurs ont tous la meme capacite comme le mentionne
			// l'enonce de lab. Donc
			// Plutot que de redemander tout le temps la capacite des serveurs et sachant
			// que ceux-ci ne change pas,
			// On le demande une fois et c'est tout.
			if (unsecureServerCapacity == -1) {
				unsecureServerCapacity = serverStubs.get(0).getValue().getCapacity(); // since all server have the same
																						// capacity.
			}
			// Calcul des increment optimaux et envoi des operations a faire au threads.
			List<Thread> threads = new ArrayList<Thread>();
			List<JobThreadUnsecure> jobs = new ArrayList<JobThreadUnsecure>();
			Integer increment = getOptimalJobCapacity(unsecureServerCapacity, operationsIndex);
			List<Pair<String, Integer>> operationsToDo = new ArrayList<Pair<String, Integer>>(
					operations.subList(operationsIndex, operationsIndex + increment));
			operationsIndex += increment;
			for (int i = 0; i < serverStubs.size(); i++) {
				JobThreadUnsecure job = new JobThreadUnsecure(serverStubs.get(i), operationsToDo);
				jobs.add(job);
				Thread th = new Thread(job);
				th.setPriority(Thread.MAX_PRIORITY);
				threads.add(th);
				th.start();
			}
			// Attente de la fin de chaque thread.
			threads.stream().forEachOrdered(i -> {
				try {
					if (i != null) {
						i.join();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			// Verification si au moins deux threads ont le meme resultat
			Set<Integer> threadResults = new HashSet<Integer>();
			boolean match = false;
			int res = 0;
			for (int i = 0; i < jobs.size(); i++) {
				if (!jobs.get(i).getResult().equals(-1)) {
					if (threadResults.isEmpty()) {
						threadResults.add(jobs.get(i).getResult());
					} else {
						if (!threadResults.add(jobs.get(i).getResult())) {
							match = true;
							res = jobs.get(i).getResult();
							break;
						}
					}
				}
			}
			if (match) {
				System.out.println("Received result : " + res);
				result = (result + res) % 4000;
			} else {
				operations.addAll(jobs.get(0).getOperations());
			}
		}
		return result;
	}

	/**
	 * Methode permettant de retourner l'increment optimal pour l'envoi des
	 * operations vers le serveur.
	 * 
	 * @param serverCapacity
	 *            qui represente la capacite d'un serveur.
	 * @param operationsIndex
	 *            qui represente l'index sur lequel ont est rendu dans la liste des
	 *            operations.
	 * @return Entier representant l'increment optimal afin d'avoir un taux de rejet
	 *         inferieur a 50% du au manque de ressources.
	 */
	private static Integer getOptimalJobCapacity(Integer serverCapacity, Integer operationsIndex) {
		Integer optimalIncrement = (int) Math.round(((7.0 / 2.0) * serverCapacity.doubleValue()));
		// Increment minimum entre optimal et le nombre d'element avant la fin des
		// operations.
		Integer minBetweenOptimalIncrementAndItemsLeftCount = Math.min(operations.size() - operationsIndex,
				optimalIncrement);
		Integer increment = (minBetweenOptimalIncrementAndItemsLeftCount >= 0)
				? minBetweenOptimalIncrementAndItemsLeftCount
				: 0;
		return increment;
	}
}
