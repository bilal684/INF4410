package ca.polymtl.inf4410.tp1.server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import ca.polymtl.inf4410.tp1.shared.Utils;

public class ServerFileInfo
{
	private String serverPathToFile = null;
	private String name = null;
	private Boolean locked = false;
	private String owner = null;
	private String checksum = null;

	public ServerFileInfo(String pathToFile, String name) throws NoSuchAlgorithmException, IOException
	{
		serverPathToFile = pathToFile;
		this.name = name;
		checksum = Utils.updateMD5Checksum(serverPathToFile + name);
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public Boolean getLocked()
	{
		return this.locked;
	}
	
	public void setLocked(Boolean locked)
	{
		this.locked = locked;
	}
	
	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getChecksum() {
		return checksum;
	}
}
