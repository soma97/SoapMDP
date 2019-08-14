package axis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import net.etfbl.model.*;

public class SoapAdminService {
		public static String DATABASE=null;
		static final Logger LOGGER = Logger.getLogger("Logger");
	    static FileHandler handler;
		InputStream inputProp=getClass().getClassLoader().getResourceAsStream(".."+File.separatorChar+"resources"+File.separatorChar+"config.properties");
		static {
			Properties prop = new Properties();
	        try {
	            prop.load(new SoapAdminService().inputProp); 
	            DATABASE = prop.getProperty("DATABASE");
	            handler=new FileHandler("error.log");
	            LOGGER.addHandler(handler);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}
	  public boolean addEmployee(String username,String passwordHash)
	  {
		  Jedis jedis=new Jedis(DATABASE);
		  if(jedis.exists(username.getBytes()))
		  {
			  jedis.close();
			  return false;
		  }
		  Employee employee=new Employee(username,passwordHash);
		  try(ByteArrayOutputStream bos=new ByteArrayOutputStream();
					ObjectOutput out=new ObjectOutputStream(bos)) {  
			  out.writeObject(employee);
			  out.flush();
			  byte[] objectBytes = bos.toByteArray();
			  jedis.set(employee.getUsername().getBytes(), objectBytes);
		  }catch(Exception ex)
		  {
			  ex.printStackTrace();
			  StackTraceElement elements[] = ex.getStackTrace();
		        for (StackTraceElement element:elements) 
		            LOGGER.log(Level.WARNING, element.toString());
			  return false;
		  }		 
		  finally {
			  jedis.close();
		  }
		  return true;
	  }
	  public boolean blockEmployee(String username,String passswordHash)
	  {
		  Jedis jedis=new Jedis(DATABASE);
		  Employee toChange=null;
		  try(ByteArrayInputStream bis = new ByteArrayInputStream(jedis.get(username.getBytes()));
				    ObjectInput in = new ObjectInputStream(bis))
		  {
			  toChange=(Employee)in.readObject();
		  }catch(Exception e)
		  {
			  e.printStackTrace();
			  jedis.close();
			  StackTraceElement elements[] = e.getStackTrace();
		        for (StackTraceElement element:elements) 
		            LOGGER.log(Level.WARNING, element.toString());
			  return false;
		  }
		  if(toChange==null) {
			  jedis.close();
			  return false;
		  }
		  jedis.del(username.getBytes());
		  toChange.setBlocked(true);
		  try(ByteArrayOutputStream bos=new ByteArrayOutputStream();
					ObjectOutput out=new ObjectOutputStream(bos)) {  
			  out.writeObject(toChange);
			  out.flush();
			  byte[] objectBytes = bos.toByteArray();
			  jedis.set(toChange.getUsername().getBytes(), objectBytes);
		  }catch(Exception ex)
		  {
			  ex.printStackTrace();
			  StackTraceElement elements[] = ex.getStackTrace();
		        for (StackTraceElement element:elements) 
		            LOGGER.log(Level.WARNING, element.toString());
			  return false;
		  }
		  finally {
			  jedis.close();
		  }
		  return true;
	  }

}
