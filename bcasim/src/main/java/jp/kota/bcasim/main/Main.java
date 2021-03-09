package jp.kota.bcasim.main;


import jp.kota.bcasim.main.node.*;
import jp.kota.bcasim.network.Network;
import jp.kota.bcasim.network.NetworkGenerator;
import jp.kota.bcasim.tool.fileio.OutputResult;


public class Main {
	
	

	public static void main(String[] args) {
		
		
		long start_point = System.currentTimeMillis();
		
		//ディレクトリ設定
		OutputResult.deleteDirectory();
		OutputResult.createDirectory();
		OutputResult.startProcess();
		
		//ネットワーク, ノード設定
		NetworkGenerator.inputNetworkConfig();
		NetworkGenerator.inputNodeConfig();
		
		
		
		//シミュレーション実行
		Scheduler.InitEventList();
		Scheduler.processEvent();
		
		
		//結果出力
		OutputResult.outBlockCsvBreadth(Network.getNodeList().get(0).getBlockchain());
		OutputResult.outBlockJsonBreadth(Network.getNodeList().get(0).getBlockchain());
		
		
		OutputResult.OutSetting();
		OutputResult.OutAdjacencyMatrix();
		OutputResult.outputMainchain(Network.getNodeList().get(0).getBlockchain());
		OutputResult.endProcess();
		
		long end_point = System.currentTimeMillis();
		long time = end_point - start_point;
		//System.out.println("処理開始前の時刻" + start_point);
	    //System.out.println("処理終了時の時刻" + end_point);
		System.out.println("****************:");
	    System.out.println("processing time" + time);
	    
	    //Attacker3 attackNode = (Attacker3)Network.getNodeList().get(0);
	    //attackNode.print_rate();
	    
	}

}
