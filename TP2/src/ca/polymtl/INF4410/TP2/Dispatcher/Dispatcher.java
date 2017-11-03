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
import java.util.List;
import java.util.concurrent.Semaphore;

import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Pair;

public class Dispatcher {

	private static List<Pair<String, Integer>> operations;
	private static List<IServer> serverStubs = null;
	public static List<Semaphore> sems = null;
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		try
		{
			Config conf = Config.getConfig();
			if (args.length > 0) 
			{
				operations = readOperations(args[0]);
			}
			else
			{
				//TODO printUsage function....
			}
			populateServerStubs(conf);
			if(conf.getIsSecured())
			{
				System.out.println(processCalculationSecured());
			}
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	private static IServer loadServerStub(String hostname, Integer port) {
		IServer stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname, port);
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
	
	private static void populateServerStubs(Config conf)
	{
		serverStubs = new ArrayList<IServer>();
		sems = new ArrayList<Semaphore>();
		for(int i = 0; i < conf.getServers().size(); i++)
		{
			serverStubs.add(i, loadServerStub(conf.getServers().get(i).getKey(), conf.getServers().get(i).getValue()));
			//Key : Semaphore from dispatcher to thread
			//Value : Semaphore from thread to dispatcher
			sems.add(new Semaphore(1));
		}
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
			br.close();
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
	
	private static Integer processCalculationSecured() throws InterruptedException, RemoteException
	{
		/*Integer operationsIndex = 0;
		Integer result = 0;
		boolean firstIterationDone = false;
		List<Thread> threads = new ArrayList<Thread>();
		List<JobThread> jobs = new ArrayList<JobThread>();
		Integer threadsAlive = 0;
		while(operationsIndex < operations.size() || threadsAlive > 0)
		{
			//System.out.println("en haut");
			//For loop that starts the different threads.
			//System.out.println("Server stubs amount : " + serverStubs.size());
			System.out.println("Operations Index : " + operationsIndex);
			System.out.println("Operations size : " + operations.size());
			System.out.println("Threads alive : " + threadsAlive);
			//loop qui cree les threads pour la premiere fois.
			for(int i = 0; i < serverStubs.size() && !firstIterationDone; i++)
			{
				//System.out.println(i);
				Integer optimalIncrement = (int) Math.round(((7.0/2.0) * serverStubs.get(i).getCapacity().doubleValue())); //optimal increment so it leaves a 50% chance of fail or success to optimize operation count.
				System.out.println("optimalIncrement : " + optimalIncrement);
				Integer increment = (Math.min(operations.size() - operationsIndex, optimalIncrement) >= 0) ? Math.min(operations.size() - operationsIndex, optimalIncrement) : 0;
				System.out.println("Increment : " + increment);
				System.out.println("Operations size - operationsIndex " + (operations.size() - operationsIndex));
				System.out.println("Operations index + optimalIncrement" + (operationsIndex + optimalIncrement));
				List<Pair<String, Integer>> operationsToDo = new ArrayList<Pair<String, Integer>>(operations.subList(operationsIndex, operationsIndex + increment));
				if(operationsToDo.size() > 0)
				{
					JobThread job = new JobThread(serverStubs.get(i), operationsIndex, operationsIndex + increment, operationsToDo, i);
					jobs.add(job);
					Thread th = new Thread(job);
					threads.add(th);
					System.out.println("Threads size : " + threads.size());
					th.start();
					threadsAlive++;
					operationsIndex = operationsIndex + increment;
				}
				else
				{
					break;
				}
			}
			firstIterationDone = true;
			for(int i = 0; i < threads.size(); i++)
			{
				System.out.println("Inside second for, hanging." + threads.size());
				for(int j = 0; j < threads.size(); j++)
				{
					System.out.println(threads.get(j).getState());
				}
				if(threads.get(i).getState().equals(State.WAITING))
				{
					//Thread a terminer de rouler et contient des resultats.
					if(jobs.get(i).getResult() != -1)
					{
						//on additionne les resultat dans le cas ou != -1
						result = (result + jobs.get(i).getResult()) % 4000;
					}
					//result was equal to 1 so these operations must be redone.
					else
					{
						//redo operations...
						operations.addAll(jobs.get(i).getOperations());
					}
					System.out.println("Inside if");
					//System.out.println("Inside thread holding lock bra.");
					Integer optimalIncrement = (int) Math.round(((7.0/2.0) * jobs.get(i).getServerStub().getCapacity().doubleValue())); //optimal increment so it leaves a 50% chance of fail or success to optimize operation count.
					System.out.println("Server capacity : " + jobs.get(i).getServerStub().getCapacity().doubleValue());
					System.out.println("Optimal increment : " + optimalIncrement);
					Integer increment = (Math.min(operations.size() - operationsIndex, optimalIncrement) >= 0) ? Math.min(operations.size() - operationsIndex, optimalIncrement) : 0;
					System.out.println("Operations.sdize - operationsIndex = " + (operations.size() - operationsIndex));
					System.out.println("Operations Index + optimalIncrement = " + (operationsIndex + optimalIncrement) );
					
					System.out.print("Increment : " + increment);
					List<Pair<String, Integer>> operationsToDo = new ArrayList<Pair<String, Integer>>(operations.subList(operationsIndex, operationsIndex + increment));
					System.out.println("Boundaries chosen : [" + operationsIndex + ", " + (operationsIndex + increment) + "]");
					if(operationsToDo.size() > 0) // il y a encore de la job...
					{
						jobs.get(i).setOperations(operationsToDo);
						operationsIndex = operationsIndex + increment;
					}
					//result was not equal to 1.
				}
			}
		}*/
		Integer operationsIndex = 0;
		Integer result = 0;
		List<Thread> threads = new ArrayList<Thread>();
		List<JobThread> jobs = new ArrayList<JobThread>();
		boolean firstIterationIsDone = false;
		while(operationsIndex < operations.size()) //tant que l'index de ou on est rendu est inferieur a la taille de la liste des operations.
		{
			
			//pour chaque serveur.
			for(int i = 0; i < serverStubs.size() && !firstIterationIsDone; i++)
			{
				//optimal increment so it leaves a 50% chance of fail or success to optimize operation count.
				Integer optimalIncrement = (int) Math.round(((7.0/2.0) * serverStubs.get(i).getCapacity().doubleValue())); 
				Integer minBetweenOptimalIncrementAndItemsLeftCount = Math.min(operations.size() - 1 - operationsIndex, optimalIncrement);
				Integer increment = (minBetweenOptimalIncrementAndItemsLeftCount >= 0) ? minBetweenOptimalIncrementAndItemsLeftCount : 0; // protection malgre la condition du while.
				List<Pair<String, Integer>> operationsToDo = new ArrayList<Pair<String, Integer>>(operations.subList(operationsIndex, operationsIndex + increment));
				System.out.println("OperationsToDoInsideFirstForLoop : " + operationsToDo.size());
				/*if(operationsToDo.size() > 0)//si ya des operations a faire...
				{*/
				JobThread job = new JobThread(serverStubs.get(i), operationsIndex, operationsIndex + increment, operationsToDo, i);
				jobs.add(job);
				Thread th = new Thread(job);
				threads.add(th);
				th.start();
				operationsIndex += increment; // increment operations index.
				//}
			}
			firstIterationIsDone = true; //on veut plus creer de threads car ils sont deja en marche.
			for(int i = 0; i < threads.size(); i++)
			{
				if(threads.get(i).getState().equals(State.WAITING))//si le thread est en train d'attendre (pend sur un semaphore)
				{
					if(jobs.get(i).getResult().equals(-1))
					{
						operations.addAll(jobs.get(i).getOperations());//on popule operations avec la liste des operations que le thread devait faire...
						//c'est operations seront a refaire.
						//jobs.get(i).getOperations().clear();//je clear ce que je vien de rajouter au niveau de la job. //protection.
					}
					else //il y a un resultat.
					{
						result = (result + jobs.get(i).getResult()) % 4000;
					}
					Integer optimalIncrement = (int) Math.round(((7.0/2.0) * serverStubs.get(i).getCapacity().doubleValue())); 
					Integer minBetweenOptimalIncrementAndItemsLeftCount = Math.min(operations.size() - 1 - operationsIndex, optimalIncrement);
					Integer increment = (minBetweenOptimalIncrementAndItemsLeftCount >= 0) ? minBetweenOptimalIncrementAndItemsLeftCount : 0; // protection malgre la condition du while.
					List<Pair<String, Integer>> operationsToDo = new ArrayList<Pair<String, Integer>>(operations.subList(operationsIndex, operationsIndex + increment));
					System.out.println("OperationsToDoInsideSecondForLoop : " + operationsToDo.size());
					jobs.get(i).setOperations(operationsToDo);
					operationsIndex += increment;
					sems.get(i).release();
				}
			}
		}
		for(int i = 0; i < threads.size(); i++)
		{
			threads.get(i).interrupt();
		}
		return result;
	}
}
