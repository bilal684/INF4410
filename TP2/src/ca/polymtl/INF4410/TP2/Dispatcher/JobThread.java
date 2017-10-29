package ca.polymtl.INF4410.TP2.Dispatcher;

import java.rmi.RemoteException;
import java.util.List;

import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Pair;

public class JobThread implements Runnable{
	
	private Integer result;
	private IServer serverStub;
	private Integer operationIndexStart;
	private Integer operationIndexEnd;
	private List<Pair<String, Integer>> operations;
	
	public JobThread(IServer serverStub, Integer operationIndexStart, Integer operationIndexEnd, List<Pair<String, Integer>> operations)
	{
		this.serverStub = serverStub;
		this.operationIndexStart = operationIndexStart;
		this.operationIndexEnd = operationIndexEnd;
		this.operations = operations;
		this.result = 0;
	}
	
	
	public void run() {
		// TODO Auto-generated method stub
		try {
			result = serverStub.processOperations(operations);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

}
