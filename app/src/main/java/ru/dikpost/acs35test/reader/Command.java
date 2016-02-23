package ru.dikpost.acs35test.reader;

public class Command {

	int type;
	int block;
	byte[] data;
	
	public Command( int type, int block ){
		this.type = type;
		this.block = block;
		this.data = null;
	}
	
	public Command(){}
	
	public void setParams(int type, int block ){
		this.type = type;
		this.block = block;
		
	}
	
	public int getType(){
		return type;
	}
	
	public byte[] getData(){
		return data;
	}
	public int getBlock(){
		return block;
	}
}
