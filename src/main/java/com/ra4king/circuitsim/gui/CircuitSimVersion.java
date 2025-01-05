package com.ra4king.circuitsim.gui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;

public final class CircuitSimVersion implements Comparable<CircuitSimVersion> {
	private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)\\.(\\d+)(b?)");
	
	public static final CircuitSimVersion VERSION = new CircuitSimVersion("1.10.0b 2110 version");
	
	private final String version;
	private final int major;
	private final int minor;
	private final int bugfix;
	private final boolean beta;
	
	public CircuitSimVersion(String version) {
		this.version = version;
		
		Matcher matcher = VERSION_PATTERN.matcher(version);
		
		if (!matcher.find()) {
			throw new IllegalArgumentException("Invalid version string: " + version);
		}
		
		major = Integer.parseInt(matcher.group(1));
		minor = Integer.parseInt(matcher.group(2));
		bugfix = Integer.parseInt(matcher.group(3));
		beta = !matcher.group(4).isEmpty();
	}
	
	public String getVersion() {
		return version;
	}
	
	@Override
	public int compareTo(CircuitSimVersion o) {
		if (this.major > o.major) {
			return 1;
		} else if (this.major == o.major) {
			if (this.minor > o.minor) {
				return 1;
			} else if (this.minor == o.minor) {
				if (this.bugfix > o.bugfix) {
					return 1;
				} else if (this.bugfix == o.bugfix) {
					if (this.beta == o.beta) {
						return 0;
					} else if (o.beta) {
						return 1;
					}
				}
			}
		}
		
		return -1;
	}
}
