package com.minder.app.tf2backpack;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;

public class Util {
	private static DatabaseHandler dbHandler;
	
	public static DatabaseHandler getDbHandler() {
		if (dbHandler == null) {
			dbHandler = new DatabaseHandler(App.getAppContext());
		}
		return dbHandler;
	}

	public static int GetPxFromDp(Context context, float dp)
	{
		float scale = context.getResources().getDisplayMetrics().density;
		return (int)(dp * scale + 0.5f);
	}
	
	public final static String GetAPIKey(){
		return "***REMOVED***";
	}
	
	public final static String GetTag(){
		return "TF2Backpack";
	}
	
    public final static int getItemColor(int quality){
    	switch (quality) {
	    	case 1: {
	    		return Color.rgb(77, 116, 85);
	    	}
	    	case 3: {
	    		return Color.rgb(71, 98, 145);
	    	} 
	    	case 5: {
	    		return Color.rgb(134, 80, 172);
	    	}
	    	case 6: {
	    		return Color.rgb(255, 215, 0);
	    	}
	    	case 7: {
	    		return Color.rgb(112, 176, 74);
	    	}
	    	case 8: {
	    		return Color.rgb(165, 15, 121);
	    	}
	    	case 9: {
	    		return Color.rgb(112, 176, 74);
	    	}
	    	case 10: {
	    		return Color.rgb(71, 98, 145);
	    	}
	    	case 11: {
	    		return Color.rgb(207, 106, 50);
	    	}
	    	case 13: {
	    		return Color.rgb(56, 243, 171);
	    	}
	    	default: {
	    		return Color.rgb(178, 178, 178);
	    	}
    	}  	
    }
	
	public static void chkStatus(Context context)
	{
		final ConnectivityManager connMgr = (ConnectivityManager)
		context.getSystemService(Context.CONNECTIVITY_SERVICE);
		
		final android.net.NetworkInfo wifi =
			connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
		final android.net.NetworkInfo mobile =
			connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		
		if (wifi.isAvailable()){

		}
		else if (mobile.isAvailable()){
		}
		else
		{

		}
	}
	
    public static void CopyStream(InputStream is, OutputStream os)
    {
        final int buffer_size=1024;
        try
        {
            byte[] bytes=new byte[buffer_size];
            for(;;)
            {
              int count=is.read(bytes, 0, buffer_size);
              if(count==-1)
                  break;
              os.write(bytes, 0, count);
            }
        }
        catch(Exception ex){}
    }
    
    public static String md5Hash(String s) {
		try {
	    	MessageDigest digester = MessageDigest.getInstance("MD5");			
	    	byte[] digest = digester.digest(s.getBytes());
	    	
	        // Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i = 0; i < digest.length; i++)
	            hexString.append(Integer.toHexString(0xFF & digest[i]));
	        return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}	
		return null;
    }

}
