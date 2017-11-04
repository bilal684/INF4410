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

import ca.polymtl.INF4410.TP2.Dispatcher.JobThreadSecure;
import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Pair;

public class Dispatcher {

	private static List<Pair<String, Integer>> operations;
	public static List<Pair<String, IServer>> serverStubs = null;
	public static List<Pair<Semaphore, Semaphore>> sems = null;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		try {
			Config conf = Config.getConfig();
			if (args.length > 0) {
				operations = readOperations(args[0]);
			} else {
				// TODO printUsage function....
			}
			setupDispatcher(conf);
			if (conf.getIsSecured()) {
				System.out.println("Final result : " + processCalculationSecured());
			} else {
				System.out.println("Final result : " + processCalculationUnSecured());
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	private static Pair<String, IServer> loadServerStub(String hostname, Integer port) {
		// IServer stub = null;
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

	private static void setupDispatcher(Config conf) {
		serverStubs = new ArrayList<Pair<String, IServer>>();
		sems = new ArrayList<Pair<Semaphore, Semaphore>>();
		for (int i = 0; i < conf.getServers().size(); i++) {

			serverStubs.add(loadServerStub(conf.getServers().get(i).getKey(), conf.getServers().get(i).getValue()));
			// Key : Semaphore from dispatcher to thread
			// Value : Semaphore from thread to dispatcher
			sems.add(new Pair<Semaphore, Semaphore>(new Semaphore(1), new Semaphore(0)));
		}
	}

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

	private static Integer processCalculationSecured() throws RemoteException {
		Integer operationsIndex = 0;
		Integer result = 0;
		List<Thread> threads = new ArrayList<Thread>();
		List<JobThreadSecure> jobs = new ArrayList<JobThreadSecure>();
		boolean firstIterationIsDone = false;
		Integer threadsAlive = 0;
		while (operationsIndex < operations.size() || threadsAlive > 0) // tant que l'index de ou on est rendu est
																		// inferieur a la taille de la liste des
																		// operations.
		{
			// A reverifier. Le bug est ici.
			if (operationsIndex.equals(operations.size()) && threadsAlive > 0 && threads.stream().allMatch(
					i -> i.getState().equals(State.WAITING) && jobs.stream().allMatch(j -> j.getResult().equals(0)))) {
				for (int i = 0; i < threads.size(); i++) {
					threads.get(i).interrupt();
					threadsAlive--;
				}
			}
			// pour chaque serveur.
			for (int i = 0; i < serverStubs.size() && !firstIterationIsDone; i++) {
				// optimal increment so it leaves a 50% chance of fail or success to optimize
				// operation count.
				Integer optimalIncrement = (int) Math
						.round(((7.0 / 2.0) * serverStubs.get(i).getValue().getCapacity().doubleValue()));
				Integer minBetweenOptimalIncrementAndItemsLeftCount = Math.min(operations.size() - operationsIndex,
						optimalIncrement);
				Integer increment = (minBetweenOptimalIncrementAndItemsLeftCount >= 0)
						? minBetweenOptimalIncrementAndItemsLeftCount
						: 0; // protection malgre la condition du while.
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
				}
			}
			firstIterationIsDone = true; // on veut plus creer de threads car ils sont deja en marche.
			for (int i = 0; i < threads.size(); i++) {
				if (sems.get(i).getValue().tryAcquire()) {
					if (jobs.get(i).getResult().equals(-1)) {
						operations.addAll(jobs.get(i).getOperations());// on popule operations avec la liste des
																		// operations que le thread devait faire...
						// c'est operations seront a refaire.
						jobs.get(i).getOperations().clear();// je clear ce que je vien de rajouter au niveau de la job.
															// //protection.
					} else // il y a un resultat.
					{
						System.out.println("Received result : " + jobs.get(i).getResult());
						result = (result + jobs.get(i).getResult()) % 4000;
						jobs.get(i).setResult(0);
					}
					Integer optimalIncrement = (int) Math
							.round(((7.0 / 2.0) * serverStubs.get(i).getValue().getCapacity().doubleValue()));
					Integer minBetweenOptimalIncrementAndItemsLeftCount = Math.min(operations.size() - operationsIndex,
							optimalIncrement);
					Integer increment = (minBetweenOptimalIncrementAndItemsLeftCount >= 0)
							? minBetweenOptimalIncrementAndItemsLeftCount
							: 0; // protection malgre la condition du while.
					List<Pair<String, Integer>> operationsToDo = new ArrayList<Pair<String, Integer>>(
							operations.subList(operationsIndex, operationsIndex + increment));
					if (operationsToDo.size() > 0) {
						jobs.get(i).setOperations(operationsToDo);
						operationsIndex += increment;
						sems.get(jobs.get(i).getJobId()).getKey().release();
					}
				}
			}
		}
		return result;
	}

	private static Integer processCalculationUnSecured() throws RemoteException {
		Integer operationsIndex = 0;
		Integer result = 0;
		while (operationsIndex < operations.size())
		{
			List<Thread> threads = new ArrayList<Thread>();
			List<JobThreadUnsecure> jobs = new ArrayList<JobThreadUnsecure>();
			Integer optimalIncrement = (int) Math
					.round(((7.0 / 2.0) * serverStubs.get(0).getValue().getCapacity().doubleValue()));
			Integer minBetweenOptimalIncrementAndItemsLeftCount = Math.min(operations.size() - operationsIndex,
					optimalIncrement);
			Integer increment = (minBetweenOptimalIncrementAndItemsLeftCount >= 0)
					? minBetweenOptimalIncrementAndItemsLeftCount
					: 0; // protection malgre la condition du while.
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
			threads.stream().forEachOrdered(i -> {
				try {
					i.join();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			Set<Integer> threadResults = new HashSet<Integer>();
			boolean match = false;
			int res = 0;
			for(int i = 0; i < jobs.size(); i++)
			{
				if(!jobs.get(i).getResult().equals(-1))
				{
					if(threadResults.isEmpty())
					{
						threadResults.add(jobs.get(i).getResult());
					}
					else
					{
						if(!threadResults.add(jobs.get(i).getResult()))
						{
							match = true;
							res = jobs.get(i).getResult();
							break;
						}
					}
				}
			}
			if(match)
			{
				result = (result + res) % 4000;
			}
			else
			{
				operations.addAll(jobs.get(0).getOperations());
			}
		}
		return result;
	}
}
