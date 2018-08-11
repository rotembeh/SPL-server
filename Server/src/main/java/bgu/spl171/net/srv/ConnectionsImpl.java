package bgu.spl171.net.srv;

import java.util.concurrent.ConcurrentHashMap;

import bgu.spl171.net.api.bidi.Connections;

public class ConnectionsImpl<T> implements Connections<T> {
	private ConcurrentHashMap<Integer,ConnectionHandler<T>> map;

	public ConnectionsImpl(){
		map=new ConcurrentHashMap<Integer,ConnectionHandler<T>>();
	}

	@Override
	public boolean send(int connectionId, T msg) {
		map.get(connectionId).send(msg);
		return true;
	}

	@Override
	public void broadcast(T msg) {
		map.forEach((id,connection) -> send(id,msg));	
	}

	@Override
	public void disconnect(int connectionId) {
		map.remove(connectionId);
	}
	
	public ConcurrentHashMap<Integer,ConnectionHandler<T>> getMap(){
		return map;
	}
	
	public void add(Integer conId, ConnectionHandler<T> handler) {
		map.put(conId, handler);
	}
    

}