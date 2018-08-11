package bgu.spl171.net.srv.packets;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ErrorPacket extends Packet {

	private short errorCode;
	private String errMsg;
	
	public ErrorPacket(byte[] bytes){
		super(bytes,(short)5);
		setErrorCode(errorbytesToShort(bytes));
		int i=4;
		while (bytes[i]!=0)
			i++;
		setErrMsg(new String(bytes, 4, i-4, StandardCharsets.UTF_8));
	}
	
	public ErrorPacket(short errorCode,String errMsg){
		super(null,(short)5);
		setErrorCode(errorCode);
		setErrMsg(errMsg);
	}

	
	private short errorbytesToShort(byte[] byteArr){
        short result = (short)((byteArr[2] & 0xff) << 8);
        result += (short)(byteArr[3] & 0xff);
        return result;
    }
	



	public short getErrorCode() {
		return errorCode;
	}


	public void setErrorCode(short errorCode) {
		this.errorCode = errorCode;
	}


	public String getErrMsg() {
		return errMsg;
	}


	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}
	
    public void createBytes(){
        	bytes=new byte[1 << 10]; 
        	importOpcode((short)(5));
    		importErrorCode(errorCode);
    		byte[] errorMsgBytes=errMsg.getBytes();
    		if (bytes.length-5<errorMsgBytes.length)
    			bytes = Arrays.copyOf(bytes, errorMsgBytes.length+5);
    		for (int i=0;i<errorMsgBytes.length;i++)
    			bytes[i+4]=errorMsgBytes[i];
    		bytes[errorMsgBytes.length+4]=0;
        }
    
	 private void importErrorCode(short num){
	     bytes[2] = (byte)((num >> 8) & 0xFF);
	     bytes[3] = (byte)(num & 0xFF);
	 }
}