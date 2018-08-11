package bgu.spl171.net.srv.packets;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class LoginPacket extends Packet {

	private String userName;
	
	public LoginPacket(byte[] bytes){
		super(bytes,(short)7);
		int i=2;
		while (bytes[i]!=0)
			i++;
		setString(new String(bytes, 2, i-2, StandardCharsets.UTF_8));
	}
	
	public LoginPacket(String userName){
		super(null,(short)7);
		setString(userName);
	}

	public void setString(String string) {
		this.userName=string;
	}


	public String getUserName() {
		return userName;
	}
    
    public void createBytes(){
    	bytes=new byte[1 << 10]; 
    	importOpcode((short)7);
    	byte[] userNameBytes=userName.getBytes();
    	if (bytes.length-3<userNameBytes.length)
    		bytes = Arrays.copyOf(bytes, userNameBytes.length+3);
    	for (int i=0;i<userNameBytes.length;i++)
    		bytes[i+2]=userNameBytes[i];
    	bytes[userNameBytes.length+2]=0;
        }
}