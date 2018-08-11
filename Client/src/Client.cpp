#include <stdlib.h>
#include <connectionHandler.h>
#include <boost/asio.hpp>
#include <boost/thread.hpp>
#include <Task.h>


/**
* This code assumes that the server replies the exact text the client sent it (as opposed to the practical session example)
*/
int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }
    boost::mutex mutex;
    Task task(1, &mutex, &connectionHandler);
    boost::thread th1(&Task::run1, &task);
    boost::thread th2(&Task::run2, &task);
    th1.join();
    th2.join();
    connectionHandler.close();
    return 0;
}
