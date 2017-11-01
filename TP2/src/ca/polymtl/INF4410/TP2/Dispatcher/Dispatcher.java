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
	public static List<Pair<Semaphore, Semaphore>> sems = null;
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
		sems = new ArrayList<Pair<Semaphore, Semaphore>>();
		for(int i = 0; i < conf.getServers().size(); i++)
		{
			serverStubs.add(i, loadServerStub(conf.getServers().get(i).getKey(), conf.getServers().get(i).getValue()));
			//Key : Semaphore from dispatcher to thread
			//Value : Semaphore from thread to dispatcher
			sems.add(new Pair<Semaphore, Semaphore>(new Semaphore(1), new Semaphore(0)));
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
	
	private synchronized static Integer processCalculationSecured() throws InterruptedException, RemoteException
	{
		Integer operationsIndex = 0;
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
			for(int i = 0; i < serverStubs.size() && !firstIterationDone; i++)
			{
				//System.out.println(i);
				Integer optimalIncrement = (int) Math.round(((7.0/2.0) * serverStubs.get(i).getCapacity().doubleValue())); //optimal increment so it leaves a 50% chance of fail or success to optimize operation count.
				System.out.println("optimalIncrement : " + optimalIncrement);
				Integer increment = (Math.min(operations.size() - operationsIndex, /*operationsIndex +*/ optimalIncrement) >= 0) ? Math.min(operations.size() - operationsIndex, /*operationsIndex +*/ optimalIncrement) : 0;
				System.out.println("Increment : " + increment);
				System.out.println("Operations size - operationsIndex " + (operations.size() - operationsIndex));
				System.out.println("Operations index + optimalIncrement" + (operationsIndex + optimalIncrement));
				List<Pair<String, Integer>> ls = new ArrayList<Pair<String, Integer>>(operations.subList(operationsIndex, operationsIndex + increment));
				operationsIndex = operationsIndex + increment;
				if(ls.size() > 0)
				{
					/*Semaphore semNotifyThread = new Semaphore(1);
					Semaphore semNotifyDispatcher = new Semaphore(0);*/
					//Pair<Semaphore, Semaphore> sems = new Pair<Semaphore, Semaphore>(semNotifyThread, semNotifyDispatcher);
					JobThread job = new JobThread(serverStubs.get(i), operationsIndex, operationsIndex + increment, ls, i);
					jobs.add(job);
					Thread th = new Thread(job);
					th.setPriority(10);
					threads.add(th);
					System.out.println("Threads size : " + threads.size());
					th.start();
					threadsAlive++;
				}
				else
				{
					break;
				}
			}
			firstIterationDone = true;
			Thread.currentThread().setPriority(1);
			for(int i = 0; i < threads.size(); i++)
			{
				System.out.println("Inside second for, hanging." + threads.size());
				for(int j = 0; j < threads.size(); j++)
				{
					System.out.println(threads.get(j).getState());
				}
				//Pair of semaphores --> key is semaphore from dispatcher to thread (notify that it can proceed).
				//Pair of semaphores --> value is semaphore from thread to dispatcher (notify dispatcher that thread is done).
					if(threads.get(i).getState().equals(State.WAITING))
					{
						System.out.println("Inside if");
						//System.out.println("Inside thread holding lock bra.");
						Integer optimalIncrement = (int) Math.round(((7.0/2.0) * jobs.get(i).getServerStub().getCapacity().doubleValue())); //optimal increment so it leaves a 50% chance of fail or success to optimize operation count.
						System.out.println("Server capacity : " + jobs.get(i).getServerStub().getCapacity().doubleValue());
						System.out.println("Optimal increment : " + optimalIncrement);
						Integer increment = (Math.min(operations.size() - operationsIndex, /*operationsIndex +*/ optimalIncrement) >= 0) ? Math.min(operations.size() - operationsIndex, /*operationsIndex +*/ optimalIncrement) : 0;
						System.out.println("Operations.sdize - operationsIndex = " + (operations.size() - operationsIndex));
						System.out.println("Operations Index + optimalIncrement = " + (operationsIndex + optimalIncrement) );
						
						System.out.print("Increment : " + increment);
						List<Pair<String, Integer>> ls = new ArrayList<Pair<String, Integer>>(operations.subList(operationsIndex, operationsIndex + increment));
						System.out.println("Boundaries chosen : [" + operationsIndex + ", " + (operationsIndex + increment) + "]");
						operationsIndex = operationsIndex + increment;
						if(jobs.get(i).getResult() != -1)
						{
							result = (result + jobs.get(i).getResult()) % 4000;
						}
						else
						{
							//redo operations...
							operations.addAll(jobs.get(i).getOperations());
						}
						if(ls.size() > 0)
						{
							jobs.get(i).setOperations(ls);
							sems.get(i).getKey().release();
						}
						else
						{
							threads.get(i).interrupt(); // kill the thread.
							//threads.remove(i);
							//jobs.remove(i);
							threadsAlive--;
						}
					}
					/*else
					{
						//Thread.sleep(2000);
					}*/
				}
				//System.out.println("outside thread holding lock bra.");
			//System.out.println("Operations size : " + operations.size());
			//System.out.println("Threads alive : " + threadsAlive);
			//System.out.println("Operations Index " + operationsIndex);
			//System.out.println("Operations size : " + operations.size());
		}
		
		return result;
	}
}
