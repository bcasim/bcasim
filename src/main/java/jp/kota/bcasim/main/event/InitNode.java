package jp.kota.bcasim.main.event;



import jp.kota.bcasim.main.node.Node;


public class InitNode extends Event{
	
	
	public InitNode(double eventTime,Node node) {
		super(eventTime,node);
	}
	
	
	public void process() {
		super.node.initNode(this);
	}
	

}
