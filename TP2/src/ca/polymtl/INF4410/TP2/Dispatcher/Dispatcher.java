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
		for(int i = 0; i < conf.getServers().size(); i++)
		{
			serverStubs.add(i, loadServerStub(conf.getServers().get(i).getKey(), conf.getServers().get(i).getValue()));
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
		Integer operationsIndex = 0;
		Integer result = 0;
		boolean firstIteration = false;
		List<Pair<Semaphore, Thread>> threads = new ArrayList<Pair<Semaphore, Thread>>();
		List<JobThread> jobs = new ArrayList<JobThread>();
		Integer threadsAlive = 0;
		
		while(operationsIndex < operations.size() || threadsAlive > 0)
		{
			//System.out.println("en haut");
			//For loop that starts the different threads.
			for(int i = 0; i < serverStubs.size() && !firstIteration; i++)
			{
				//System.out.println(i);
				Integer optimalIncrement = (int) Math.round(((7.0/2.0) * serverStubs.get(i).getCapacity().doubleValue())); //optimal increment so it leaves a 50% chance of fail or success to optimize operation count.
				Integer increment = (Math.min(operations.size() - operationsIndex, operationsIndex + optimalIncrement) >= 0) ? Math.min(operations.size() - operationsIndex, operationsIndex + optimalIncrement) : 0;
				List<Pair<String, Integer>> ls = new ArrayList<Pair<String, Integer>>(operations.subList(operationsIndex, operationsIndex + increment));
				if(ls.size() > 0)
				{
					Semaphore sem = new Semaphore(1);
					JobThread job = new JobThread(serverStubs.get(i), operationsIndex, operationsIndex + increment, ls, sem);
					jobs.add(job);
					Thread th = new Thread(job);
					threads.add(new Pair<Semaphore, Thread>(sem, th));
					th.start();
					threadsAlive++;
				}
				else
				{
					break;
				}
				operationsIndex += increment;
			}
			firstIteration = true;
			for(int i = 0; i < threads.size(); i++)
			{
				if(threads.get(i).getValue().getState() == State.WAITING)
				{
					//System.out.println("Inside thread holding lock bra.");
					Integer optimalIncrement = (int) Math.round(((7.0/2.0) * jobs.get(i).getServerStub().getCapacity().doubleValue())); //optimal increment so it leaves a 50% chance of fail or success to optimize operation count.
					Integer increment = (Math.min(operations.size() - operationsIndex, operationsIndex + optimalIncrement) >= 0) ? Math.min(operations.size() - operationsIndex, operationsIndex + optimalIncrement) : 0;
					List<Pair<String, Integer>> ls = new ArrayList<Pair<String, Integer>>(operations.subList(operationsIndex, operationsIndex + increment));
					if(jobs.get(i).getResult() != -1)
					{
						result = (result + jobs.get(i).getResult()) % 4000;
						if(ls.size() > 0)
						{
							jobs.get(i).setOperations(ls);
							threads.get(i).getKey().release();
						}
						else
						{
							threads.get(i).getValue().interrupt(); // kill the thread.
							threadsAlive--;
						}
					}
					else
					{
						//redo operations...
						operations.addAll(jobs.get(i).getOperations());
					}
					operationsIndex += increment;
				}
				//System.out.println("outside thread holding lock bra.");
			}
			//System.out.println("Operations Index " + operationsIndex);
			//System.out.println("Operations size : " + operations.size());
		}
		return result;
	}
}
