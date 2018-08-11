package bgu.spl171.net.impl.TFTPtpc;
import bgu.spl171.net.srv.BidiProtocolImpl;
import bgu.spl171.net.srv.MessageEncoderDecoderImpl;
import bgu.spl171.net.srv.Server;
import bgu.spl171.net.srv.packets.Packet;

public class TPCMain {

	public static void main(String[] args) {
		String portstr=args[0];
		int port = Integer.parseInt(portstr);
		Server<Packet> baseServer=Server.threadPerClient(
		                port, ()-> new BidiProtocolImpl(),       			
		                () -> new MessageEncoderDecoderImpl()
		                );
		baseServer.serve();
		}
	}


