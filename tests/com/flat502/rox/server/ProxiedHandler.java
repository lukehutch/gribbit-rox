package com.flat502.rox.server;

import java.util.Date;

public class ProxiedHandler {
	public String toUpper(String s) {
		return s.toUpperCase();
	}
	
	public String noArgs() {
		return "NO ARGS";
	}
	
	public String plentyOfArgs(int anInt, double aDouble, String aString, Date aDate) {
		return "PLENTY OF ARGS";
	}
	
	public String customTypeArg(CustomType arg) {
		return "CUSTOM TYPE ARG";
	}
}
