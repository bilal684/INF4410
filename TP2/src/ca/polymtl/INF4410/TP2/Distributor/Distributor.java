package ca.polymtl.INF4410.TP2.Distributor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Distributor {

	private static List<String> operations;
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		operations = readOperations(args[0]);
		System.out.println(operations.size());
		System.out.println(operations.get(3));
	}
	
	
	
	private static List<String> readOperations(String filePath) throws IOException
	{
		File file;
		FileReader fileReader = null;
		List<String> listOfOperations;
		try
		{
			file = new File(filePath);
			fileReader = new FileReader(file);
			listOfOperations = new ArrayList<String>();
			BufferedReader br = new BufferedReader(fileReader);
			String line;
			while((line = br.readLine()) != null)
			{
				listOfOperations.add(line);
			}
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

}
