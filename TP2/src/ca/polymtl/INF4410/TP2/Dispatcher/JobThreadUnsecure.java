package ca.polymtl.INF4410.TP2.Dispatcher;

import java.util.List;

import ca.polymtl.INF4410.TP2.Shared.IServer;
import ca.polymtl.INF4410.TP2.Shared.Pair;

public class JobThreadUnsecure implements Runnable {

	private Integer result = 0;
	private Pair<String, IServer> serverStub;
	private List<Pair<String, Integer>> operations;

	public JobThreadUnsecure(Pair<String, IServer> serverStub, List<Pair<String, Integer>> operations) {
		this.serverStub = serverStub;
		this.operations = operations;
	}

	public synchronized void run() {
		// TODO Auto-generated method stub
		try {
			result = serverStub.getValue().processOperations(operations);
		} catch (Exception e) {
			return;
		}
	}

	public Integer getResult() {
		return result;
	}

	public List<Pair<String, Integer>> getOperations() {
		return operations;
	}

}
