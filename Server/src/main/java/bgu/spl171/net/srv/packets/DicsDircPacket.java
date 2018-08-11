package bgu.spl171.net.srv.packets;

public class DicsDircPacket extends Packet {

	public DicsDircPacket(byte[] bytes, short opcode) {
		super(bytes, opcode);
	}

	@Override
	public void createBytes() {
		importOpcode(super.opcode);
	}
	
}
