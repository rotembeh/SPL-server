#ifndef TASK__
#define TASK__
                                           
#include <string>
#include <iostream>
#include <boost/asio.hpp>

using boost::asio::ip::tcp;
using namespace::std;

class Task{

private:
    int _id;
    boost::mutex * _mutex;
    ConnectionHandler * _connectionHandler;
    short nextBlock; //next Blcok to send
    string FileToSendName;
    int RemainingBytesToSend;
    string FileToSaveName;
    bool DIRQING;
	bool shouldTerminate;
	bool disconnecting;
	long dirqSavememSize;
    long savememSize;
    char * dirqSavemem;
    char * savemem; //mem of downloaded file
    char * memblock; //mem of send file
	string filenameToPrint;
	string filenameToPrint2;
	bool logged;
	bool triedlog;


public:
    Task(int id, boost::mutex* mutex,ConnectionHandler* connectionHandler);
    Task(const Task&);
    Task& operator=(const Task&);
    void run1();
    void run2();
    short bytesToShort(char* bytesArr);
    short getSecondShort(char* bytesArr);
    short getThirdShort(char* bytesArr);
    void sendFirstBlock();
    void sendNextBlock();
    void shortToBytes(short num, char* bytesArr);
    void putSizeBytes(short num, char* bytesArr);
    void putBlockBytes(short num, char* bytesArr);
    void saveNextPacket(char* bytes);
    void saveNextDirqPack(char* bytes);
};
#endif
