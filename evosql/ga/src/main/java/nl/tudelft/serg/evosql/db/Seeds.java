package nl.tudelft.serg.evosql.db;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

public class Seeds {

	private ArrayList<String> longs;
	private ArrayList<String> doubles;
	private ArrayList<String> strings;
	private ArrayList<String> tempSeeds;

	public Seeds() {
		this.longs = new ArrayList<String>();
		this.doubles = new ArrayList<String>();
		this.strings = new ArrayList<String>();
		this.tempSeeds = new ArrayList<String>();
	}
	
	public void addLong(long n) {
		longs.add(String.valueOf(n));
	}

	public void addDouble(double n) {
		doubles.add(String.valueOf(n));
	}

	public void addString(String n) {
		this.strings.add(n);
	}

	public List<String> getLongs() {
		List<String> result = new ArrayList<String>();
		result.addAll(longs);
		result.addAll(tempSeeds);
		return result;
	}

	public List<String> getDoubles() {
		List<String> result = new ArrayList<String>();
		result.addAll(doubles);
		result.addAll(tempSeeds);
		return result;
	}
	
	public List<String> getLongsAndDoubles() {
		List<String> result = new ArrayList<String>();
		result.addAll(longs);
		result.addAll(doubles);
		result.addAll(tempSeeds);
		return result;
	}

	public List<String> getStrings() {
		List<String> result = new ArrayList<String>();
		result.addAll(strings);
		result.addAll(tempSeeds);
		return result;
	}
	
	public void addToTemp(List<String> values) {
		tempSeeds.addAll(values);
	}
	
	public void unsetTemp() {
		tempSeeds.clear();
	}
	
	public boolean hasStrings() {
		return strings.size() > 0;
	}
	
	public boolean hasLongs() {
		return longs.size() > 0;
	}
	
	public boolean hasDoubles() {
		return doubles.size() > 0;
	}

	private static Seeds empty;
	static {
		empty = new Seeds();
	}
	public static Seeds emptySeed() {
		return empty;
	}
	
	//MOO part
	public void addLongList(List<String> n) {
		this.longs.addAll(n);
		this.longs = new ArrayList<>(new HashSet<>(this.longs));
	}

	public void addDoubleList(List<String> n) {
		this.doubles.addAll(n);
		this.doubles = new ArrayList<>(new HashSet<>(this.doubles));
	}

	public void addStringList(List<String> n) {
		this.strings.addAll(n);
		this.strings = new ArrayList<>(new HashSet<>(this.strings));
	}
	public void addTempList(List<String> n) {
		this.tempSeeds.addAll(n);
		this.tempSeeds = new ArrayList<>(new HashSet<>(this.tempSeeds));
	}

	public List<String> getLongList() {
		return this.longs;
	}

	public List<String> getDoubleList() {
		return this.doubles;
	}
	
	public List<String> getStringList() {
		return this.strings;
	}

	public List<String> getTempList() {
		return this.tempSeeds;
	}

}
