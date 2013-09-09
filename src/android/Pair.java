package org.apache.cordova.bluetooth;


/**
 * Small utility class for holding pairs of same class.
 * 
 * @param <T> The class which the Pair should be of.
 */
public class Pair<T> 
{
	public final T a;
	public final T b;
	
	public Pair(T a, T b)
	{
		this.a = a;
		this.b = b;
	}
}
