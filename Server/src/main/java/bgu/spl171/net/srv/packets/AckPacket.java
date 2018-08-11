package bgu.spl171.net.srv.packets;

public class AckPacket extends Packet {
 private short block;
 
 public AckPacket(byte[] bytes){
	 super(bytes,(short)4);
		 setBlock(blockToShort(bytes));	 
	 }
	 
 public AckPacket(short block){
	 	super(null,(short)4);
		setBlock(block);
	 }
	 
 
 private short blockToShort(byte[] byteArr){
     short result = (short)((byteArr[2] & 0xff) << 8);
     result += (short)(byteArr[3] & 0xff);
     return result;
 }
 
 private void importBlock(short num){
	     bytes[2] = (byte)((num >> 8) & 0xFF);
	     bytes[3] = (byte)(num & 0xFF);
	 }

public int getBlock() {
	return block;
}
public void setBlock(short block) {
	this.block =  block;
}

public void createBytes(){
	bytes=new byte[4];
	importOpcode((short)(4));
	importBlock((block));
}
 
}