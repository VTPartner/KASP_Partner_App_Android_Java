package com.kapstranspvtltd.kaps_partner.models;

public class CountryCodeItem {
	private final String countryCode;
	private final String code;
	private final String name;
	private final int flagResource;

	public CountryCodeItem(String countryCode, String code, String name, int flagResource) {
		this.countryCode = countryCode;
		this.code = code;
		this.name = name;
		this.flagResource = flagResource;
	}

	public String getCountryCode() { return countryCode; }
	public String getCode() { return code; }
	public String getName() { return name; }
	public int getFlagResource() { return flagResource; }
}