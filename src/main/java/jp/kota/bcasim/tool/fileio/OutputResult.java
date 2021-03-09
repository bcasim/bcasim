package jp.kota.bcasim.tool.fileio;

import jp.kota.bcasim.configuration.Configuration;
import jp.kota.bcasim.datastructure.Block;
import jp.kota.bcasim.datastructure.Blockchain;
import jp.kota.bcasim.datastructure.Genesis;
import jp.kota.bcasim.datastructure.Transaction;
import jp.kota.bcasim.main.event.Event;
import jp.kota.bcasim.main.event.FoundBlock;
import jp.kota.bcasim.main.event.ReceiveBlock;
import jp.kota.bcasim.main.event.ReceiveTransaction;
import jp.kota.bcasim.main.event.SendTransaction;
import jp.kota.bcasim.main.node.Node;
import jp.kota.bcasim.network.Network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;


public class OutputResult {
	
	
	private static String DIRECTORY_NAME = "outputFile";
	
	private static final String FILE_NAME1 = "blockchain.json";
	private static final String FILE_NAME2 = "blockchain.csv";
	private static final String FILE_NAME3 = "eventLog.txt";
	private static final String FILE_NAME4 = "event.json";
	private static final String FILE_NAME5 = "configuration.txt";
	private static final String FILE_NAME6 = "adjacencyMatrix.csv";
	private static final String FILE_NAME7 = "attackLog.txt";
	
	
	private static final String NEW_LINE= "\r\n";
	private static final String COMMA = ",";
	
	
	//はじめの処理
	public static void startProcess() {
		FileWriter fw = null;
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME4);
			fw = new FileWriter(file, true);
			fw.append("[");
			fw.close();
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + "block.json");
			fw = new FileWriter(file, true);
			fw.append("[");
			fw.close();
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		OutputResult.outBlockJson(Genesis.getGenesis());
		
	}
	
	public static void endProcess() {
		
		FileWriter fw = null;
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME4);
			fw = new FileWriter(file, true);
			fw.append("]");
			fw.close();
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + "block.json");
			fw = new FileWriter(file, true);
			fw.append("]");
			fw.close();
		} catch (IOException e) {
            e.printStackTrace();
        }
		
	}
	
	//******************************************************************
	/**
	 * シミュレーション結果出力用のディレクトリ作成
	 */
	public static void createDirectory() {
		File newDirectory = new File(OutputResult.DIRECTORY_NAME);
		int directoryNumber = 0;
		while(true) {
			if (newDirectory.exists()){
				directoryNumber++;
				newDirectory = new File(OutputResult.DIRECTORY_NAME + directoryNumber);
				continue;
			}
			if (newDirectory.mkdir()){
				if(directoryNumber != 0) {
					DIRECTORY_NAME += directoryNumber;
				}
			}
			break;
		}
	}
	
	
	//1ブロックずつ出力
	static boolean firstBlock = false;
	public static void outBlockJson(Block block) {
		FileWriter fw = null;
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + "block.json");
			fw = new FileWriter(file, true);
			
			if(firstBlock) {
				fw.append(",");
				fw.append(NEW_LINE);
			}
			
			fw.append("{");
			fw.append(NEW_LINE);
			fw.append("  \"hash\":\""+block.getHash()+"\",");
			fw.append(NEW_LINE);
			fw.append("  \"previousHash\":\""+ block.getPreviousHash() +"\",");
			fw.append(NEW_LINE);
			fw.append("  \"timestamp\":\""+ block.getTimestamp() +"\",");
			fw.append(NEW_LINE);
			fw.append("  \"receiveTime\":\""+ block.getReceiveBlockTime() +"\",");
			fw.append(NEW_LINE);
			fw.append("  \"height\":\""+ block.getHeight() + "\",");
			fw.append(NEW_LINE);
			fw.append("  \"miner\":\""+ block.getMiner().getName() +"\",");
			fw.append(NEW_LINE);
			fw.append("  \"reward\":\""+ Configuration.BLOCK_REWARD +"\",");
			fw.append(NEW_LINE);
			
			String strTransaction = "[";
			for(int i=0;i<block.getTransactionList().size();i++) {
				strTransaction += "\"";
				strTransaction += block.getTransactionList().get(i).getTransactionHash();
				strTransaction += "\"";
				if(i<block.getTransactionList().size()) {
					strTransaction += ",";
				}
			}
			strTransaction += "]";

			fw.append("  \"transaction\":"+ strTransaction);
			fw.append(NEW_LINE);
			fw.append("}");
			
			fw.close();
			firstBlock = true;
			
		} catch (IOException e) {
            e.printStackTrace();
        }
		
	}
	
	//******************************************************************
	/**
	 * json形式でブロックチェーンを出力
	 * 
	 */
	public static void outBlockJson(Blockchain blockchain) {
		FileWriter fw;
		try {
            fw = new FileWriter("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME1);
            ArrayList<Block> stack = new ArrayList<Block>();
			stack.add(blockchain.getGenesis());
			fw.append("[");
			while(stack.size()!=0) {
				Block block = stack.get(stack.size()-1);
				stack.remove(stack.size()-1);
				
				fw.append("{");
				fw.append(NEW_LINE);
				fw.append("  \"hash\":\""+block.getHash()+"\",");
				fw.append(NEW_LINE);
				fw.append("  \"previousHash\":\""+ block.getPreviousHash() +"\",");
				fw.append(NEW_LINE);
				fw.append("  \"timestamp\":\""+ block.getTimestamp() +"\",");
				fw.append(NEW_LINE);
				fw.append("  \"height\":\""+ block.getHeight() +"\",");
				fw.append(NEW_LINE);
				fw.append("  \"miner\":\""+ block.getMiner().getName() +"\"");
				fw.append(NEW_LINE);
				fw.append("}");
				for(Block NB : block.getNextBlocks()) {
					stack.add(NB);
				}
				if(stack.size()!=0) {
					fw.append(COMMA);
					fw.append(NEW_LINE);
				}
			}
			fw.append("]");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	//******************************************************************
	/**
	 * csv形式でブロックチェーン出力
	 */
	public static void outBlockCsv(Blockchain blockchain) {
		FileWriter fw;
		try {
            fw = new FileWriter("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME2);
            ArrayList<Block> stack = new ArrayList<Block>();
			stack.add(blockchain.getGenesis());
			while(stack.size()!=0) {
				Block block = stack.get(stack.size()-1);
				stack.remove(stack.size()-1);
				fw.append(block.getHash());
				fw.append(COMMA);
				fw.append(block.getPreviousHash());	
				fw.append(COMMA);
				fw.append(String.valueOf(block.getTimestamp()));
				fw.append(COMMA);
				fw.append(String.valueOf(block.getHeight()));
				fw.append(COMMA);
				fw.append(block.getMiner().getName());
				fw.append(NEW_LINE);
				
				for(Block NB : block.getNextBlocks()) {
					stack.add(NB);
				}
			}
			fw.close();
		} catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	
	//******************************************************************
	/**
	 * イベントログの出力
	 */
	public static void outEvent(Event event) {
		FileWriter fw = null;
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME3);
			fw = new FileWriter(file, true);
			
			fw.append("--------");
			fw.append(NEW_LINE);
			fw.append("eventID : " + event.getEventID());
			fw.append(NEW_LINE);
			fw.append("time : " + event.getEventTime());
			fw.append(NEW_LINE);
			fw.append("type : " + event.getEventType());
			fw.append(NEW_LINE);
			fw.append("node : " + event.getNode().getName());
			fw.append(NEW_LINE);
				
			if(event.getEventType().equals("ArriveBlock")) {
				ReceiveBlock arriveBlock = (ReceiveBlock)event;
				fw.append("height : " + arriveBlock.getBlock().getHeight());
				fw.append(NEW_LINE);
				fw.append("Miner : " + arriveBlock.getBlock().getMiner().getName());
				fw.append(NEW_LINE);
					
			}else if(event.getEventType().equals("publishBlock")) {
				//PublishBlock publishBlock = (PublishBlock)event;
				//fw.append("height : " + publishBlock.getBlock().getHeight());
				//fw.append(NEW_LINE);
				//fw.append("Miner : " + publishBlock.getBlock().getMiner().getName());
				//fw.append(NEW_LINE);
				
			}else if(event.getEventType().equals("FoundBlock")) {
				FoundBlock foundBlock = (FoundBlock)event;
				fw.append("height : " + foundBlock.getBlock().getHeight());
				fw.append(NEW_LINE);
				fw.append("Miner : " + foundBlock.getBlock().getMiner().getName());
				fw.append(NEW_LINE);
			}else if(event.getEventType().equals("PropagateBlock")) {
				//PropagateBlock propagateBlock = (PropagateBlock)event;
				//fw.append("height : " + propagateBlock.getBlock().getHeight());
				fw.append(NEW_LINE);
				//fw.append("Miner : " + propagateBlock.getBlock().getMiner().getName());
				fw.append(NEW_LINE);
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//******************************************************************
	/**
	 * 設定ファイルを出力
	 *  
	 */
	public static void OutSetting() {
		FileWriter fw;
		try {
			fw = new FileWriter("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME5);
			fw.append("simulation configuration");
			fw.append(NEW_LINE);
			fw.append("SIMULATION_TIME:"+Configuration.SIMULATION_TIME);
			fw.append(NEW_LINE);
			fw.append("BLOCK_INTERVAL:"+Configuration.BLOCK_INTERVAL);
			fw.append(NEW_LINE);
			fw.append("BLOCK_SIZE:"+Configuration.BLOCK_SIZE);
			fw.append(NEW_LINE);
			fw.append("BLOCK_REWARD:"+Configuration.BLOCK_REWARD);
			fw.append(NEW_LINE);
			fw.append("BLOCK_DELAY:"+Configuration.BLOCK_DELAY);
			fw.append(NEW_LINE);
			fw.append("TRANSACTION_DELAY:"+Configuration.TRANSACTION_DELAY);
			fw.append(NEW_LINE);
			fw.append("NUMBER_OF_NODES:"+Configuration.NUMBER_OF_NODES);
		
			fw.append(NEW_LINE);
			fw.append("HASHRATE_LIST:{");
			for(int i=0;i<Configuration.NUMBER_OF_NODES;i++) {
				fw.append(String.valueOf(Network.getNodeList().get(i).getHashrate()));
				if(Configuration.NUMBER_OF_NODES-1!=i) {
					fw.append(",");
				}
			}
			fw.append("}"+NEW_LINE);
			
			
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	//******************************************************************
	
	/**
	* ノードの隣接行列を出力
	*/
	public static void OutAdjacencyMatrix() {
		int adjacencyMatrix[][] = Network.getAdjacencyMatrix();
		
		FileWriter fw;
		try {
			fw = new FileWriter("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME6);
			for(int i=0;i<adjacencyMatrix.length;i++) {
				for(int j=0;j<adjacencyMatrix.length;j++) {
					String strMatrix = String.valueOf(adjacencyMatrix[i][j]);
					fw.append(strMatrix);
					if(adjacencyMatrix[i].length-1!=j) {
						fw.append(COMMA);
					}
				}
				
				if(adjacencyMatrix.length-1 != i) {
					fw.append(NEW_LINE);
				}
				
			}
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	//******************************************************************
		/**
		 * 幅優先探索json形式でブロックチェーンを出力
		 *  
		 */
		public static void outBlockJsonBreadth(Blockchain blockchain) {
			FileWriter fw;
			try {
				fw = new FileWriter("./" + OutputResult.DIRECTORY_NAME + "/" + blockchain.getChainID() + "_" + OutputResult.FILE_NAME1);
		        ArrayList<Block> stack = new ArrayList<Block>();
		        stack.add(blockchain.getGenesis());
		        fw.append("[");
				while(stack.size()!=0) {
					Block block = stack.get(0);
					stack.remove(0);
					
					fw.append("{");
					fw.append(NEW_LINE);
					fw.append("  \"hash\":\""+block.getHash()+"\",");
					fw.append(NEW_LINE);
					fw.append("  \"previousHash\":\""+ block.getPreviousHash() +"\",");
					fw.append(NEW_LINE);
					fw.append("  \"timestamp\":\""+ block.getTimestamp() +"\",");
					fw.append(NEW_LINE);
					fw.append("  \"receiveTime\":\""+ block.getReceiveBlockTime() +"\",");
					fw.append(NEW_LINE);
					fw.append("  \"height\":\""+ block.getHeight() + "\",");
					fw.append(NEW_LINE);
					fw.append("  \"miner\":\""+ block.getMiner().getName() +"\",");
					fw.append(NEW_LINE);
					fw.append("  \"reward\":\""+ Configuration.BLOCK_REWARD +"\",");
					fw.append(NEW_LINE);
					
					String strTransaction = "[";
					for(int i=0;i<block.getTransactionList().size();i++) {
						strTransaction += "\"";
						strTransaction += block.getTransactionList().get(i).getTransactionHash();
						strTransaction += "\"";
						if(i<block.getTransactionList().size()) {
							strTransaction += ",";
						}
					}
					strTransaction += "]";
		
					fw.append("  \"transaction\":"+ strTransaction);
					fw.append(NEW_LINE);
					fw.append("}");
					for(Block NB : block.getNextBlocks()) {
						stack.add(NB);
					}
					if(stack.size()!=0) {
						fw.append(COMMA);
						fw.append(NEW_LINE);
					}
				}
				fw.append("]");
		        fw.close();
		    } catch (IOException e) {
		            e.printStackTrace();
		    }
		}
	
	//******************************************************************
		
	/**
	 * 幅優先探索でcsvブロックを出力
	 */
	public static void outBlockCsvBreadth(Blockchain blockchain) {
		FileWriter fw;
		try {
			fw = new FileWriter("./" + OutputResult.DIRECTORY_NAME + "/" + blockchain.getChainID() + "_" + OutputResult.FILE_NAME2);
			ArrayList<Block> stack = new ArrayList<Block>();
			stack.add(blockchain.getGenesis());
			
			while(stack.size()!=0) {
				Block block = stack.get(0);
				stack.remove(0);
				fw.append(block.getHash());
				fw.append(COMMA);
				fw.append(block.getPreviousHash());	
				fw.append(COMMA);
				fw.append(String.valueOf(block.getTimestamp()));
				fw.append(COMMA);
				fw.append(String.valueOf(block.getHeight()));
				fw.append(COMMA);
				fw.append(block.getMiner().getName());
				fw.append(NEW_LINE);
						
				for(Block NB : block.getNextBlocks()) {
					stack.add(NB);
				}
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
      
	}
		
	
	
	//******************************************************************
	/**
	 * イベントログの出力
	 */
	static boolean first_event = false;
	public static void outEventJson(Event event) {
		FileWriter fw = null;
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME4);
			fw = new FileWriter(file, true);
			
			if(event.getEventType().equals("ReceiveBlock")) {
				ReceiveBlock receiveBlock = (ReceiveBlock)event;
				if(receiveBlock.getFrom()==receiveBlock.getNode()) {
					fw.close();	
					return;
				}
			}
			
			if(first_event) {
				fw.append(",");
				fw.append(NEW_LINE);
			}
			
			fw.append("{");
			fw.append(NEW_LINE);
			fw.append("  \"eventID\":\"" + event.getEventID() + "\",");
			fw.append(NEW_LINE);
			fw.append("  \"time\":\"" + event.getEventTime() + "\",");
			fw.append(NEW_LINE);
			fw.append("  \"type\":\"" + event.getEventType() + "\",");
			fw.append(NEW_LINE);
			fw.append("  \"node\":\"" + event.getNode().getName() + "\"");
			
						
			if(event.getEventType().equals("ReceiveBlock")) {
				ReceiveBlock receiveBlock = (ReceiveBlock)event;
				fw.append(",");
				fw.append(NEW_LINE);
				fw.append("  \"height\":\"" + receiveBlock.getBlock().getHeight() + "\",");
				fw.append(NEW_LINE);
				fw.append("  \"miner\":\"" + receiveBlock.getBlock().getMiner().getName() + "\",");
				fw.append(NEW_LINE);
				fw.append("  \"hash\":\"" + receiveBlock.getBlock().getHash() + "\",");
				fw.append(NEW_LINE);
				fw.append("  \"from\":\"" + receiveBlock.getFrom().getName() + "\"");
				fw.append(NEW_LINE);
				
										
			}else if(event.getEventType().equals("FoundBlock")) {
				FoundBlock foundBlock = (FoundBlock)event;
				fw.append(",");
				fw.append(NEW_LINE);
				fw.append("  \"height\":\"" + foundBlock.getBlock().getHeight() + "\",");
				fw.append(NEW_LINE);
				fw.append("  \"miner\":\"" + foundBlock.getBlock().getMiner().getName() + "\",");
				fw.append(NEW_LINE);
				fw.append("  \"hash\":\"" + foundBlock.getBlock().getHash() + "\"");
				fw.append(NEW_LINE);
				
			}else if(event.getEventType().equals("ReceiveTransaction")) {
				ReceiveTransaction receiveTransaction = (ReceiveTransaction)event;
				fw.append(",");
				fw.append(NEW_LINE);
				fw.append("  \"hash\":\"" + receiveTransaction.getTransaction().getTransactionHash() + "\",");
				fw.append(NEW_LINE);
				fw.append("  \"from\":\"" + receiveTransaction.getFrom().getName() + "\"");
				fw.append(NEW_LINE);
			}else {
				fw.append(NEW_LINE);
			}
			
			fw.append("}");		
			fw.close();	
			first_event = true;
						
		} catch (IOException e) {
				e.printStackTrace();
		}
	}
	
	public static void outEventJson(Node node,int height,String eventType,double time,Block block) {
		FileWriter fw = null;
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME4);
			fw = new FileWriter(file, true);
			
			if(first_event) {
				fw.append(",");
				fw.append(NEW_LINE);
			}
			fw.append("{");
			fw.append(NEW_LINE);
			fw.append("  \"eventID\":\"" + -1 +"\",");
			fw.append(NEW_LINE);
			fw.append("  \"time\":\"" + time +"\",");
			fw.append(NEW_LINE);
			fw.append("  \"type\":\"" + eventType +"\",");
			fw.append(NEW_LINE);
			fw.append("  \"node\":\"" + node.getName()+"\",");
			fw.append(NEW_LINE);
			fw.append("  \"hash\":\"" + block.getHash()+ "\"");
			fw.append("}");
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
	}
	}
	
	
	//******************************************************************
	/**
	 * 結果出力用のディレクトリ削除
	 */
	public static void deleteDirectory() {
		try {
            delete(DIRECTORY_NAME);
        }catch(Throwable th) {
            th.printStackTrace();
        }
	}
	
	public static void delete(String path) {
        File filePath = new File(path);
        String[] list = filePath.list();
        for(String file : list) {
            File f = new File(path + File.separator + file);
            if(f.isDirectory()) {
                delete(path + File.separator + file);
            }else {
                f.delete();
            }
        }
        filePath.delete();
    }
	
	//******************************************************************
	//******************************************************************
	/**
	 * イベントログをjsonで出力
	 */
	public static void outEventLongJson(Event event) {
		FileWriter fw = null;
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME4);
			
			fw = new FileWriter(file, true);
			
			fw.append("{");
			fw.append(NEW_LINE);
			fw.append("  \"eventID\":\"" + event.getEventID()+"\",");
			fw.append(NEW_LINE);
			fw.append("  \"time\":\"" + event.getEventTime()+"\",");
			fw.append(NEW_LINE);
			fw.append("  \"type\":\"" + event.getEventType()+"\",");
			fw.append(NEW_LINE);
			fw.append("  \"node\":\"" + event.getNode().getName()+"\",");
			fw.append(NEW_LINE);
			Block block = null;
			Transaction transaction = null;
			
			switch (event.getEventType()) {
			
			case "StartMining":
				break;
				
			case "StopMining":
				break;
			
			case "ReceiveBlock":
				ReceiveBlock arriveBlock = (ReceiveBlock)event;
				block = arriveBlock.getBlock();
				break;
				
			case "FoundBlock":
				FoundBlock foundBlock = (FoundBlock)event;
				block = foundBlock.getBlock();
				break;
				
			case "PropageteBlock":
				//PropagateBlock propagateBlock = (PropagateBlock)event;
				//block = propagateBlock.getBlock();
				break;
			case "PublishBlock":
				//PublishBlock publishBlock = (PublishBlock)event;
				//block = publishBlock.getBlock();
				break;
				
			case "SendTransaction":
				SendTransaction sendTransaction = (SendTransaction)event;
				transaction = sendTransaction.getTransaction();
				break;
				
			case "PropagateTransaction":
				//PropagateTransaction propagateTransaction = (PropagateTransaction)event;
				//transaction = propagateTransaction.getTransaction();
				//break;
				
			case "ReceiveTransaction":
				ReceiveTransaction arriveTransaction = (ReceiveTransaction)event;
				transaction = arriveTransaction.getTransaction();
				break;
			}
			
			if(block != null) {
				fw.append("  \"block\":\"{\"");
				fw.append("    \"height\":" + block.getHeight()+",");
				fw.append(NEW_LINE);
				fw.append("    \"miner\":\"" + block.getHeight()+"\",");
				fw.append(NEW_LINE);
				fw.append("    \"hash\":\"" + block.getHash()+"\"");
				fw.append(NEW_LINE);
				fw.append("  }");
				
			}
			if(transaction != null) {
				fw.append("  \"transaction\":\"{\"");
				fw.append("    \"from\":\"" + transaction.getFrom()+"\",");
				fw.append(NEW_LINE);
				fw.append("    \"to\":\"" + transaction.getTo()+"\",");
				fw.append(NEW_LINE);
				fw.append("    \"value\":\"" + transaction.getValue()+"\"");
				fw.append(NEW_LINE);
				fw.append("  }");
			}
			
			fw.append("},");
			fw.append(NEW_LINE);
			fw.close();				
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	
	
	//******************************************************************
	/**
	 * 攻撃ログを出力
	 */
	public static void outAttackLog(String text) {
		FileWriter fw = null;
		try {
			File file = new File("./" + OutputResult.DIRECTORY_NAME + "/" + OutputResult.FILE_NAME7);
			fw = new FileWriter(file, true);
			fw.append(text);
			fw.append(NEW_LINE);
			
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	
	
	
	
	//******************************************************************
		//--------分析----------
		public static void outputMainchain(Blockchain blockchain) {
			Block endBlock = blockchain.getLatestBlock();
			int nodeA = 0;
			int nodeB = 0;
			int nodeC = 0;
			int nodeD = 0;
			int nodeE = 0;
			while(true) {
				
				if(endBlock.getMiner().getName().equals("0")) {
					
					nodeA++;
				}else if(endBlock.getMiner().getName().equals("1")) {
					nodeB++;
				}else if(endBlock.getMiner().getName().equals("2")) {
					nodeC++;
				}else if(endBlock.getMiner().getName().equals("3")) {
					nodeD++;
				}else if(endBlock.getMiner().getName().equals("4")) {
					nodeE++;
				}
				
				
				endBlock = endBlock.getPreviousBlock();
				if(endBlock == null) {
					break;
				}
			}
			FileWriter fileWriter = null;
			try {
				fileWriter = new FileWriter("./"+DIRECTORY_NAME+"/"+"Mainchain.txt");
				fileWriter.append("node0: ");
				fileWriter.append(String.valueOf(nodeA));
				fileWriter.append(NEW_LINE);
				fileWriter.append("node1: ");
				fileWriter.append(String.valueOf(nodeB));
				fileWriter.append(NEW_LINE);
				fileWriter.append("node2: ");
				fileWriter.append(String.valueOf(nodeC));
				fileWriter.append(NEW_LINE);
				fileWriter.append("node3: ");
				fileWriter.append(String.valueOf(nodeD));
				fileWriter.append(NEW_LINE);
				fileWriter.append("node4: ");
				fileWriter.append(String.valueOf(nodeE));
				fileWriter.append(NEW_LINE);

				fileWriter.append("合計: ");
				fileWriter.append(String.valueOf(nodeA + nodeB + nodeC + nodeD + nodeE));
				
			} catch (Exception e) {
				
			}finally {
				try {
			        fileWriter.flush();
			        fileWriter.close();
			      } catch (IOException e) {
			        e.printStackTrace();
			      }
				
			}
		}
	
	
	
	
	
}
