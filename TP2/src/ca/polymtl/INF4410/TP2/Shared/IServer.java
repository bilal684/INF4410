package ca.polymtl.INF4410.TP2.Shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IServer extends Remote {

	public Integer getCapacity() throws RemoteException;

	public Integer processOperations(List<Pair<String, Integer>> listOfOperations) throws RemoteException;

}
