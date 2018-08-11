#include <iostream>
#include <boost/thread.hpp>
#include <connectionHandler.h>
#include <Task.h>
#include <string>
#include <iostream>
#include <fstream>

using boost::asio::ip::tcp;
using namespace::std;

    Task::Task(int id, boost::mutex* mutex,ConnectionHandler* connectionHandler) : _id(id), _mutex(mutex),_connectionHandler(connectionHandler), nextBlock(0), FileToSendName("0"),RemainingBytesToSend(0),FileToSaveName("0"),DIRQING(false),shouldTerminate(false),disconnecting(false),dirqSavememSize(0), savememSize(0),dirqSavemem(0),savemem(0),memblock(0),filenameToPrint("0"),filenameToPrint2("0"),logged(false),triedlog(false) {
    }

    Task::Task(const Task&): _id(0), _mutex(0),_connectionHandler(0), nextBlock(0), FileToSendName("0"),RemainingBytesToSend(0),FileToSaveName("0"),DIRQING(false),shouldTerminate(false),disconnecting(false),dirqSavememSize(0), savememSize(0),dirqSavemem(0),savemem(0),memblock(0),filenameToPrint("0"),filenameToPrint2("0"),logged(false),triedlog(false) {
    }
    Task& Task::operator=(const Task& other){
    	return *this;
    }

    void Task::run1(){
        while(!disconnecting){
    	    string input,command,name;
            getline(cin,input);
            istringstream s(input);
            s>>command;
            s>>name;
            string third;
            s>>third;
            short opcode=-1;
            string cln="";
            string login="LOGRQ";
            string read="RRQ";
            string write="WRQ";
            string disc="DISC";
            string dirq="DIRQ";
            string delrq="DELRQ";
            bool validInput=true;
	if (third.compare(cln)!=0) {
		validInput=false; //if there is third word
	}
	else
	if (command.compare(read)==0){
		if (name.compare(cln)==0) validInput=false;
		opcode=1;
	}
	else
		if (command.compare(write)==0){
			if (name.compare(cln)==0) validInput=false;
			opcode=2;
		}
		else
			if (command.compare(login)==0){
				if (name.compare(cln)==0) validInput=false;
				opcode=7;
			}
			else
				if (command.compare(disc)==0){
					if (name.compare(cln)!=0) validInput=false;
					opcode=10;
				}
				else
					if (command.compare(dirq)==0){
						if (name.compare(cln)!=0) validInput=false;
						opcode=6;
					}
					else
						if (command.compare(delrq)==0){
							if (name.compare(cln)==0) validInput=false;
							opcode=8;
						}
					else{
						validInput=false;
					}
	if (validInput==true){
	char OpcodeBytes[]="  ";
	shortToBytes(opcode, OpcodeBytes); //insert opcode to the 0,1 places in bytes
	switch(opcode){
					case 1:{
						 ifstream infile(name);
					    if (infile.good()){
					    	cout<<"file already exist on client side"<<endl;
					    }
					    else{
			            boost::mutex::scoped_lock lock(*_mutex);
			            _connectionHandler->sendBytes(OpcodeBytes,2);
			            if(_connectionHandler->sendFrameAscii(name,'\0')){
			            	savemem=new char[10000000];
			            	savememSize=10000000;
			            	FileToSaveName=name;
			            	filenameToPrint2=name;
			            }
					    }
						break;
					}
					case 2:{
					    ifstream infile(name);
					    if (!infile.good()){
					    	cout<<"file isn't exist on client side"<<endl;
					    }
					    else{
						boost::mutex::scoped_lock lock(*_mutex);
						_connectionHandler->sendBytes(OpcodeBytes,2);
						if (_connectionHandler->sendFrameAscii(name,'\0')){
					    	nextBlock=1;
							FileToSendName=name;
							filenameToPrint=name;
						}
					    }
						break;
					}
					case 6:{
						boost::mutex::scoped_lock lock(*_mutex);
						if(_connectionHandler->sendBytes(OpcodeBytes, 2))
							DIRQING=true;
						break;
					}
					case 7:{
						boost::mutex::scoped_lock lock(*_mutex);
						_connectionHandler->sendBytes(OpcodeBytes,2);
						if(_connectionHandler->sendFrameAscii(name,'\0'))
							triedlog=true;
						break;
					}
					case 8:{
						boost::mutex::scoped_lock lock(*_mutex);
						_connectionHandler->sendBytes(OpcodeBytes,2);
						_connectionHandler->sendFrameAscii(name,'\0');
						break;
					}
					case 10:{
						boost::mutex::scoped_lock lock(*_mutex);
						if (_connectionHandler->sendBytes(OpcodeBytes, 2))
							if (logged)
								disconnecting=true;
						break;
					}
            }
        }
	else
		cout<<"invalid command"<<endl;
        }
    }
    void Task:: run2(){
    	while (!shouldTerminate){
    		short opcode=-1;
    		char* bytes=new char[1024];
    		if (_connectionHandler->getBytes(bytes,1024))
    		{
    			opcode=bytesToShort(bytes);
    			switch (opcode){
    					case 3:{
    						if (!DIRQING){
    							saveNextPacket(bytes);
    							char* ackPack=new char[4];
    							shortToBytes(4,ackPack);
    							putSizeBytes(getThirdShort(bytes),ackPack); //puts blocknum in the ack pack
    							boost::mutex::scoped_lock lock(*_mutex);
    							_connectionHandler->sendBytes(ackPack, 4);
    							delete ackPack;
    						}
    						if (DIRQING){
    							if (getThirdShort(bytes)==1){
    								dirqSavemem=new char[10000];
    								dirqSavememSize=10000;
    							}
    							saveNextDirqPack(bytes);
    							char* ackPack=new char[4];
    							shortToBytes(4,ackPack);
    							putSizeBytes(getThirdShort(bytes),ackPack); //puts blocknum in the ack pack
    							boost::mutex::scoped_lock lock(*_mutex);
    							_connectionHandler->sendBytes(ackPack, 4);
    							delete ackPack;
    						}
    							break;
    					}
    					case 4:{
    						short block=getSecondShort(bytes);
    						if (block==0){
    							cout<<"ACK 0"<<endl;
							if (triedlog){
								logged=true; 
								triedlog=false;
								}
    							if (nextBlock==1)
    								sendFirstBlock();
    							if (disconnecting)
    								shouldTerminate=true;
    						}
    						else{
    							cout<<"ACK "<<block<<endl;
       							if (block!=nextBlock-1 && FileToSendName!="0"){ //WRONG ACK PACK NUMBER
    							cout<<"wrong block num from server"<<endl;
    							nextBlock=0;
    							FileToSendName="0";
    						    delete memblock;
    						    filenameToPrint="";
    							}
    							else
    								if (block==nextBlock-1 && FileToSendName!="0"){
    									sendNextBlock();
    								}
    								else
    									if (block==nextBlock-1 && FileToSendName=="0"){ //its the last ACK for that file
    										nextBlock=0;
    										cout<<"WRQ "<<filenameToPrint<<" complete"<<endl;
    										filenameToPrint="";
    									}
    							}
    							break;
    					}
    					case 5:{
    					    	short errorcode=getSecondShort(bytes);
    					    	cout<<"Error "<<errorcode<<endl;
						if (triedlog)
							triedlog=false;
    					    	if (nextBlock!=0){
    					    		nextBlock=0;
    					    		FileToSendName="0";
    					    	}
    					    	break;
    					}
    					case 9:{
    					  		  string delAdd;
    				  			  string fileName="";
    				  			  if(bytes[2]=='\0')
    					   			  delAdd="del";
    				 			  else
    		    					  delAdd="add";
   				    			  char* workString=new char[1024];
   				    			  int i=0;
   					    		  while(bytes[i+3]!='\0'){
    					  			  workString[i]=bytes[i+3];
    					  			  i++;
   					    		  }
   					    		  string str(workString, workString+i);

    			    			  cout<<"BCAST "<<delAdd<<" "<<str<<endl;
    			    			  delete workString;
    			    			  break;
    					    	 }
    			}
    		}
    		delete bytes;

    }
    }

    short Task::bytesToShort(char* bytesArr)
    {
        short result = (short)((bytesArr[0] & 0xff) << 8);
        result += (short)(bytesArr[1] & 0xff);
        return result;
    }

    short Task:: getSecondShort(char* bytesArr)
    {
        short result = (short)((bytesArr[2] & 0xff) << 8);
        result += (short)(bytesArr[3] & 0xff);
        return result;
    }
    short Task::getThirdShort(char* bytesArr)
    {
        short result = (short)((bytesArr[4] & 0xff) << 8);
        result += (short)(bytesArr[5] & 0xff);
        return result;
    }

    void Task::sendFirstBlock(){
    	  streampos size;
    	  ifstream file (FileToSendName, ios::in|ios::binary|ios::ate);
    	  if (file.is_open())
    	  {
    	    size = file.tellg();
    	    memblock = new char [size];
    	    file.seekg (0, ios::beg);
    	    file.read (memblock, size);
    	    file.close();
    	  }
    	  else {
    		  RemainingBytesToSend=0;
    		  delete memblock;
    		  cout << "VERY BAD ERROR. Unable to open file";
    	  }
    	  RemainingBytesToSend=size;
    	  sendNextBlock();
    }


    void Task::sendNextBlock(){
    	 char* sixFirstBytes=new char[6];
    	 short dataPacketSize;
    	 if (RemainingBytesToSend>=512){
    		 dataPacketSize=512;
    	 }
    	 else
    		 dataPacketSize=RemainingBytesToSend;
    	    	  RemainingBytesToSend=RemainingBytesToSend-dataPacketSize;
    	    	  shortToBytes(3,sixFirstBytes);
    	       	  putSizeBytes((short)dataPacketSize,sixFirstBytes);
    	       	  putBlockBytes(nextBlock,sixFirstBytes);

    	    	  //sending data packet now:::
		          boost::mutex::scoped_lock lock(*_mutex);
    	    	  if (!_connectionHandler->sendBytes(sixFirstBytes,6)){
    	    		  cout<<"file send failed (first six bytes), try again"<<endl;
    	    		  nextBlock=0;
    	    		  FileToSendName="0";
    	    	  }
    	    	  else
    	    		  if (!_connectionHandler->sendBytes(&memblock[512*(nextBlock-1)],(int)dataPacketSize)){
    	    		      		  cout<<"file send failed (data bytes), try again"<<endl;
    	    		      		  nextBlock=0;
    	    		      		  FileToSendName="0";
    	    		      	  }
    	 if (RemainingBytesToSend==0 && dataPacketSize !=512) //so dataPacketSize<512, all the file has sent , dataPacketSize!=512 for case file size isDividedwith512
      	 {
    		 FileToSendName="0";
    	     delete memblock;
    	     }
    	nextBlock++;
    	delete sixFirstBytes;
    }
    void Task::shortToBytes(short num, char* bytesArr)
    {
        bytesArr[0] = ((num >> 8) & 0xFF);
        bytesArr[1] = (num & 0xFF);
    }
    void Task::putSizeBytes(short num, char* bytesArr)
    {
        bytesArr[2] = ((num >> 8) & 0xFF);
        bytesArr[3] = (num & 0xFF);
    }
    void Task::putBlockBytes(short num, char* bytesArr)
    {
        bytesArr[4] = ((num >> 8) & 0xFF);
        bytesArr[5] = (num & 0xFF);
    }

    void Task::saveNextPacket(char* bytes){
    	short block=getThirdShort(bytes);
    	short packetSize=getSecondShort(bytes);
    	if (savememSize<(block-1)*512+packetSize){//in case savemem has not enough place for the next packet
    		savememSize=2*savememSize;
    		char * savememCopy=new char[savememSize];
    		for (int i=0;i<(block-1)*512;i++)
           		fill_n(&savememCopy[i],1,savemem[i]);
    		delete savemem;
    		savemem=new char[savememSize];
       		for (int i=0;i<(block-1)*512;i++)
               		fill_n(&savemem[i],1,savememCopy[i]);
       		delete savememCopy;
    	}

    	for (int i=0;i<packetSize; i++){
       		fill_n(&savemem[(block-1)*512+i],1,bytes[i+6]);
       	}
    	if (packetSize<512){
        	ofstream myFile (FileToSaveName, ios::out | ios::binary);
        	myFile.write (savemem, (block-1)*512+packetSize );
        	delete savemem;
        	myFile.close();
        	savememSize=-1;
        	FileToSaveName="0";
        	cout<<"RRQ "<<filenameToPrint2<<" complete"<<endl;
        	filenameToPrint2="";
    	}
    }

    void Task::saveNextDirqPack(char* bytes){
    	short packetSize=getSecondShort(bytes);
    	short block=getThirdShort(bytes);
    	if (dirqSavememSize<(block-1)*512+packetSize){//in case savemem has not enough place for the next packet
    		dirqSavememSize=2*dirqSavememSize;
    	    		char * savememCopy=new char[dirqSavememSize];
    	    		for (int i=0;i<(block-1)*512;i++)
    	           		fill_n(&savememCopy[i],1,dirqSavemem[i]);
    	    		delete dirqSavemem;
    	    		dirqSavemem=new char[dirqSavememSize];
    	       		for (int i=0;i<(block-1)*512;i++)
    	               		fill_n(&dirqSavemem[i],1,savememCopy[i]);
    	       		delete savememCopy;
    	    	}
		for (int i=0;i<packetSize;i++)
       		fill_n(&dirqSavemem[(block-1)*512+i],1,bytes[i+6]);

    	if (packetSize<512){
    		int totalBytes=(block-1)*512+packetSize;

    		for (int i=0;i<totalBytes;i++){
    			cout<<dirqSavemem[i];
    			if (dirqSavemem[i]=='\0')
    				cout<<endl;
    		}
     		 DIRQING=false;
     		 delete dirqSavemem;
     		 dirqSavememSize=-1;
 			 }
    	}
