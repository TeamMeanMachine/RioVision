import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;


public class AxisCamera {
	String ipAddress;
	
	public AxisCamera(String ip) {
		ipAddress = ip;
	}

	public String getAllParams() {
		return urlRequest("/axis-cgi/param.cgi?usergroup=admin&action=list");
	}
	
	public void setColorLevel(int level) {
		urlRequest("/axis-cgi/param.cgi?action=update&ImageSource.I0.Sensor.ColorLevel=" + level);
	}
	
	public int getColorLevel() {
		String valuePair = urlRequest("/axis-cgi/param.cgi?action=list&group=ImageSource.I0.Sensor.ColorLevel");
		return Integer.parseInt(valuePair.trim().split("=")[1]);
	}
	
	public void setBrightness(int brightness) {
		urlRequest("/axis-cgi/param.cgi?action=update&ImageSource.I0.Sensor.Brightness=" + brightness);
	}
	
	public int getBrightness() {
		String valuePair = urlRequest("/axis-cgi/param.cgi?action=list&group=ImageSource.I0.Sensor.Brightness");
		return Integer.parseInt(valuePair.trim().split("=")[1]);
	}
	
	public void setSharpness(int sharpness) {
		urlRequest("/axis-cgi/param.cgi?action=update&ImageSource.I0.Sensor.Sharpness=" + sharpness);
	}
	
	public int getSharpness() {
		String valuePair = urlRequest("/axis-cgi/param.cgi?action=list&group=ImageSource.I0.Sensor.Sharpness");
		return Integer.parseInt(valuePair.trim().split("=")[1]);
	}
	
	public void setContrast(int contrast) {
		urlRequest("/axis-cgi/param.cgi?action=update&ImageSource.I0.Sensor.Contrast=" + contrast);
	}
	
	public int getContrast() {
		String valuePair = urlRequest("/axis-cgi/param.cgi?action=list&group=ImageSource.I0.Sensor.Contrast");
		return Integer.parseInt(valuePair.trim().split("=")[1]);
	}
	
	public void setExposure(int exposure) {
		urlRequest("/axis-cgi/param.cgi?action=update&ImageSource.I0.Sensor.ExposureValue=" + exposure);
	}
	
	public int getExposure() {
		String valuePair = urlRequest("/axis-cgi/param.cgi?action=list&group=ImageSource.I0.Sensor.ExposureValue");
		return Integer.parseInt(valuePair.trim().split("=")[1]);				
	}
	
	public String getVideoURL() {
		return "http://" + ipAddress + "/axis-cgi/mjpg/video.cgi?dummy=param.mjpg";
	}
	
	private String urlRequest(String address) {
		try {
			URL url = new URL("http://" + ipAddress + address);
	        URLConnection con = url.openConnection();
	        String auth = Base64.encodeBase64String("root:root".getBytes());
			con.setRequestProperty("Authorization", "Basic " + auth);
	        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
	        String inputLine;
	        String input = "";
	        while ((inputLine = in.readLine()) != null) {
	            input += inputLine + "\n";
	        }
	        in.close();
	        return input;
		} catch(IOException e) {
			e.printStackTrace();
			return e.toString();
		}
	}
	
}
