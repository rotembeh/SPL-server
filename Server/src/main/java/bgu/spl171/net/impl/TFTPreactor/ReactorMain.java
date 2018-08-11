package bgu.spl171.net.impl.TFTPreactor;

import bgu.spl171.net.srv.BidiProtocolImpl;
import bgu.spl171.net.srv.MessageEncoderDecoderImpl;
import bgu.spl171.net.srv.Server;
import bgu.spl171.net.srv.packets.Packet;

public class ReactorMain {
	
	public static void main(String[] args) {
		String portstr=args[0];
		int port = Integer.parseInt(portstr);
		int nthreads=4;
		Server<Packet> Reactor=Server.reactor(nthreads,
		                port, ()->new BidiProtocolImpl(),       			
		                () -> new MessageEncoderDecoderImpl()
		                );
		Reactor.serve();
		}
	}
