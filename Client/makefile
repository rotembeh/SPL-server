CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lboost_locale -lboost_thread

all: hello
	g++ -o bin/TFTPclient bin/connectionHandler.o bin/Client.o bin/Task.o $(LDFLAGS) 

hello: bin/connectionHandler.o bin/Client.o bin/Task.o
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp

bin/Client.o: src/Client.cpp
	g++ $(CFLAGS) -o bin/Client.o src/Client.cpp
	
bin/Task.o: src/Task.cpp
	g++ $(CFLAGS) -o bin/Task.o src/Task.cpp

.PHONY: clean
clean:
	rm -f bin/*
