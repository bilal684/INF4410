package ca.polymtl.inf4410.tp1.shared;

import java.io.Serializable;
import java.util.Map;

/**
 * Cette classe generique represente une pair. Elle est serialisable et permettra
 * d'envoye des messages a partir du serveur vers le client ainsi que des resultats comme
 * des fichiers.
 * */
public class Pair<K, V> implements Serializable, Map.Entry<K, V> {

	private static final long serialVersionUID = -7859036939708925504L;
	private final K key;
	private V value;
	
	/**
	 * Constructeur de la classe.
	 */
	public Pair(K key, V value)
	{
		this.key = key;
		this.value = value;
	}
	
	/**
	 * Getter sur l'attribut Key. La clef ici contiendra les messages du serveur vers le client.
	 */
	public K getKey() {
		return key;
	}

	/**
	 * Getter sur l'attribut Value. Cette attribut contiendra les resultats du serveur, comme les fichiers a envoyer.
	 */
	public V getValue() {
		return value;
	}

	/**
	 * Setter sur l'attribut Value.
	 */
	public V setValue(V value) {
		V oldValue = this.value;
		this.value = value;
		return oldValue;
	}
}
