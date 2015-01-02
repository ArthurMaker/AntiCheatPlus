package net.dynamicdev.anticheat.util;

import org.bukkit.Location;
import org.bukkit.World;

public class SimpleLocation {
	
	private int x, y, z;
	private World world;
	
	public SimpleLocation(Location l)
	{
		this(l.getBlockX(), l.getBlockY(), l.getBlockZ());
	}
	
	public SimpleLocation(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
	}

}
