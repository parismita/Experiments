package util;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Configuration implements ConfigurationInterface{
	
	private static Logger logger = Logger.getLogger(Configuration.class.getName());
	public static String databaseName = getProperty("databaseName");
	public static String existingDatabaseUser = getProperty("existingDatabaseUser");
	public static String existingDatabaseUserPasswd = getProperty("existingDatabaseUserPasswd");
	
	public static String testDatabaseUser = getProperty("testDatabaseUser");
	public static String testDatabaseUserPasswd = getProperty("testDatabaseUserPasswd");
	public static String databaseIP = getProperty("databaseIP");
	public static String databasePort = getProperty("databasePort");
	public static String homeDir= getProperty("homeDir");
	public static String testDir= getProperty("testDir");
	public static String smtsolver = getProperty("smtsolver");
	public static String smtargs = getProperty("smtargs");
	public static String logFile=getProperty("logFile");
	public static String logLevel=getProperty("logLevel");
	public static String tempJoins=getProperty("tempJoins");
	public static String existsUnrollFlag=getProperty("existsUnrollFlag");
	public static String outerSQ=getProperty("outerSQ");
	public static String isEnumInt=getProperty("isEnumInt");
	public static String enumArrayIndex=getProperty("enumArrayIndex");
	public static String enumIndexVar=getProperty("enumIndexVar");
	public static String regressDS0=getProperty("regressDS0");
	public static String sampleDataJson=getProperty("sampleDataJson");
	public static String tempDatabaseType = getProperty("tempDatabaseType");
	public static String dataset = getProperty("dataset");
	public static String queries = getProperty("queries");
	public static String mutants = getProperty("mutants");
	public static String sample = getProperty("sample");
	public static String base = getProperty("base");
	public static String primarykey = getProperty("primarykey");

	public static boolean calledFromApplicationTester = false;

	
	//public static String assignmentFolder= getProperty("assignmentFolder");
	public static ConfigurationInterface object;
	static void getObject(){
		try {
			object = (ConfigurationInterface) Class.forName("database.Configuration").newInstance();
		} catch (Exception e) {
			// Ignoring error
		}
		if(object == null){
			object = new Configuration();
			InitLogger.initLogger();
		}
	}
	
	public static String getProperty(String property)
	{
		if(object==null){
			getObject();
		}
		return object.getPropetyValue(property);
	}

	@Override
	public String getPropetyValue(String property) {
		Properties properties=new Properties();
		
		// Class.forName(name, initialize, loader)
		
		try{
        properties.load(Configuration.class.getResourceAsStream("XData.properties"));
		
	          
		}catch(IOException e){
			logger.log(Level.SEVERE,e.getMessage(),e);
			//e.printStackTrace();
			System.exit(1);
		} 
		String prop = properties.getProperty(property);
		if (prop== null)
		{
			logger.log(Level.SEVERE,"Property "+property+" not found");
			throw new NullPointerException();
		}
		return prop;
	}
}
