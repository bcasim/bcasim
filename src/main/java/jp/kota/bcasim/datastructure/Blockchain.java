package jp.kota.bcasim.datastructure;

import java.util.ArrayList;
import java.util.Random;

import jp.kota.bcasim.main.Scheduler;
import jp.kota.bcasim.main.node.Node;



public class Blockchain {
	
	
	
	private int topHeight = 0;
	private Block topBlock;
	
	private Block genesisBlock;
	private String chainID;
	private BlockCache blockCache;
	
	private Node node;
	
	public Blockchain(String chainID,Node node) {
		this.genesisBlock = Genesis.getGenesis();
		this.chainID = chainID;
		this.node = node;
		this.blockCache = new BlockCache();
		this.topBlock = this.genesisBlock;
	}
	
	
	public void addBlock(Block clone_block) {
		
		Block block = Block.cloneBlock(clone_block);
		
		//---------------速度が落ちるため削除--------------
		//チャーンに同じブロックのハッシュがないか確認
		/*Block targetBlock = serchBlockHash(block.getHash());
		if(targetBlock != null) {
			System.out.println("同じハッシュ");
			System.exit(0);
			return;
		}
		//System.out.println("違うハッシュ");*/
		//------------------------------------------------
		
		//同じハッシュが存在するかをチェックする
		//if(!blockHashSet.containsKey(block.getHash())) {
		//	return;
		//}
		//親ブロックが存在するかをチェックする
		//if(blockHashSet.containsKey(block.getPreviousHash())) {
		//	return;
		//}
		//mapにブロックを追加
		//this.blockHashSet.put(block.getHash(),block);
		//Block targetBlock = this.blockHashSet.get(block.getPreviousHash());
		
		
		//すでに登録済みブロック--------
		if(block.verifyNode(this.node)) {
			return;
		}
		
		//正しいブロックであるかを確認
		Block targetBlock = this.serchBlockHash(block.getPreviousHash());
		
		if(targetBlock == null) {
			System.out.println("Err targetBlock == null");
			System.exit(0);
			return;
		}
		
		if(targetBlock.getHeight()+1 != block.getHeight()) {
			System.out.println("Err targetBlock.getHeight()+1 != block.getHeight()");
			System.exit(0);
			return;
		}
		
		
		//ブロックの到着時刻を追加
		block.setReceiveBlockTime(Scheduler.getSimulationTime());
		
		
		//正しい場合は登録
		targetBlock.addNextBlock(block);
		block.setPreviousBlock(targetBlock);
		this.blockCache.addBlock(block);
		
		//伝送済みノードに追加
		block.addTransmittedNodes(this.node);
		
		//最大ブロック高の場合は登録しておく
		if(this.topHeight < block.getHeight()) {
			this.topHeight = block.getHeight();
			this.topBlock = block;
			
		//伝送係数　ブロックの長さが同じ場合どちらのブロックを生成するかを選択
		}else if(this.topHeight == block.getHeight() && 
				chainID.equals("1") &&
				!(block.getMiner().getName().equals(chainID))) {
			
			Random rand = new Random();
			if(rand.nextInt(10)<0) {// γ
				this.topBlock = block;
			}
			
			
		//同じブロック高の場合は自分のブロックを優先
		}else if(this.topHeight == block.getHeight() && 
				chainID.equals(block.getMiner().getName())) 
		{
			this.topBlock = block;
		}
		
	}
	
	
	
	/**
	 * 高速化したがバグが発生
	 */
	public Block getLatestBlock() {
		return this.topBlock;
	}

	public Block getLatestBlock1() {
		int maximum = -1;
		Block block = null;
		ArrayList<Block> stack = new ArrayList<Block>();
		stack.add(this.genesisBlock);
		
		while(stack.size()!=0) {
			
			Block nextBlock = stack.get(stack.size()-1);
			stack.remove(stack.size()-1);
			
			if(nextBlock.getHeight() > maximum) {
				block = nextBlock;
				maximum = nextBlock.getHeight();
			}
			
			
			for(Block NB : nextBlock.getNextBlocks()) {
				stack.add(NB);
			}
		}
			
		return block;
		
	}
	
	
	
	/**
	 * 指定されたハッシュのブロックがあるか確認
	 */
	public Block serchBlockHash(String hash) {
		Block block = this.serchCacheHash(hash);
		if(block != null) {
			return block;
		}
		
		ArrayList<Block> stack = new ArrayList<Block>();
		stack.add(this.genesisBlock);
		while(stack.size()!=0) {
			Block nextBlock = stack.get(stack.size()-1);
			stack.remove(stack.size()-1);			
			
			if(nextBlock.getHash().equals(hash)) {
				return nextBlock;
			}
			for(Block NB : nextBlock.getNextBlocks()) {
				stack.add(NB);
			}
		}
		return null;
	}
	
	
	/**
	 * ブロック高を返す
	 * 深さ優先探索
	 */
	public int getHeight() {
		int maximum = -1;
		ArrayList<Block> stack = new ArrayList<Block>();
		stack.add(genesisBlock);
		
		while(stack.size()!=0) {
			
			Block nextBlock = stack.get(stack.size()-1);
			stack.remove(stack.size()-1);
			
			if(nextBlock.getHeight() > maximum) {
				maximum = nextBlock.getHeight();
			}
			
			for(Block NB : nextBlock.getNextBlocks()) {
				stack.add(NB);
			}
		}
		return maximum;
	}
	
	public String getChainID() {
		return this.chainID;
	}
	
	
	public Block getGenesis() {
		return this.genesisBlock;
	}
	
	
	
	//--------高速化のためキャッシュを探索--------
	
	public Block serchCacheHash(String hash) {
		return this.blockCache.serchHash(hash);
	}
	
	public Block serchCacheHeight(int Height) {
		return this.blockCache.serchHeight(Height);
	}
	
	
	
	
}
