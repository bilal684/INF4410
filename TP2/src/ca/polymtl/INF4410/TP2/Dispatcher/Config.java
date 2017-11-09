package ca.polymtl.INF4410.TP2.Dispatcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ca.polymtl.INF4410.TP2.Shared.Pair;

/**
 * Cette classe permet de lire le fichier "config" et de creer un objet qui la
 * represente.
 * 
 * @author Bilal Itani & Mohameth Alassane Ndiaye
 *
 */
public class Config {

	private static final String CONFIGFILE = "config";
	private List<Pair<String, Integer>> servers;
	private boolean isSecured;

	private static Config conf = null;

	/**
	 * Methode permettant de construire la config si jamais elle n'existe pas, sinon
	 * elle retourne la config (singleton).
	 * 
	 * @return Un objet config qui represente la configuration.
	 * @throws IOException
	 *             Si jamais le fichier n'est pas trouve.
	 */
	public static Config getConfig() throws IOException {
		if (conf != null) {
			return conf;
		} else {
			conf = new Config();
			return conf;
		}
	}

	/**
	 * Constructeur privee (singleton).
	 * 
	 * @throws IOException
	 *             Si jamais le fichier de config n'existe pas.
	 */
	private Config() throws IOException {
		servers = new ArrayList<Pair<String, Integer>>();
		parseConfig();
	}

	/**
	 * Methode permettant de lire le fichier de config.
	 * 
	 * @throws IOException
	 *             Si jamais le fichier de config n'existe pas.
	 */
	private void parseConfig() throws IOException {
		File configFile;
		FileReader fileReader = null;
		try {
			configFile = new File(CONFIGFILE);
			fileReader = new FileReader(configFile);
			BufferedReader br = new BufferedReader(fileReader);
			String line;
			while ((line = br.readLine()) != null) // Lecture jusqu'a fin de fichier.
			{
				if (line.contains("%")) // commentaire si % dans la ligne.
				{
					continue;
				} else if (line.contains("SecureMode")) {
					isSecured = Boolean.parseBoolean((line.split("\\:")[1]));
				} else if (line.contains("IP")) {
					String[] res = line.split("\\:");
					servers.add(new Pair<String, Integer>(res[1], Integer.parseInt(res[2])));
				}
			}
			br.close();
		} finally {
			if (fileReader != null) {
				fileReader.close();
			}
		}
	}

	/**
	 * Getter sur la variable servers.
	 * 
	 * @return Liste de tous les serveurs present dans la config.
	 */
	public List<Pair<String, Integer>> getServers() {
		return servers;
	}

	/**
	 * Getter sur la variable isSecured.
	 * 
	 * @return boolean qui indique si le mode actuel est securitaire ou non
	 *         securitaire.
	 */
	public boolean getIsSecured() {
		return isSecured;
	}
}
