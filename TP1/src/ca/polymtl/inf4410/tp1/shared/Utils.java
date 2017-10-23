package ca.polymtl.inf4410.tp1.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

/**
 * Cette classe contient des methodes statiques qui seront utile autant au niveau du serveur qu'au niveau du client.
 * */
public class Utils {

	/**
	 * Methode permettant de s'assurer de la creation d'un repertoire. Retourne true dans le cas ou
	 * le repertoire a ete creer, false s'il existe deja.
	 * */
	public static boolean createDirectory(String directory)
	{
		File dir = new File(directory);
		if(!dir.exists())
		{
			dir.mkdir();
			return true;
		}
		return false;
	}

	/**
	 * Methode permettant le calcul et le retour d'un hash de type MD5 sous forme de String.
	 * */
	public static String getMD5Checksum(String file) throws IOException, NoSuchAlgorithmException
	{		
		byte[] bytes = Files.readAllBytes(Paths.get(file));
		byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
		return DatatypeConverter.printHexBinary(hash);
	} 
	
	/**
	 * Methode permettant de transformer le contenue d'un fichier en objet ServerFile. Cette objet
	 * contient le contenu du fichier en octet.
	 * */
	public static ServerFile serializeFile(String fileName, File file) throws IOException
	{
		byte[] bytes = new byte[(int) file.length()];
		FileInputStream fileStream = new FileInputStream(file);
		fileStream.read(bytes);
		ServerFile fileToSend = new ServerFile(fileName, bytes);
		fileStream.close();
		return fileToSend;
	}
}
