package bgu.spl171.net.srv.packets;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BCASTPacket extends Packet {

	private byte delAdd;
	private String FileName;
	
	
	public BCASTPacket(byte[] bytes){
		super(bytes,(short)9);
		setDelAdd(bytes[3]);
		int i=3;
		while (bytes[i]!=0)
			i++;
		setFileName(new String(bytes, 3, i-3, StandardCharsets.UTF_8));
	}
	
	public BCASTPacket(byte delAdd, String FileName){
		super(null,(short)9);
		setDelAdd(delAdd);
		setFileName(FileName);
	}


	public String getFileName() {
		return FileName;
	}


	public void setFileName(String fileName) {
		FileName = fileName;
	}


	public byte getDelAdd() {
		return delAdd;
	}


	public void setDelAdd(byte delAdd) {
		this.delAdd = delAdd;
	}
	
    public void createBytes(){
    	bytes=new byte[1 << 10]; 
    	importOpcode((short)(9));
		bytes[2]=delAdd;
		byte[] fileNameBytes=FileName.getBytes();
		if (bytes.length-4<fileNameBytes.length)
			bytes = Arrays.copyOf(bytes, fileNameBytes.length+4);
		for (int i=0;i<fileNameBytes.length;i++)
			bytes[i+3]=fileNameBytes[i];
		bytes[fileNameBytes.length+3]=0;
    }
}