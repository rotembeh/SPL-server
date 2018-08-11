package bgu.spl171.net.srv.packets;

public class DataPacket extends Packet {
	private short packetSize;
	private short block;
	private byte[] data;
	
	public DataPacket(byte[] bytes){
		super(bytes,(short)3);
		setPacketSize(packetSizebytesToShort(bytes));
		setBlock(blockbytesToShort(bytes));
		data=new byte[packetSize];
		for(int i=0;i<packetSize;i++){
			data[i]=bytes[i+6];
		}
			
	}
	
	public DataPacket(short packetSize,short block, byte[] data){
		super(null,(short)3);
		setPacketSize(packetSize);
		setBlock(block);
		setData(data);
			
	}
	
	private short packetSizebytesToShort(byte[] byteArr){
        short result = (short)((byteArr[2] & 0xff) << 8);
        result += (short)(byteArr[3] & 0xff);
        return result;
    }
	
	private short blockbytesToShort(byte[] byteArr){
        short result = (short)((byteArr[4] & 0xff) << 8);
        result += (short)(byteArr[5] & 0xff);
        return result;
    }
	
	public short getPacketSize() {
		return packetSize;
	}


	public void setPacketSize(short packetSize) {
		this.packetSize = packetSize;
	}


	public short getBlock() {
		return block;
	}


	public void setBlock(short block) {
		this.block = block;
	}


	public byte[] getData() {
		return data;
	}


	public void setData(byte[] data) {
		this.data = data;
	}
    
	public void createBytes(){
		bytes=new byte[6+packetSize];
		importOpcode((short)(3));
		importPacketSize(packetSize);
		importBlock(block);
		for(int i=0;i<data.length;i++)
			bytes[i+6]=data[i];
    }
	
	 private void importPacketSize(short num){
	     bytes[2] = (byte)((num >> 8) & 0xFF);
	     bytes[3] = (byte)(num & 0xFF);
	 }
	 
	 private void importBlock(short num){
	     bytes[4] = (byte)((num >> 8) & 0xFF);
	     bytes[5] = (byte)(num & 0xFF);
	 }
}