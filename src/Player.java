import java.util.*;

class Player {
    private static final boolean INPUT_ON = true;
    private static final boolean DEBUG_ON = true;
    private static short me = 1;
    private static short opponent = 2;
    private static int turn = 0;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        Board board = new Board();

        while (true) {
            turn ++;
            initTurn(in, board);
            long start = System.currentTimeMillis();
            Cell cell = MonteCarloTreeSearch.findNextMove(board);
            debug("TIME " + (System.currentTimeMillis() - start));
            board.setAction(cell, me);
            System.out.println(cell.row + " " + cell.col);
        }
    }

    private static class Cell {
        int col, row;
        Cell(int col, int row) {
            this.col = col;
            this.row = row;
        }
        @Override
        public String toString() {
            return row + " " + col;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Cell cell = (Cell) o;

            if (col != cell.col) return false;
            return row == cell.row;
        }

        @Override
        public int hashCode() {
            int result = col;
            result = 31 * result + row;
            return result;
        }
    }

    enum BoardState {
        UNFINISHED(0), DRAW(-1), WIN(1), LOSE(2);
        private int id;
        BoardState(int id) {
            this.id = id;
        }

        static BoardState getValueById(int id) {
            for (BoardState boardState : BoardState.values()) {
                if (boardState.id == id) {
                    return boardState;
                }
            }
            return null;
        }
    }

    private static class Board {
        static final int size = 3;
        //0 - пусто, 1 - я, 2 - противник
        short[][] grid;
        static Cell[][] cells;
        int steps = 0;
        List<Cell> validActions;
        Board() {
            grid = new short[size][size];
            validActions = new ArrayList<>();
        }

        static Cell getPoint(int row, int col) {
            return cells[row][col];
        }

        List<Cell> getCellForMoves() {
            validActions.clear();
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    if (grid[row][col] == 0) {
                        validActions.add(getPoint(row, col));
                    }
                }
            }
            return validActions;
        }

        void setAction(Cell cell, short playerId) {
            grid[cell.row][cell.col] = playerId;
            steps ++;
        }

        void clearAction(Cell cell) {
            grid[cell.row][cell.col] = 0;
            steps --;
        }

        void clearActions(List<Cell> cells) {
            for (Cell move : cells) {
                clearAction(move);
            }
        }

        BoardState randomPlay(short player) {
            List<Cell> playMoves = new ArrayList<>();
            BoardState state = getState();
            while (state.equals(BoardState.UNFINISHED)) {
                List<Cell> stepMoves = getCellForMoves();
                Collections.shuffle(stepMoves);
                playMoves.add(stepMoves.get(0));
                setAction(stepMoves.get(0), player);
                state = getState();
                player = (short) (3 - player);
            }
            clearActions(playMoves);
            return state;
        }

        BoardState getState() {
            if (steps < 3) {
                return BoardState.UNFINISHED;
            }
            for (int i = 0; i < 3; i++) {
                // check rows
                if (grid[i][0] > 0 && grid[i][0] == grid[i][1] && grid[i][0] == grid[i][2]) {
                    return BoardState.getValueById(grid[i][0]);
                }
                // check cols
                if (grid[0][i] > 0 && grid[0][i] == grid[1][i] && grid[0][i] == grid[2][i]) {
                    return BoardState.getValueById(grid[0][i]);
                }
            }
            // check diags
            if (grid[0][0] > 0 && grid[0][0] == grid[1][1] && grid[0][0] == grid[2][2]) {
                return BoardState.getValueById(grid[0][0]);
            }
            if (grid[2][0] > 0 && grid[2][0] == grid[1][1] && grid[2][0] == grid[0][2]) {
                return BoardState.getValueById(grid[2][0]);
            }

            if (steps == size * size) {
                return BoardState.DRAW;
            } else {
                return BoardState.UNFINISHED;
            }
        }

        static {
            cells = new Cell[size][size];
            for (int row = 0; row < size; row ++) {
                for (int col = 0; col < size; col++) {
                    cells[row][col] = new Cell(col, row);
                }
            }
        }
    }

    private static class Node {
        Cell cell;
        short playerId;
        int visits;
        int wins;
        Node parent;
        List<Node> childArray = new ArrayList<>();

        Node getRandomChildNode() {
            Collections.shuffle(childArray);
            return childArray.get(0);
        }

        Node getChildWithMaxVisits() {
            int max = 0;
            Node best = null;
            for (Node child : childArray) {
                if (child.visits > max) {
                    max = child.visits;
                    best = child;
                }
            }
            return best;
        }

        @Override
        public String toString() {
            return playerId + ": " + cell + ": " + wins + "/" + visits;
        }
    }

    private static class Tree {
        Node root;
    }

    private static class UCT {
        static double calcUCTValue(int parentVisits, int wins, int nodeVisits) {
            if (nodeVisits == 0) {
                return Integer.MAX_VALUE;
            }
            return wins * 1.0 / nodeVisits + 1.414 * Math.sqrt(Math.log(parentVisits) / nodeVisits);
        }

        static Node findBestNodeWithUCT(Node node) {
            int parentVisit = node.visits;
            Node best = null;
            double max = 0;
            for (Node child : node.childArray) {
                double uct = calcUCTValue(parentVisit, child.wins, child.visits);
                if (uct > max) {
                    max = uct;
                    best = child;
                }
            }
            return best;
        }
    }

    private static class MonteCarloTreeSearch {

        static Cell findNextMove(Board board) {
            long start = System.currentTimeMillis();
            Tree tree = new Tree();
            tree.root = new Node();
            tree.root.cell = new Cell(-1, -1);
            tree.root.playerId = Player.opponent;
            int n = 0;
            while (/*n <= 1000*/  System.currentTimeMillis() - start < 60) {
                Node promisingNode = selectPromisingNode(tree.root, board);
                if (promisingNode.equals(tree.root) ||
                        (promisingNode.visits > 0 && BoardState.UNFINISHED.equals(board.getState()))) {
                    expandNode(promisingNode, board);
                }
                Node nodeToExplore = promisingNode;
                if (promisingNode.childArray.size() > 0) {
                    nodeToExplore = promisingNode.getRandomChildNode();
                    board.setAction(nodeToExplore.cell, nodeToExplore.playerId);
                }
                BoardState playoutResult = board.randomPlay((short) (3 - nodeToExplore.playerId));
                backPropogation(nodeToExplore, playoutResult, board);
                n++;
            }
            debug(String.valueOf(n));
            Node winnerNode = tree.root.getChildWithMaxVisits();
            return winnerNode.cell;
        }

        private static void backPropogation(Node nodeToExplore, BoardState playoutResult, Board board) {
            Node tempNode = nodeToExplore;
            while (tempNode != null) {
                tempNode.visits ++;
                if (tempNode.playerId == playoutResult.id) {
                    tempNode.wins ++;
                }
                if (tempNode.parent != null) {
                    board.clearAction(tempNode.cell);
                }
                tempNode = tempNode.parent;
            }
        }

        private static void expandNode(Node promisingNode, Board board) {
            List<Cell> possibleMoves = board.getCellForMoves();
            for (Cell cell : possibleMoves) {
                Node newNode = new Node();
                newNode.parent = promisingNode;
                newNode.cell = cell;
                newNode.playerId = (short) (3 - promisingNode.playerId);
                promisingNode.childArray.add(newNode);
            }
        }

        private static Node selectPromisingNode(Node root, Board board) {
            Node node = root;
            while (node.childArray.size() != 0) {
                node = UCT.findBestNodeWithUCT(node);
                board.setAction(node.cell, node.playerId);
            }
            return node;
        }
    }

    private static void initTurn(Scanner in, Board board) {
        int opponentRow = in.nextInt();
        int opponentCol = in.nextInt();
        out(opponentRow + " " + opponentCol);
        if (opponentCol >= 0) {
            board.setAction(Board.getPoint(opponentRow, opponentCol), opponent);
        }

        int validActionCount = in.nextInt();
        out(validActionCount);
        for (int i = 0; i < validActionCount; i++) {
            int row = in.nextInt();
            int col = in.nextInt();
            out(row + " " + col);
        }
    }

    private static void out(Object string) {
        if (INPUT_ON) {
            System.err.println(string.toString());
        }
    }

    private static void debug(String string) {
        if (DEBUG_ON) {
            System.err.println(string);
        }
    }
}
