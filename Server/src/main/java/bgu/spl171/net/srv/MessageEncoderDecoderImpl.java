package bgu.spl171.net.srv;

import bgu.spl171.net.api.MessageEncoderDecoder;
import bgu.spl171.net.srv.packets.Packet;
import java.util.Arrays;

public class MessageEncoderDecoderImpl implements MessageEncoderDecoder<Packet> {

    private byte[] bytes = new byte[1 << 10]; //start with 1k
    private int len = 0;
    private short opcode=-1;
    private short packetSize=-100;    

    @Override
    public Packet decodeNextByte(byte nextByte) {
        //notice that the top 128 ascii characters have the same representation as their utf-8 counterparts
        //this allow us to do the following comparison
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }
        bytes[len] = nextByte;
        len++;


    	if (opcode==7 || opcode==8 || opcode==1 || opcode==2){
    		if (nextByte == 0) {
    			return popPacket();
    		}
    	}
    	
    	if (opcode==3){
			byte[] packetSizebytes=new byte[2];
    		if (len==4) {
    			packetSizebytes[0]=bytes[2];
    			packetSizebytes[1]=bytes[3];
    			packetSize=bytesToShort(packetSizebytes);
    		}
    			if (len==(packetSize+6))
    				return popPacket();
    	}
    	
    	if (opcode==4){
    		if (len==4) //right after the forth byte is in
    			return popPacket();
    	}
    	
    	if (opcode==5){
    		if (len>4 && nextByte==0) 
    			return popPacket();
    	}
    	
    	if (len>=2 && opcode==-1){
    		opcode=bytesToShort(bytes);
    	}
    	
    	if (opcode==6 || opcode==10)
    		return popPacket();
    	
        return null;
    }

    @Override
    public byte[] encode(Packet packet) {
    	packet.createBytes();
    	return packet.getBytes();
    }

    private Packet popPacket() {
    	Packet packet = null;
    	byte[] bytesCopy=Arrays.copyOf(bytes, bytes.length);
    	packet=new Packet(bytesCopy,opcode);
    	len=0;
    	opcode=-1;
    	packetSize=-100;
    	bytes = new byte[1 << 10];
        return packet;
    }
    
	private short bytesToShort(byte[] byteArr){
        short result = (short)((byteArr[0] & 0xff) << 8);
        result += (short)(byteArr[1] & 0xff);
        return result;
    }
	
	public byte[] shortToBytes(short num)
	{
	    byte[] bytesArr = new byte[2];
	    bytesArr[0] = (byte)((num >> 8) & 0xFF);
	    bytesArr[1] = (byte)(num & 0xFF);
	    return bytesArr;
	}

	

}