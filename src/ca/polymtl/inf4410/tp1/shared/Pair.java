package ca.polymtl.inf4410.tp1.shared;

import java.io.Serializable;
import java.util.Map;

public class Pair<K, V> implements Serializable, Map.Entry<K, V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7859036939708925504L;
	private final K key;
	private V value;

	public Pair(K key, V value)
	{
		this.key = key;
		this.value = value;
	}
	
	public K getKey() {
		return key;
	}

	public V getValue() {
		return value;
	}

	public V setValue(V value) {
		V oldValue = this.value;
		this.value = value;
		return oldValue;
	}
}
