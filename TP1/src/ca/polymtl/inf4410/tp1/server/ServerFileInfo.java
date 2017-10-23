package ca.polymtl.inf4410.tp1.server;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import ca.polymtl.inf4410.tp1.shared.Utils;

/**
 * Cette classe permet de garder en memoire les informations relatives a un fichier, excluant son contenue.
 * */
public class ServerFileInfo
{
	private String serverPathToFile = null;
	private String name = null;
	private Boolean locked = false;
	private String owner = null;
	private String checksum = null;
	/**
	 * Constructeur de la classe.
	 * */
	public ServerFileInfo(String pathToFile, String name) throws NoSuchAlgorithmException, IOException
	{
		serverPathToFile = pathToFile;
		this.name = name;
		checksum = Utils.getMD5Checksum(serverPathToFile + name);
	}
	
	/**
	 * Getter pour l'attribut name.
	 * */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * Getter pour l'attribut locked.
	 * */
	public Boolean getLocked()
	{
		return this.locked;
	}
	
	/**
	 * Setter pour l'attribut locked.
	 * */
	public void setLocked(Boolean locked)
	{
		this.locked = locked;
	}
	
	/**
	 * Getter pour l'attribut owner.
	 * */
	public String getOwner() {
		return owner;
	}

	/**
	 * Setter pour l'attribut owner.
	 * */
	public void setOwner(String owner) {
		this.owner = owner;
	}

	/**
	 * Getter pour l'attribut checksum.
	 * */
	public String getChecksum() {
		return checksum;
	}
	
	/**
	 * Methode permettant la mise a jour du checksum d'un fichier.
	 * */
	public void updateChecksum() throws NoSuchAlgorithmException, IOException
	{
		checksum = Utils.getMD5Checksum(serverPathToFile + name);
	}
}
