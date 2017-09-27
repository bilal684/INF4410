package ca.polymtl.inf4410.tp1.shared;

import java.io.Serializable;

/**
 * Cette classe represente un fichier physique. Elle contient le nom du fichier ainsi que son contenue. L'idee derriere
 * la separation de cette classe et de ServerFileInfo etait de sauvegarder de la memoire. On ne voulais
 * pas avoir les contenus des fichiers en memoire lors de l'execution du serveur.
 * */
public class ServerFile implements Serializable {
	
	private static final long serialVersionUID = 361417043140836160L;
	private String fileName;
	private byte[] content;
	
	/**
	 * Constructeur de la classe.
	 * */
	public ServerFile(String fileName, byte[] content)
	{
		this.fileName = fileName;		
		this.content = content;
	}

	/**
	 * Getter sur l'attribut fileName.
	 * */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Setter sur l'attribut fileName.
	 * */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * Getter sur l'attribut content.
	 * */
	public byte[] getContent() {
		return content;
	}

	/**
	 * Setter sur l'attribut content.
	 * */
	public void setContent(byte[] content) {
		this.content = content;
	}
}
