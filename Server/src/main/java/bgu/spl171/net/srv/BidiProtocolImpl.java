package bgu.spl171.net.srv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import bgu.spl171.net.api.bidi.BidiMessagingProtocol;
import bgu.spl171.net.api.bidi.Connections;
import bgu.spl171.net.srv.packets.AckPacket;
import bgu.spl171.net.srv.packets.BCASTPacket;
import bgu.spl171.net.srv.packets.DataPacket;
import bgu.spl171.net.srv.packets.DelPacket;
import bgu.spl171.net.srv.packets.ErrorPacket;
import bgu.spl171.net.srv.packets.LoginPacket;
import bgu.spl171.net.srv.packets.Packet;
import bgu.spl171.net.srv.packets.ReadPacket;
import bgu.spl171.net.srv.packets.WritePacket;


public class BidiProtocolImpl implements BidiMessagingProtocol<Packet>  {
	
	public static ConcurrentHashMap<Integer,String> usernames=new ConcurrentHashMap<Integer,String>();
	public static Deque<String> filesList=new ConcurrentLinkedDeque<String>();
	private FileOutputStream currentFileOut;
	private String currentFileOutName;
	private Connections<Packet> connections;
	private int connectionId;
	boolean LogedIn=false;
	private short nextBlock=0;
	private long dataCounter=0;
	private FileInputStream currentInputFile;
	private String currentInputFileName;
	private int readOrDirqStatus=-1;
	LinkedList<byte[]> bytesArraysForDirq=new LinkedList<byte[]>();
	private short nextDIRQBlock=0;
	boolean shouldTerminate=false;
	
	@Override
	public void start(int connectionId, Connections<Packet> connections) {
		setConnectionId(connectionId);
		setConnections(connections);
	}

	@Override
	public void process(Packet packet) {
		short opcode=packet.getOpcode();
		byte[] bytes=packet.getBytes();	
		
		if(opcode>(short)10 || opcode<(short)1){
			ErrorPacket errorPacket=new ErrorPacket((short)4,"Illegal TFTP operation – Unknown Opcode.");
			 connections.send(connectionId, errorPacket);
			 return;
		}
		if(opcode!=7){        // the first command must be login
			if(!LogedIn){
				ErrorPacket errorPacket=new ErrorPacket((short)6,"User not logged in – Any opcode received before Login completes.");
		    	connections.send(connectionId, errorPacket);
				return;
			}
		}
    	switch(packet.getOpcode()){
    	case 1: {
    		ReadPacket readPacket=new ReadPacket(bytes);
    		String filename=readPacket.getFileName();
    		if (!filesList.contains(filename)){
    			ErrorPacket errorPacket=new ErrorPacket((short)1,"File not found – RRQ of non-existing file.");
    			connections.send(connectionId, errorPacket);
    			return;
    		}
    		File file=new File("Files/"+File.separator+filename);
    		currentInputFileName=filename;
    		currentInputFile=null;
    		readOrDirqStatus=0;
    		try {
    			currentInputFile=new FileInputStream(file);
			}
    		catch (FileNotFoundException e) {
    			ErrorPacket errorPacket=new ErrorPacket((short)1,"File not found – RRQ of non-existing file");
    			connections.send(connectionId,errorPacket);
    			e.printStackTrace();
    			readOrDirqStatus=-1;
    			currentInputFile=null;
    			currentInputFileName=null;
    			return;
			}
    		catch (SecurityException e){
    			ErrorPacket errorPacket=new ErrorPacket((short)2,"Access violation – File cannot be written, read or deleted.");
    			connections.send(connectionId,errorPacket);
    			readOrDirqStatus=-1;
    			currentInputFile=null;
    			currentInputFileName=null;
    			return;
    		}
    		dataCounter=file.length();
    		//send the first packet:::
			try {
				nextBlock=1;
				sendNextPacket();
			} catch (IOException e) {
				ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
				connections.send(connectionId, errorPacket);
				e.printStackTrace();
			}
			break;
    	}
    	case 2: {
    		WritePacket writePacket=new WritePacket(bytes);
    		if (filesList.contains(writePacket.getFileName())){
    			ErrorPacket errorPacket=new ErrorPacket((short)5,"File already exists – File name exists on WRQ.");
    			connections.send(connectionId, errorPacket);
    			return;
    		}
    		boolean init=initiateFile(writePacket.getFileName());
    		if (init){
    		AckPacket ackPacket=new AckPacket((short)0);
    		connections.send(connectionId, ackPacket);
    		}
    		break;
    	}
    	case 3: {
    		DataPacket dataPacket=new DataPacket(bytes);
    		if (dataPacket.getData().length>0) //or if equal to 512
    			continueWriteCurrentFile(dataPacket.getData());
    		if (dataPacket.getData().length<512){
				try {
					currentFileOut.close();
					filesList.addFirst(currentFileOutName);
		    		AckPacket ackPacket=new AckPacket(dataPacket.getBlock());
		    		connections.send(connectionId, ackPacket);		    	
		    		BCASTPacket bcastPacket=new BCASTPacket((byte)1,currentFileOutName);
		    		usernames.forEach((id,userName) -> connections.send(id, bcastPacket));
					currentFileOutName=null;
					currentFileOut=null;
				} catch (IOException e) {
					ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
					connections.send(connectionId, errorPacket);
					e.printStackTrace();
				}
    		}
    		else{
    		AckPacket ackPacket=new AckPacket(dataPacket.getBlock());
    		connections.send(connectionId, ackPacket);
    		}
    		break;
    	}
    	case 4: {
    		AckPacket ackPacket=new AckPacket(bytes);
    		switch(readOrDirqStatus){
    		case 0:{ //reading file
    		if (ackPacket.getBlock()!=nextBlock-1 && currentInputFileName!=null){ //WRONG ack pack:::
				ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
				connections.send(connectionId, errorPacket);
    			nextBlock=0;
    			readOrDirqStatus=-1;
    			try {
        			Path p = FileSystems.getDefault().getPath("Files/"+File.separator+currentInputFileName);
        			Files.delete(p);
					currentInputFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
    			currentInputFileName=null;
    		}
	    	if (ackPacket.getBlock()==nextBlock-1 && currentInputFileName!=null){
				try {
					sendNextPacket();
				} catch (IOException e) {
					ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
					connections.send(connectionId, errorPacket);
					e.printStackTrace();
				}
	    	}
	    	else{
	    		if (currentInputFileName==null && ackPacket.getBlock()==nextBlock-1){
	    			nextBlock=0;
	    			readOrDirqStatus=-1;
	    		}
	    	}
    		}
    		case 1:{ //DIRQing
        		if (ackPacket.getBlock()!=nextDIRQBlock-1 && !bytesArraysForDirq.isEmpty()){ //WRONG ACK pack:::
    				ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
    				connections.send(connectionId, errorPacket);
					nextDIRQBlock=0;
					readOrDirqStatus=-1;
					while (!bytesArraysForDirq.isEmpty())
						bytesArraysForDirq.remove();
        		}
    			if (ackPacket.getBlock()==nextDIRQBlock-1 && !bytesArraysForDirq.isEmpty() ){
    				sendNextPacketForDIRQ();
    			}		
    			else
    				if (ackPacket.getBlock()==nextDIRQBlock-1 && bytesArraysForDirq.isEmpty()){
    					nextDIRQBlock=0;
    					readOrDirqStatus=-1;
    				}
    		}
    		}
    		break;
    	}
    	case 5: {
    		ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
			connections.send(connectionId, errorPacket);
			break;
    	}
    	case 6: {
        	if (filesList.isEmpty()){ //case that no files yet
        		byte[] emp=new byte[0];
        		bytesArraysForDirq.addLast(emp);
         		readOrDirqStatus=1;
         		nextDIRQBlock=1;
        		sendNextPacketForDIRQ();
        	}
        	else{
    		 Iterator<String> iter=filesList.iterator();
     		short packetSize=(short) filesList.size();
     		String ans="";
     		for(int i=0; i<packetSize;i++){
     			ans=ans+iter.next()+'\0';
     		}
     		byte[] stringBytes=ans.getBytes();
     		int stringBytesRemaining=stringBytes.length;
     		int j=0;
     		byte[] toInsert=null;
     		readOrDirqStatus=1;
     		nextDIRQBlock=1;
     		while(stringBytesRemaining>0){   //dividing the stringBytes to packetDatas  
     			if(stringBytes.length-j>512){    // if the packet is longer than 512
     				int i=0;
     				toInsert=new byte[512];
     				while(i<512){
     					toInsert[i]=stringBytes[i+j];
     					i++;	
     				}
     				j=j+512;
     			}
     			else{ 						// if the packet is shorter than 512 (the size is stringBytes.length-j)
     				toInsert=new byte[stringBytes.length-j]; //=remaining
     				int i=0;
     				while(i<stringBytes.length-j){
     					toInsert[i]=stringBytes[i+j];
     					i++;	
     				}
     				j=j+(stringBytes.length-j);
     			}
     			bytesArraysForDirq.addLast(toInsert);		
     			stringBytesRemaining=stringBytesRemaining-j;	
     		}
     		sendNextPacketForDIRQ();
        	}
     		break;
     	}
    	case 7:{
    		LoginPacket loginPacket=new LoginPacket(bytes);
    		String userName=loginPacket.getUserName();
    		if(usernames.contains(userName)){
    		     ErrorPacket errorPacket=new ErrorPacket((short)7,"User already logged in – Login username already connected.");
				 connections.send(connectionId, errorPacket);
				 return;
    		}
    		else{
    			if(LogedIn){ //in case that client tried to login twice
        			 ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
    				 connections.send(connectionId, errorPacket);
    				 return;
    			}
    			usernames.put(connectionId, userName);
    			LogedIn=true;
    			AckPacket ackPacket=new AckPacket((short)0);
    			connections.send(connectionId, ackPacket);
    		} 
    		break;
    	} 
    	case 8: {
    		DelPacket delPacket=new DelPacket(bytes);
    		String fileName=delPacket.getFileName();
    		if (!filesList.contains(fileName)){
   			 ErrorPacket errorPacket=new ErrorPacket((short)1,"File not found – DELRQ of non-existing file");
				 connections.send(connectionId, errorPacket);
				 return;
    		}
    		try {
    			Path p = FileSystems.getDefault().getPath("Files/"+File.separator+fileName);
    			Files.delete(p);
    		} catch (NoSuchFileException e) {
    			 ErrorPacket errorPacket=new ErrorPacket((short)1,"File not found – DELRQ of non-existing file");
				 connections.send(connectionId, errorPacket);
				 return;
    		} catch (DirectoryNotEmptyException e) {
    			e.printStackTrace();
    			ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
				 connections.send(connectionId, errorPacket);
				 return;
    		} catch (IOException e) {
    			e.printStackTrace();
    			ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
				 connections.send(connectionId, errorPacket);
				 return;
    		}
    		filesList.remove(fileName);
    		AckPacket ackPacket=new AckPacket((short)0);
			connections.send(connectionId, ackPacket);
			
			// now we need to broadcast everyone that the file was deleted
			BCASTPacket bcastPacket=new BCASTPacket((byte)0,fileName);
			usernames.forEach((id,userName) -> connections.send(id, bcastPacket));
			break;
    	}
    	case 9: {
    		ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
			connections.send(connectionId, errorPacket);
			break;
    	}
    	case 10:{ 
    		usernames.remove(connectionId);
    		LogedIn=false;
    		AckPacket ackPacket=new AckPacket((short)0);
			connections.send(connectionId, ackPacket);
    		connections.disconnect(connectionId);
			shouldTerminate=true;
			break;
    		}    	
    	}
	}

	@Override
	public boolean shouldTerminate() {
		return shouldTerminate;
	}

	public Connections<Packet> getConnections() {
		return connections;
	}

	public void setConnections(Connections<Packet> connections) {
		this.connections = connections;
	}

	public int getConnectionId() {
		return connectionId;
	}

	public void setConnectionId(int connectionId) {
		this.connectionId = connectionId;
	}
	
	public boolean initiateFile(String filename){
		File file=new File("Files/"+File.separator+filename);
		if (file.exists()){
			ErrorPacket errorPacket=new ErrorPacket((short)5,"File already exists – File name exists on WRQ.");
			connections.send(connectionId,errorPacket);
			return false;
		}
		try {
			currentFileOut=new FileOutputStream(file,true);
		}	catch (SecurityException e){
			ErrorPacket errorPacket=new ErrorPacket((short)2,"Access violation – File cannot be written, read or deleted.");
			connections.send(connectionId,errorPacket);
			e.printStackTrace();
			return false;
		} 
		catch (FileNotFoundException e){
			ErrorPacket errorPacket=new ErrorPacket((short)3,"Disk full or allocation exceeded – No room in disk.");
			connections.send(connectionId,errorPacket);
			e.printStackTrace();
			return false;
		}
		currentFileOutName=filename;
		return true;
	}
	
	private void continueWriteCurrentFile(byte[] dataToWrite){
		try {
			currentFileOut.write(dataToWrite);
		} catch (IOException e) {
			ErrorPacket errorPacket=new ErrorPacket((short)3,"Disk full or allocation exceeded – No room in disk.");
			connections.send(connectionId,errorPacket);
			e.printStackTrace();
		}
	}
	private void sendNextPacketForDIRQ(){
		byte[] dataBytes=bytesArraysForDirq.removeFirst();
		DataPacket dataPacket=new DataPacket((short)dataBytes.length,nextDIRQBlock,dataBytes);
		connections.send(connectionId, dataPacket);
			nextDIRQBlock++;
	}
	
	private void sendNextPacket() throws IOException{
		short currentDataPacketSize;
		if (dataCounter>=512)
			currentDataPacketSize=512;
		else
			currentDataPacketSize=(short) (dataCounter%512);
		byte[] dataBytes=new byte[currentDataPacketSize];
		int CurPacketCounter=0;
		while(CurPacketCounter < currentDataPacketSize){
			int bytesRead = currentInputFile.read(dataBytes);
			if (bytesRead > 0){
	        	  dataCounter = dataCounter - bytesRead;
	        	  CurPacketCounter=CurPacketCounter + bytesRead;
	        }
			if (bytesRead==-1 && CurPacketCounter < currentDataPacketSize){
				ErrorPacket errorPacket=new ErrorPacket((short)0,"Not defined, see error message (if any).");
				connections.send(connectionId, errorPacket);
				return;
			}
		}
		DataPacket dataPacket=new DataPacket(currentDataPacketSize,nextBlock,dataBytes);
		if (currentDataPacketSize==512)
			nextBlock++;
		connections.send(connectionId, dataPacket);
		if (currentDataPacketSize<512){
			nextBlock++;
			currentInputFile.close();
			currentInputFileName=null;
		}			
		}
		
	}