package ca.polymtl.INF4410.TP2.Dispatcher;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Semaphore;

import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Pair;

public class JobThread implements Runnable{
	private Integer jobId;
	private Integer result;
	private IServer serverStub;
	private Integer operationIndexStart;
	private Integer operationIndexEnd;
	private volatile List<Pair<String, Integer>> operations;
	
	public JobThread(IServer serverStub, Integer operationIndexStart, Integer operationIndexEnd, List<Pair<String, Integer>> operations, Integer jobId)
	{
		this.serverStub = serverStub;
		this.operationIndexStart = operationIndexStart;
		this.operationIndexEnd = operationIndexEnd;
		this.operations = operations;
		this.result = 0;
		this.jobId = jobId;
	}
	
	
	public synchronized void run() {
		// TODO Auto-generated method stub
		try {
			while(true)
			{
				Dispatcher.sems.get(jobId).getKey().acquire();
				//sems.getKey().acquire(); //acquires semaphore to start proceeding.
				result = serverStub.processOperations(operations);
				//sems.getValue().release(); // notifies the dispatcher that it has done.
				Dispatcher.sems.get(jobId).getValue().release();
			}
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			return;
		}
	}


	public Integer getResult() {
		return result;
	}


	public IServer getServerStub() {
		return serverStub;
	}


	public Integer getOperationIndexStart() {
		return operationIndexStart;
	}


	public Integer getOperationIndexEnd() {
		return operationIndexEnd;
	}


	public List<Pair<String, Integer>> getOperations() {
		return operations;
	}


	public void setOperations(List<Pair<String, Integer>> operations) {
		this.operations = operations;
	}


	/*public Pair<Semaphore, Semaphore> getSems() {
		return sems;
	}*/

}
