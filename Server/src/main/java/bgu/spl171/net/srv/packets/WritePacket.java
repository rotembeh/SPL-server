package bgu.spl171.net.srv.packets;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class WritePacket extends Packet{
private String fileName;
	
	public WritePacket(byte[] bytes){
		super(bytes,(short)2);
		int len=2;
		while (bytes[len]!=0)
			len++;
		fileName=new String(bytes, 2, len-2, StandardCharsets.UTF_8);
	}
	
    public WritePacket(String fileName){
   		super(null,(short)2);
   		setString(fileName);
    }
	

	public void setString(String string) {
		this.fileName=string;
	}


	public String getFileName() {
		return fileName;
	}
	
	public void createBytes(){
    	bytes=new byte[1 << 10]; 
    	importOpcode((short)2);
    	byte[] fileNameBytes=fileName.getBytes();
    	if (bytes.length-3<fileNameBytes.length)
    		bytes = Arrays.copyOf(bytes, fileNameBytes.length+3);
    	for (int i=0;i<fileNameBytes.length;i++)
    		bytes[i+2]=fileNameBytes[i];	
  		bytes[fileNameBytes.length+2]=0;
        }
	
}