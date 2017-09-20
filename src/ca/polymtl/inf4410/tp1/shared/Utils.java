package ca.polymtl.inf4410.tp1.shared;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

public class Utils {

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

	public static String getMD5Checksum(String file) throws IOException, NoSuchAlgorithmException
	{		
		byte[] bytes = Files.readAllBytes(Paths.get(file));
		byte[] hash = MessageDigest.getInstance("MD5").digest(bytes);
		return DatatypeConverter.printHexBinary(hash);
	} 
	
}
