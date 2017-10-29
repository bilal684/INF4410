package ca.polymtl.INF4410.TP2.Dispatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.polymtl.INF4410.TP2.Shared.Pair;

public class Config {
	
	private static final String CONFIGFILE = "config";
	private List<Pair<String, Integer>> servers;
	private boolean isSecured;
	
	private static Config conf = null;
	
	
	public static Config getConfig() throws IOException
	{
		if(conf != null)
		{
			return conf;
		}
		else
		{
			conf = new Config();
			return conf;
		}
	}
	
	private Config() throws IOException
	{
		servers = new ArrayList<Pair<String, Integer>>();
		parseConfig();
	}
	
	
	/*
	 * 	private static List<Pair<String, Integer>> readOperations(String filePath) throws IOException
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
	 * 
	 * 
	 */
	
	private void parseConfig() throws IOException
	{
		File configFile;
		FileReader fileReader = null;
		try
		{
			configFile = new File(CONFIGFILE);
			fileReader = new FileReader(configFile);
			BufferedReader br = new BufferedReader(fileReader);
			String line;
			while((line = br.readLine()) != null) 
			{
				if(line.contains("%"))
				{
					continue;
				}
				else if (line.contains("SecureMode"))
				{
					isSecured = Boolean.parseBoolean((line.split("\\:")[1]));
				}
				else if (line.contains("IP"))
				{
					String[] res = line.split("\\:");
					servers.add(new Pair<String, Integer>(res[1], Integer.parseInt(res[2])));
				}
			}
		}
		finally
		{
			if(fileReader != null)
			{
				fileReader.close();
			}
		}
	}

	public List<Pair<String, Integer>> getServers() {
		return servers;
	}

	public boolean getIsSecured() {
		return isSecured;
	}
}
