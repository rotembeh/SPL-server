package bgu.spl171.net.srv.packets;

public class Packet {
	protected byte[] bytes;
	protected short opcode;
	
	public Packet(byte[] bytes, short opcode){
		this.bytes=bytes;
		this.opcode=opcode;
	}

	public short getOpcode(){
		return opcode;
	}
	
	public byte[] getBytes(){
		return bytes;
	}
	
    public void createBytes(){
    	bytes=new byte[2];
    	importOpcode(opcode);
    }
 //   public abstract void execute(Connections<Packet> connections);
    
	protected void importOpcode(short num)
	{
	    bytes[0] = (byte)((num >> 8) & 0xFF);
	    bytes[1] = (byte)(num & 0xFF);
	}
	
	
}
