import java.util.*;

public class Player {
    /**
     * Performs a move
     *
     * @param pState
     *            the current state of the board
     * @param pDue
     *            time before which we must have returned
     * @return the next state the board is in after our move
     */
	
	int player;
	int opponent;
	int MAXDEPTH;
	int nextMove=0;
	int nextPartialMove=0;
	int nodesVisited;
	int nodesToVisit;
	int expectedNumberOfNodes;
	boolean breakIteration;
	
	HashMap<Long, State> zobrist = new HashMap<>();
	long[][] piece = new long[32][4];
	public class State {
		int vmin;
		int vmax;
		int depth;
		long key;
		public State(int depth, int valueMin, int valueMax, long key) {
			this.depth = depth;
			this.vmin = valueMin;
			this.vmax = valueMax;
			this.key = key;
		}
	}
	
	int enemyKilledw = 8;
	int enemyKilledr = 6;
	
	
    public GameState play(final GameState pState, final Deadline pDue) {

        Vector<GameState> lNextStates = new Vector<GameState>();
        pState.findPossibleMoves(lNextStates);

        if (lNextStates.size() == 0) {
            // Must play "pass" move if there are no other moves possible.
            return new GameState(pState, new Move());
        }

        player = pState.getNextPlayer();
        if(player == Constants.CELL_RED)
        	opponent = Constants.CELL_WHITE;
        else
        	opponent = Constants.CELL_RED;
        
        Random rand = new Random();
        for(int i=0; i<GameState.NUMBER_OF_SQUARES; i++) {
            for(int j=0; j<4; j++) {
                piece[i][j] = rand.nextLong();
            }
        }
        
        long startTime;
        long finishTime;
        boolean haveTime = true;
        MAXDEPTH = 10;
        
        expectedNumberOfNodes = 0;
        breakIteration = false;
        
        //while(haveTime && MAXDEPTH < 50) {
        	startTime = pDue.timeUntil();
        	nodesVisited = 0;
            nodesToVisit = 0;
            
            //zobrist = new HashMap<>();
            
        	alphaBetaPruning(MAXDEPTH, pState, player, Integer.MIN_VALUE, Integer.MAX_VALUE, pDue);
        	//if(breakIteration)
        	//	break;
        	nextMove = nextPartialMove;
        	expectedNumberOfNodes = nodesToVisit;
        	finishTime = pDue.timeUntil();
        	if(finishTime < (startTime-finishTime)*((double) nodesToVisit/nodesVisited))
        		haveTime = false;
        	if(nodesToVisit == nodesVisited)
        		haveTime = false;
        	//System.err.println("depth: " + MAXDEPTH);
        	MAXDEPTH++;
        	
        //}
        
        return lNextStates.get(nextMove);
        
        /**
         * Here you should write your algorithms to get the best next move, i.e.
         * the best next state. This skeleton returns a random move instead.
         */
        //Random random = new Random();
        //return lNextStates.elementAt(random.nextInt(lNextStates.size()));
    }
    
    public int alphaBetaPruning(int depth, GameState currentState, int player, int alpha, int beta, Deadline deadline) {
    	
    	nodesVisited++;
    	nodesToVisit++;
    	
    	long key = createKey(currentState);
    	if(zobrist.containsKey(key) && zobrist.get(key).key == key) {
    		if(player == this.player)
    			return zobrist.get(key).vmax;
    		return zobrist.get(key).vmin;
    	}
    		
    	
    	if(deadline.timeUntil() < 1e3) { // || deadline.timeUntil() < 1e6) {
    		breakIteration = true;
    		return 0;
    	}
    	
    	
    	if(0 != (this.player & Constants.CELL_RED)) {
	    	if(currentState.isWhiteWin()) {
	    		return Integer.MIN_VALUE;
	    	} else if(currentState.isRedWin()) {
	    		return Integer.MAX_VALUE;
	    	} else if(currentState.isEOG() || currentState.isDraw()) {
	    		return -1;
	    	}
    	} else {
    		if(currentState.isWhiteWin()) {
	    		return Integer.MAX_VALUE;
	    	} else if(currentState.isRedWin()) {
	    		return Integer.MIN_VALUE;
	    	} else if(currentState.isEOG() || currentState.isDraw()) {
	    		return -1;
	    	}
    	}
    	
    	int i;
    	Vector<GameState> nextStates = new Vector<GameState>();
    	currentState.findPossibleMoves(nextStates);
    	
    	if(depth == 0) {
    		nodesToVisit += nextStates.size();
    		int value = heuristic(currentState);
    		return value;
    	}
    	
    	
    	if(player == this.player) { //noi

    		int tmp, max = Integer.MIN_VALUE, min = Integer.MAX_VALUE;

    		
    		for(i = 0; i < nextStates.size(); i++) {

    			tmp = alphaBetaPruning(depth-1, nextStates.get(i), this.opponent, alpha, beta, deadline);
    			if(breakIteration)
    				return 0;
    			if(max < tmp) {
    				max = tmp;
    				if(depth == MAXDEPTH)
    					nextPartialMove = i;
    			}
    			if(min > tmp) 
    				min = tmp;
    			if(alpha < tmp) {
    				alpha = tmp;
    			}
    			if(alpha >= beta)
    				i = nextStates.size();
    		}
    		
    		
    		zobrist.put(key, new State(MAXDEPTH-depth, min, max, key));
    		return max;
    		
    	} else { //loro
    		
    		int tmp, min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;


    		for(i = 0; i < nextStates.size(); i++) {
    			tmp = alphaBetaPruning(depth-1, nextStates.get(i), this.player, alpha, beta, deadline);
    			if(breakIteration)
    				return 0;
    			if(min > tmp) {
    				min = tmp;
    				if(depth == MAXDEPTH)
    					nextPartialMove = i;
    			}
    			if(max < tmp)
    				max = tmp;
    			if(beta > tmp) {
    				beta = tmp;
    			}
    			if(alpha >= beta)
    				i = nextStates.size();
    		}
    		
    		zobrist.put(key, new State(MAXDEPTH-depth, min, max, key));
    		return min;
    	}
    }
    
    public long createKey(GameState board) {
    	long key = 0;
        int currentPiece;
        int index;
        
        // 0: red pawn, 1: white pawn, 2: red king, 3: white king
        
        for(int i = 0; i < GameState.NUMBER_OF_SQUARES; i++) {
           
            currentPiece = board.get(i);
           
            if(currentPiece != Constants.CELL_EMPTY) { 
                index = 0;
                if(0 != (currentPiece & Constants.CELL_KING)) //is king
                    index = 2;
                if(0 != (currentPiece & Constants.CELL_WHITE)) //is white
                    index++;
               
                key ^= piece[i][index];
            }
        }
       
        return key;
    }
    
    public int heuristic(GameState board) {
    	int i, cell, cell2, red=0, white=0, utility=0, nw=0, nr=0;
    	int pieceExistance = 10;
    	int kingExistance = 4;
    	int willBeKing = 4;
    	int pieceInLastRow = 2;
    	
    	//number of pieces and kings
    	for(i = 0; i < GameState.NUMBER_OF_SQUARES; i++) {
    		cell = board.get(i);
			if (0 != (cell & Constants.CELL_RED)) {
				red += pieceExistance;
				nr++;
				if(0 != (cell & Constants.CELL_KING))
					red += kingExistance;
			} else if (0 != (cell & Constants.CELL_WHITE)) {
				white += pieceExistance;
				nw++;
				if(0 != (cell & Constants.CELL_KING))
					white += kingExistance;
			}
		}
    	
    	
    	if(player == Constants.CELL_RED) {
    		if(nr > nw) {
    			enemyKilledw = 8;
    			enemyKilledr = 6;
    		} else {
    			enemyKilledw = 6;
    			enemyKilledr = 8;
    		}
    	} else if(player == Constants.CELL_WHITE) {
    		if(nw > nr) {
    			enemyKilledw = 6;
    			enemyKilledr = 8;
    		} else {
    			enemyKilledw = 8;
    			enemyKilledr = 6;
    		}
    	}
    	
    	
    	//number of pieces that will become kings in the next move without eating other pieces
    	for(i = 0; i < (GameState.NUMBER_OF_SQUARES/8); i++) {
    		cell = board.get(1, 2*i);
    		if(0 != (cell & Constants.CELL_WHITE) && 0 == (cell & Constants.CELL_KING)) {
    			cell2 = board.get(0, 2*i-1);
    			if(cell2 == Constants.CELL_EMPTY) {
    				white += willBeKing;
    			}
    			cell2 = board.get(0, 2*i+1);
    			if(cell2 == Constants.CELL_EMPTY) {
    				white += willBeKing;
    			}
    		}
    		cell = board.get(6, 2*i+1);
    		if(0 != (cell & Constants.CELL_RED) && 0 == (cell & Constants.CELL_KING)) {
    			cell2 = board.get(7, 2*i);
    			if(cell2 == Constants.CELL_EMPTY) {
    				red += willBeKing;
    			}
    			cell2 = board.get(7, 2*i+2);
    			if(cell2 == Constants.CELL_EMPTY) {
    				red += willBeKing;
    			}
    		}
    	}
    	
    	//number of pieces in the defensive row
    	for(i = 0; i < (GameState.NUMBER_OF_SQUARES/8); i++) {
    		cell = board.get(28+i);
    		if(0 != (cell & Constants.CELL_WHITE)) {
    			white += pieceInLastRow;
    		}
    		cell = board.get(i);
    		if(0 != (cell & Constants.CELL_RED)) {
    			red += pieceInLastRow;
    		}
    	}
    	
    	//pieces that are going to be eaten
    	for(i = 0; i < GameState.NUMBER_OF_SQUARES; i++) {
    		cell = board.get(i);
    		if(0 != (cell & Constants.CELL_WHITE))
    			white += checkjump(GameState.cellToRow(i), GameState.cellToCol(i), board, cell, 0);
    		else if(0 != (cell & Constants.CELL_RED))
    			red += checkjump(GameState.cellToRow(i), GameState.cellToCol(i), board, cell, 0);
		}
    	
    	//some up total utility
    	if(0 != (this.player & Constants.CELL_RED)) {
    		utility += red;
    		utility -= white;
    	} else {
    		utility -= red;
    		utility += white;
    	}
    	
    	return utility;
    }
    
    public int checkjump(int row, int col, GameState board, int piece, int utility) {
    	
    	if(0 != (piece & Constants.CELL_WHITE)) {
	    	if(0 != (board.get(row-1, col+1) & Constants.CELL_RED)) {
	    		if(board.get(row-2, col+2) == Constants.CELL_EMPTY) {
	    			utility += enemyKilledw;
	    			utility += checkjump(row-2, col+2, board, piece, 0);
	    		}
	    	}
	    	if(0 != (board.get(row-1, col-1) & Constants.CELL_RED)) {
	    		if(board.get(row-2, col-2) == Constants.CELL_EMPTY) {
	    			utility += enemyKilledw;
	    			utility += checkjump(row-2, col+2, board, piece, 0);
	    		}
	    	}
	    	if(0 != (piece & Constants.CELL_KING)) {
	    		if(0 != (board.get(row+1, col-1) & Constants.CELL_RED)) {
		    		if(board.get(row+2, col+2) == Constants.CELL_EMPTY) {
		    			utility += enemyKilledw;
		    			utility += checkjump(row-2, col+2, board, piece, 0);
		    		}
		    	}
		    	if(0 != (board.get(row+1, col+1) & Constants.CELL_RED)) {
		    		if(board.get(row+2, col-2) == Constants.CELL_EMPTY) {
		    			utility += enemyKilledw;
		    			utility += checkjump(row-2, col+2, board, piece, 0);
		    		}
		    	}
	    	}
    	} else if(0 != (piece & Constants.CELL_RED)) {
	    	if(0 != (board.get(row+1, col+1) & Constants.CELL_WHITE)) {
	    		if(board.get(row+2, col+2) == Constants.CELL_EMPTY) {
	    			utility += enemyKilledr;
	    			utility += checkjump(row+2, col+2, board, piece, 0);
	    		}
	    	}
	    	if(0 != (board.get(row+1, col-1) & Constants.CELL_WHITE)) {
	    		if(board.get(row+2, col-2) == Constants.CELL_EMPTY) {
	    			utility += enemyKilledr;
	    			utility += checkjump(row+2, col+2, board, piece, 0);
	    		}
	    	}
	    	if(0 != (piece & Constants.CELL_KING)) {
	    		if(0 != (board.get(row-1, col-1) & Constants.CELL_WHITE)) {
		    		if(board.get(row-2, col+2) == Constants.CELL_EMPTY) {
		    			utility += enemyKilledr;
		    			utility += checkjump(row-2, col+2, board, piece, 0);
		    		}
		    	}
		    	if(0 != (board.get(row-1, col+1) & Constants.CELL_WHITE)) {
		    		if(board.get(row-2, col-2) == Constants.CELL_EMPTY) {
		    			utility += enemyKilledr;
		    			utility += checkjump(row-2, col+2, board, piece, 0);
		    		}
		    	}
	    	}
    	}
    	
    	return utility;
    	
    }
    
}













