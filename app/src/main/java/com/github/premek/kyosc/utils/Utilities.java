package com.github.premek.kyosc.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class Utilities {
	static final String LOG_TAG = Utilities.class.getSimpleName();

	public static String readAssetFileContents(Context context, String filePath) {
		InputStream is = null;
		try {
			is = context.getAssets().open(filePath);
			Reader reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[8192];
			int read;
			while((read = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, read);
			}
			return builder.toString();
		}
		catch(Exception exp) {
			// TODO: Logging and error handling
			exp.printStackTrace();			
			return null;
		}
		finally {
			if(is != null) {
				try {is.close(); }catch(Exception e) {}
			}
		}
	}
	
	
	public static String readFileContents(String filePath) {
		InputStream is = null;
		try {
			is = new FileInputStream(filePath);
			Reader reader = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
			StringBuilder builder = new StringBuilder();
			char[] buffer = new char[8192];
			int read;
			while((read = reader.read(buffer, 0, buffer.length)) > 0) {
				builder.append(buffer, 0, read);
			}
			return builder.toString();
		}
		catch(Exception exp) {
			// TODO: Logging and error handling
			exp.printStackTrace();			
			return null;
		}
		finally {
			if(is != null) {
				try {is.close(); }catch(Exception e) {}
			}
		}
	}

	public static boolean write(String contents, File file) {
		BufferedWriter writer = null;

		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.append(contents);
			return true;
		} catch (IOException e) {
			Log.e(LOG_TAG, "", e);
			return false;
		}
		finally {
			if(writer != null) { 
				try { writer.close(); }catch(Exception exp){}
			}
		}		
	}

    /***
     * Utility method to parse string for specific OSC types.
     * @param value
     * @return the value in int, float, string
     */
    public static Object simpleParse(String value) {
        try {
            return Integer.parseInt(value);
        }
        catch(NumberFormatException nfe) {}

        try {
            return Float.parseFloat(value);
        }
        catch(NumberFormatException nfe) {}

        return value;
    }
}
