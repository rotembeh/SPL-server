package bgu.spl171.net.srv.packets;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class DelPacket extends Packet {
	private String fileName;
	
	public DelPacket(byte[] bytes){
		super(bytes,(short)8);
		int i=2;
		while (bytes[i]!=0)
			i++;
		setString(new String(bytes, 2, i-2, StandardCharsets.UTF_8));
	}
	
	public DelPacket(String fileName){
		super(null,(short)8);
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
    	importOpcode((short)8);
    	byte[] fileNameBytes=fileName.getBytes();
    	if (bytes.length-3<fileNameBytes.length)
    		bytes = Arrays.copyOf(bytes, fileNameBytes.length+3);
    	for (int i=0;i<fileNameBytes.length;i++)
    		bytes[i+2]=fileNameBytes[i];		
    	bytes[fileNameBytes.length+2]=0;
    	}
	}