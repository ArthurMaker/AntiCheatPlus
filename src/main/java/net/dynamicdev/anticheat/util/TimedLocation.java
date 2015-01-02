package net.dynamicdev.anticheat.util;

import org.bukkit.Location;

public class TimedLocation {
	
	private Location location;
	private long timestamp;
	
	public TimedLocation(Location location, long time)
	{
		this.location = location;
		timestamp = time;
	}
	
	public Location getLocation()
	{
		return location;
	}
	
	public long getTimestamp()
	{
		return timestamp;
	}
	
	public long getTimeDeltaFromNow()
	{
		return System.currentTimeMillis() - timestamp;
	}
	
	public double getDistanceXFrom(Location loc)
	{
		return Utilities.getXDelta(getLocation(), loc);
	}
	
	public double getDistanceZFrom(Location loc)
	{
		return Utilities.getZDelta(getLocation(), loc);
	}

}
