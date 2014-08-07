/*
 * This file is part of PGNParse.
 *
 * PGNParse is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PGNParse is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PGNParse.  If not, see <http://www.gnu.org/licenses/>. 
 */
package pgnparse;

import java.util.*;
import yacql.PayloadAnalyzer;


// Maps a row,col to file,rank
class BoardPos {
    int row;
    int col;
    char file;
    int rank;

    BoardPos(int row, int col, char file, int rank) {
        this.row = row;
        this.col = col;
        this.file = file;
        this.rank = rank;
    }
    
    static public BoardPos indexToPEN(int row, int col) {
        int rank = row+1;        
        char file = (char)('a' + col);
        return new BoardPos(row, col, file, rank);
    }
    
    static public String indexToPENStr(int row, int col) {
        int rank = row+1;        
        char file = (char)('a' + col);
        return file + "" + rank;
    }
    
    static public BoardPos PENToIndex(String pos) {
        char file = pos.charAt(0);
        int rank = pos.charAt(1);
        int row = rank - '1';
        int col = file - 'a';
        return new BoardPos(row, col, file, rank);
    }    
}

class PENPositionList {
    byte[][] board;

    public PENPositionList() {
        board = new byte[8][8];
    }
    
    void addPiece(String pos, byte piece) {
        BoardPos bpos = BoardPos.PENToIndex(pos);
        board[bpos.col][bpos.row] = piece;
    }

    public byte[][] getBoard() {
        return board;
    }
    
    public String toString() {
        int i, j;
        StringBuffer buff = new StringBuffer();
        for (i = 7; i >= 0; i--) {
            for (j = 0; j < 8; j++) {
                BoardCoordinate bc = new BoardCoordinate(j, i, board);
                buff.append(bc.getPiece()).append(" ");
            }
            buff.append("\n");
        }
        return buff.toString();
    }
}

class BoardCoordinate {
    int col;
    int row;
    byte piece;
    byte[][] board;
    
    static final String[] pieces = {"", "P", "N", "B", "R", "Q", "K"};

    public BoardCoordinate(int row, int col, byte piece) {
        this.col = col;
        this.row = row;
        this.piece = piece;
    }

    public BoardCoordinate(String pos, byte[][] board) {
        char file = pos.charAt(0);
        int rank = pos.charAt(1);
        BoardPos bpos = BoardPos.PENToIndex(pos);
        this.col = bpos.row;
        this.row = bpos.col;
        this.piece = board[bpos.col][bpos.row];
        this.board = board;
    }
    
    public BoardCoordinate(int row, int col, byte[][] board) {
        this.col = row;
        this.row = col;
        this.piece = board[this.row][this.col];
        this.board = board;
    }
    
    String getPiece() {
        return piece < 0 ? pieces[Math.abs(piece)] : pieces[Math.abs(piece)].toLowerCase();
    }
    
    int getDistance(BoardCoordinate that) {
        // assume: 'that' is reachable (vertically, horizontally or diagonally)
        // from this position
        if (this.row == that.row) {
                return Math.abs(this.col - that.col);
        }
        else if (this.col == that.col) {
                return Math.abs(this.row - that.row);
        }
        return (Math.abs(this.row - that.row) + Math.abs(this.col - that.col))>>1;
    }
    
    public String toString() {
        BoardPos bpos = BoardPos.indexToPEN(col, row);
        return getPiece() + bpos.file + "" + bpos.rank;
    }    
}


interface ChessPiece {    
    void analyze();
}


abstract class AbstractChessPiece implements ChessPiece {

    int color;
    int rowpos;
    int colpos;

    // Encoding symbols (arbitrarily chosen) for piece connectivity
    // Assume: these symbols aren't a part of the PGN data
    static final String attackingMarker = ">";
    static final String defenseMarker = "<";
    static final String rayAttackMarker = "=";
    
    List<BoardCoordinate> reachableSquares;
    List<BoardCoordinate> attackingSquares;
    List<BoardCoordinate> rayAttackingSquares;
    List<BoardCoordinate> defenseSquares;

    BoardCoordinate bc;

    public List<BoardCoordinate> getReachable() { return reachableSquares; }
    public List<BoardCoordinate> getAttacking() { return attackingSquares; }
    public List<BoardCoordinate> getRayAttcking() { return rayAttackingSquares; }
    public List<BoardCoordinate> getDefenses() { return defenseSquares; }

    public AbstractChessPiece(BoardCoordinate bc, int pieceCode) throws Exception {
        rowpos = bc.row;
        colpos = bc.col;
        color = bc.board[bc.row][bc.col] > 0 ? 1 : bc.board[bc.row][bc.col] == 0 ? 0 : -1;
        
        if (Math.abs(bc.board[bc.row][bc.col]) != Math.abs(pieceCode))
            throw new Exception("Unexpected chess piece found at " + bc.row + ", " + bc.col);
        
        this.bc = bc;
        
        this.reachableSquares = new LinkedList<BoardCoordinate>();
        this.attackingSquares = new LinkedList<BoardCoordinate>();
        this.defenseSquares = new LinkedList<BoardCoordinate>();
        this.rayAttackingSquares = new LinkedList<BoardCoordinate>();
    }

    boolean isOpponentPiece(byte piece) {
        return Math.signum(piece) * this.color == -1 ? true : false;
    }

    boolean isFriendPiece(byte piece) {
        return Math.signum(piece) * this.color == 1 ? true : false;
    }	
    
    public void analyze() {
        this.reachableSquares.clear();
        this.attackingSquares.clear();
        this.defenseSquares.clear();
        this.rayAttackingSquares.clear();
    }
    
    String encodeReachable(BoardCoordinate currPos, BoardCoordinate reachableSquare) {        
        StringBuffer buff = new StringBuffer();
        String pieceName = currPos.getPiece();
        int dist = reachableSquare.getDistance(currPos);
        // the max possible distance is 8
        float wt = -7/64.0f * dist + 1;
        buff.append(pieceName);
        buff.append(reachableSquare.toString());
        buff.append(PayloadAnalyzer.delim);
        buff.append(wt);
        return buff.toString();
    }
    
    public String toString(boolean isDoc) {
        StringBuffer buff = new StringBuffer();        
        buff.append(bc.toString()).append(" ");
        
        if (isDoc) {
            for (BoardCoordinate square: this.getReachable()) {
                //buff.append(bc.getPiece() + square.toString() + " ");
                buff.append(encodeReachable(bc, square));
                buff.append(" ");
            }
        }
        for (BoardCoordinate square: this.getAttacking()) {
            buff.append(bc.getPiece() + attackingMarker + square.toString() + " ");
        }
        for (BoardCoordinate square: this.getDefenses()) {
            buff.append(bc.getPiece() + defenseMarker + square.toString() + " ");
        }
        for (BoardCoordinate square: this.getRayAttcking()) {
            buff.append(bc.getPiece() + rayAttackMarker + square.toString() + " ");
        }
        
        return buff.toString();
    }
}


class Pawn extends AbstractChessPiece {

    public Pawn(BoardCoordinate bc) throws Exception {
        super(bc, PGNParser.BLACK_PAWN);
    }
    
    @Override
    public void analyze() {
        
        super.analyze();
        int i = rowpos;
        int j = colpos + 1;
        BoardCoordinate square = null;
        
        if (i < 8 && j < 8) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (bc.board[i][j] == PGNParser.EMPTY)
                this.reachableSquares.add(square);
        }
        
        i = rowpos + 1;
        j = colpos + 1;
        
        if (i < 8 && j < 8) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (isOpponentPiece(bc.board[i][j]))
                this.attackingSquares.add(square);
        }

        i = rowpos - 1;
        j = colpos + 1;
        
        if (i >= 0 && j < 8) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (isOpponentPiece(bc.board[i][j]))
                this.attackingSquares.add(square);
        }
    }
}

class Knight extends AbstractChessPiece {

    public Knight(BoardCoordinate bc) throws Exception  {
        super(bc, PGNParser.BLACK_KNIGHT);
    }
    
    @Override
    public void analyze() {
        
        super.analyze();
        int i, j, ki, kj, di, dj, ii, jj;
        BoardCoordinate square = null;
                
        i = rowpos;
        j = colpos;
        
        for (ki = -1; ki <= 1; ki++) {
            i = rowpos + ki;
            
            for (kj = -1; kj <= 1; kj++) {
                j = colpos + kj;
                
                if (!(ki * kj == 0))
                    continue;
                
                for (di = -1; di <=1; di += 2) {
                    ii = i + di;
                    
                    for (dj = -1; dj <=1; dj += 2) {
                        jj = j + dj;
                        
                        if (!(ii >= 0 && jj >= 0 && ii < 8 && jj < 8))
                            continue;
                        
                        // check the fork by chess-board distance
                        if (Math.abs(rowpos-ii) + Math.abs(colpos-jj) <= 2)
                            continue;
                        
                        square = new BoardCoordinate(ii, jj, bc.board[ii][jj]);
                        if (bc.board[ii][jj] == PGNParser.EMPTY)
                            this.reachableSquares.add(square);

                        else if (isOpponentPiece(bc.board[ii][jj]))
                            this.attackingSquares.add(square);
                        
                        else if (isFriendPiece(bc.board[ii][jj]))
                            this.defenseSquares.add(square);
                    }
                }
            }
        }        
    }
}


class King extends Queen {

    public King(BoardCoordinate bc) throws Exception  {
        super(bc, PGNParser.BLACK_KING);
    }
    
    @Override
    public void analyze() {
        
        super.analyze();    // delegate responsibility to the queen
        
        // Filter the list on the basis of chess-board distance        
        for (Iterator<BoardCoordinate> iter = this.reachableSquares.iterator(); iter.hasNext(); ) {
            if (iter.next().getDistance(bc) > 1)
                iter.remove();
        }
        for (Iterator<BoardCoordinate> iter = this.defenseSquares.iterator(); iter.hasNext(); ) {
            if (iter.next().getDistance(bc) > 1)
                iter.remove();
        }
        for (Iterator<BoardCoordinate> iter = this.attackingSquares.iterator(); iter.hasNext(); ) {
            if (iter.next().getDistance(bc) > 1)
                iter.remove();
        }
        rayAttackingSquares.clear();
    }
}

class Queen extends AbstractChessPiece {

    // A Queen esentially (is-a) (or has-a in case of Java since it
    // doesn't support multiple inheritance.
    Bishop bishop;
    Rook   rook;

    Queen(BoardCoordinate bc, int code) throws Exception {
        super(bc, code);
        bishop = new Bishop(bc, code);
        rook = new Rook(bc, code);
    }
    
    Queen(BoardCoordinate bc) throws Exception {
        super(bc, PGNParser.BLACK_QUEEN);
        bishop = new Bishop(bc, PGNParser.BLACK_QUEEN);
        rook = new Rook(bc, PGNParser.BLACK_QUEEN);
    }
    
    @Override
    public void analyze() {
        
        super.analyze();
        
        bishop.analyze();
        this.reachableSquares.addAll(bishop.reachableSquares);
        this.attackingSquares.addAll(bishop.attackingSquares);
        this.defenseSquares.addAll(bishop.defenseSquares);
        this.rayAttackingSquares.addAll(bishop.rayAttackingSquares);
        
        
        rook.analyze();
        this.reachableSquares.addAll(rook.reachableSquares);
        this.attackingSquares.addAll(rook.attackingSquares);
        this.defenseSquares.addAll(rook.defenseSquares);
        this.rayAttackingSquares.addAll(rook.rayAttackingSquares);        
    }

}

class Rook extends AbstractChessPiece {

    public Rook(BoardCoordinate bc, int code) throws Exception  {
        super(bc, code);
    }
    
    public Rook(BoardCoordinate bc) throws Exception  {
        super(bc, PGNParser.BLACK_ROOK);
    }
    
    @Override
    public void analyze() {
        
        super.analyze();
        
        BoardCoordinate square = null;        
        List<BoardCoordinate> attackList = null;
        int i, j;
        boolean rayAttack;
        boolean blocked;
        
        // left-hrizontal
        i = rowpos - 1;
        j = colpos;
        rayAttack = false;
        blocked = false;
        
        // left-horizontal
        while (i >= 0) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (bc.board[i][j] == PGNParser.EMPTY && !blocked) {
                reachableSquares.add(square);
            }
            else if (isFriendPiece(bc.board[i][j])) {
                defenseSquares.add(square);
                blocked = true;
                break;
            }
            else if (isOpponentPiece(bc.board[i][j])) {
                attackList = rayAttack? rayAttackingSquares : attackingSquares;
                attackList.add(square);
                if (rayAttack)
                    break;  // ray attack of depth 1
                rayAttack = true;
                blocked = true;
            }
            i--;
        }
        
        // right-hrizontal
        i = rowpos + 1;
        j = colpos;
        rayAttack = false;
        blocked = false;
        
        while (i < 8) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (bc.board[i][j] == PGNParser.EMPTY && !blocked) {
                reachableSquares.add(square);
            }
            else if (isFriendPiece(bc.board[i][j])) {
                defenseSquares.add(square);
                blocked = true;
                break;
            }
            else if (isOpponentPiece(bc.board[i][j])) {
                blocked = true;
                attackList = rayAttack? rayAttackingSquares : attackingSquares;
                attackList.add(square);
                if (rayAttack)
                    break;  // ray attack of depth 1
                rayAttack = true;
            }
            i++;
        }

        // up-vertical
        i = rowpos;
        j = colpos - 1;
        rayAttack = false;
        blocked = false;
        
        while (j >= 0) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (bc.board[i][j] == PGNParser.EMPTY && !blocked) {
                reachableSquares.add(square);
            }
            else if (isFriendPiece(bc.board[i][j])) {
                blocked = true;
                defenseSquares.add(square);
                break;
            }
            else if (isOpponentPiece(bc.board[i][j])) {
                blocked = true;
                attackList = rayAttack? rayAttackingSquares : attackingSquares;
                attackList.add(square);
                if (rayAttack)
                    break;  // ray attack of depth 1
                rayAttack = true;
            }
            j--;
        }

        // down-vertical
        i = rowpos;
        j = colpos + 1;
        rayAttack = false;
        blocked = false;
        
        while (j < 8) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (bc.board[i][j] == PGNParser.EMPTY && !blocked) {
                reachableSquares.add(square);
            }
            else if (isFriendPiece(bc.board[i][j])) {
                blocked = true;
                defenseSquares.add(square);
                break;
            }
            else if (isOpponentPiece(bc.board[i][j])) {
                blocked = true;
                attackList = rayAttack? rayAttackingSquares : attackingSquares;
                attackList.add(square);
                if (rayAttack)
                    break;  // ray attack of depth 1
                rayAttack = true;
            }
            j++;
        }
        
    }
}


class Bishop extends AbstractChessPiece {

    public Bishop(BoardCoordinate bc, int code) throws Exception  {
        super(bc, code);
    }
    
    public Bishop(BoardCoordinate bc) throws Exception  {
        super(bc, PGNParser.BLACK_BISHOP);
    }

    @Override
    public void analyze() {
        
        super.analyze();
        
        BoardCoordinate square = null;        
        List<BoardCoordinate> attackList = null;
        int i, j;
        boolean rayAttack;
        boolean blocked;
        
        // upper-left-right diagonal
        i = rowpos - 1;
        j = colpos - 1;
        rayAttack = false;
        blocked = false;
        
        while (i >=0 && j >= 0) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (bc.board[i][j] == PGNParser.EMPTY && !blocked) {
                reachableSquares.add(square);
            }
            else if (isFriendPiece(bc.board[i][j])) {
                blocked = true;
                defenseSquares.add(square);
                break;
            }
            else if (isOpponentPiece(bc.board[i][j])) {
                blocked = true;
                attackList = rayAttack? rayAttackingSquares : attackingSquares;
                attackList.add(square);
                if (rayAttack)
                    break;  // ray attack of depth 1
                rayAttack = true;
            }
            i--;
            j--;
        }
        
        // lower  left-right diagonal
        i = rowpos + 1;
        j = colpos + 1;
        blocked = false;
        rayAttack = false;
        
        while (i < 8 && j < 8) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (bc.board[i][j] == PGNParser.EMPTY && !blocked) {
                reachableSquares.add(square);
            }
            else if (isFriendPiece(bc.board[i][j])) {
                blocked = true;
                defenseSquares.add(square);
                break;
            }
            else if (isOpponentPiece(bc.board[i][j])) {
                blocked = true;
                attackList = rayAttack? rayAttackingSquares : attackingSquares;
                attackList.add(square);
                if (rayAttack)
                    break;  // ray attack of depth 1
                rayAttack = true;
            }
            i++;
            j++;
        }
        
        // upper-right-left diagonal
        i = rowpos + 1;
        j = colpos - 1;
        blocked = false;
        rayAttack = false;
        
        while (i < 8 && j >=0) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (bc.board[i][j] == PGNParser.EMPTY && !blocked) {
                reachableSquares.add(square);
            }
            else if (isFriendPiece(bc.board[i][j])) {
                blocked = true;
                defenseSquares.add(square);
                break;
            }
            else if (isOpponentPiece(bc.board[i][j])) {
                blocked = true;
                attackList = rayAttack? rayAttackingSquares : attackingSquares;
                attackList.add(square);
                if (rayAttack)
                    break;  // ray attack of depth 1
                rayAttack = true;
            }
            i++;
            j--;
        }
        
        // lower right-left diagonal
        i = rowpos - 1;
        j = colpos + 1;
        blocked = false;
        rayAttack = false;
        
        while (i >= 0 && j < 8) {
            square = new BoardCoordinate(i, j, bc.board[i][j]);
            if (bc.board[i][j] == PGNParser.EMPTY && !blocked) {
                reachableSquares.add(square);
            }
            else if (isFriendPiece(bc.board[i][j])) {
                blocked = true;
                defenseSquares.add(square);
                break;
            }
            else if (isOpponentPiece(bc.board[i][j])) {
                blocked = true;
                attackList = rayAttack? rayAttackingSquares : attackingSquares;
                attackList.add(square);
                if (rayAttack)
                    break;  // ray attack of depth 1
                rayAttack = true;
            }
            i--;
            j++;
        }
    }
}

class ChessPieceGenerator {
    
    static AbstractChessPiece createPiece(BoardCoordinate bc) throws Exception {
        // Generate the chess piece which corresponds to the current
        // board position
        AbstractChessPiece piece = null;
        byte boardVal = bc.board[bc.row][bc.col];
        int offset;
        
        if (boardVal == PGNParser.EMPTY)
            return null;
        
        offset = Math.abs(boardVal - PGNParser.EMPTY);
        switch (offset) {
            case PGNParser.BLACK_BISHOP:
                piece = new Bishop(bc);
                break;
            case PGNParser.BLACK_KNIGHT:
                piece = new Knight(bc);
                break;
            case PGNParser.BLACK_ROOK:
                piece = new Rook(bc);
                break;
            case PGNParser.BLACK_PAWN:
                piece = new Pawn(bc);
                break;
            case PGNParser.BLACK_QUEEN:
                piece = new Queen(bc);
                break;                
            case PGNParser.BLACK_KING:
                piece = new King(bc);
        }
        return piece;
    }
}

/**
 * 
 * @author Deyan Rizov
 * 
 */
public class PGNMove {

    private String move;

    private String fullMove;

    private String fromSquare;

    private String toSquare;

    private String piece;

    private Color color;

    private String comment;

    private boolean checked;

    private boolean checkMated;

    private boolean captured;

    private boolean promoted;

    private String promotion;

    private boolean endGameMarked;

    private String endGameMark;

    private boolean kingSideCastle;

    private boolean queenSideCastle;

    private boolean enpassant;

    private boolean enpassantCapture;

    private String enpassantPieceSquare;

    byte[][] board;
    
    /**
     * @param fullMove
     */
    PGNMove(String fullMove, byte[][] board) throws MalformedMoveException {
            this(fullMove, "", board);
    }

    /**
     * @param fullMove
     * @param comment
     * @throws MalformedMoveException 
     */
    PGNMove(String fullMove, String comment, byte[][] board) throws MalformedMoveException {
            super();
            this.fullMove = fullMove;
            this.comment = comment;
            parse();
            
            this.board = new byte[8][8];
            
            for (int i = 0; i < 8; i++) {
                System.arraycopy(board[i], 0, this.board[i], 0, 8);
            }
    }

    public byte[][] getBoard() {
        return board;
    }
    
    /**
     * @return the comment
     */
    public String getComment() {
            return comment;
    }

    /**
     * @param comment the comment to set
     */
    void setComment(String comment) {
            this.comment = comment;
    }

    /**
     * @return the move
     */
    public String getMove() {
            return move;
    }

    /**
     * @return the fullMove
     */
    public String getFullMove() {
            return fullMove;
    }

    /**
     * @return the fromSquare
     */
    public String getFromSquare() {
            return fromSquare;
    }

    /**
     * @param fromSquare the fromSquare to set
     */
    void setFromSquare(String fromSquare) {
            this.fromSquare = fromSquare;
    }

    /**
     * @return the toSquare
     */
    public String getToSquare() {
            return toSquare;
    }

    /**
     * @param toSquare the toSquare to set
     */
    void setToSquare(String toSquare) {
            this.toSquare = toSquare;
    }

    /**
     * @return the piece
     */
    public String getPiece() {
            return piece;
    }

    /**
     * @param piece the piece to set
     */
    void setPiece(String piece) {
            this.piece = piece;
    }

    /**
     * @return the color
     */
    public Color getColor() {
            return color;
    }

    /**
     * @param color the color to set
     */
    void setColor(Color color) {
            this.color = color;
    }

    /**
     * @return the checked
     */
    public boolean isChecked() {
            return checked;
    }

    /**
     * @return the captured
     */
    public boolean isCaptured() {
            return captured;
    }

    /**
     * @return the promoted
     */
    public boolean isPromoted() {
            return promoted;
    }

    /**
     * @return the promotion
     */
    public String getPromotion() {
            return promotion;
    }

    /**
     * @return the endGameMarked
     */
    public boolean isEndGameMarked() {
            return endGameMarked;
    }

    /**
     * @return the endGameMark
     */
    public String getEndGameMark() {
            return endGameMark;
    }

    /**
     * @return the checkMated
     */
    public boolean isCheckMated() {
            return checkMated;
    }

    /**
     * @return the kingSideCastle
     */
    public boolean isKingSideCastle() {
            return kingSideCastle;
    }

    /**
     * @param kingSideCastle the kingSideCastle to set
     */
    void setKingSideCastle(boolean kingSideCastle) {
            this.kingSideCastle = kingSideCastle;
    }

    /**
     * @return the queenSideCastle
     */
    public boolean isQueenSideCastle() {
            return queenSideCastle;
    }

    /**
     * @param queenSideCastle the queenSideCastle to set
     */
    void setQueenSideCastle(boolean queenSideCastle) {
            this.queenSideCastle = queenSideCastle;
    }

    /**
     * @return
     */
    public boolean isCastle() {
            return kingSideCastle || queenSideCastle;
    }

    /**
     * @return the enpassant
     */
    public boolean isEnpassant() {
            return enpassant;
    }

    /**
     * @param enpassant the enpassant to set
     */
    void setEnpassant(boolean enpassant) {
            this.enpassant = enpassant;
    }

    /**
     * @return the enpassantCapture
     */
    public boolean isEnpassantCapture() {
            return enpassantCapture;
    }

    /**
     * @param enpassantCapture the enpassantCapture to set
     */
    void setEnpassantCapture(boolean enpassantCapture) {
            this.enpassantCapture = enpassantCapture;
    }

    /**
     * @return the enpassantPieceSquare
     */
    public String getEnpassantPieceSquare() {
            return enpassantPieceSquare;
    }

    /**
     * @param enpassantPieceSquare the enpassantPieceSquare to set
     */
    void setEnpassantPieceSquare(String enpassantPieceSquare) {
            this.enpassantPieceSquare = enpassantPieceSquare;
    }

    /**
     * 
     * @throws MalformedMoveException
     */
    private void parse() throws MalformedMoveException {
            if (fullMove == null) {
                    throw new NullPointerException();
            }

            String move = fullMove;

            if (move.startsWith(PGNParser.PAWN)) {
                    this.piece = PGNParser.PAWN;
            } else if (move.startsWith(PGNParser.KNIGHT)) {
                    this.piece = PGNParser.KNIGHT;
            } else if (move.startsWith(PGNParser.BISHOP)) {
                    this.piece = PGNParser.BISHOP;
            } else if (move.startsWith(PGNParser.ROOK)) {
                    this.piece = PGNParser.ROOK;
            } else if (move.startsWith(PGNParser.QUEEN)) {
                    this.piece = PGNParser.QUEEN;
            } else if (move.startsWith(PGNParser.KING)) {
                    this.piece = PGNParser.KING;
            } else {
                    this.piece = PGNParser.PAWN;
            }

            if (move.contains("x")) {
                    this.captured = true;
                    move = move.replace("x", "");
            }

            if (move.contains("+")) {
                    this.checked = true;
                    move = move.replace("+", "");
            }

            if (move.contains("#")) {
                    this.checkMated = true;
                    move = move.replace("#", "");
            }

            if (move.contains("=")) {
                    try {
                            String promotedPiece = move.substring(move.indexOf('=') + 1);

                            if (promotedPiece.equals(PGNParser.PAWN)
                                            || promotedPiece.equals(PGNParser.KNIGHT)
                                            || promotedPiece.equals(PGNParser.BISHOP)
                                            || promotedPiece.equals(PGNParser.ROOK)
                                            || promotedPiece.equals(PGNParser.QUEEN)
                                            || promotedPiece.equals(PGNParser.KING))
                            {
                                    move = move.substring(0, move.indexOf('='));
                                    this.promoted = true;
                                    this.promotion = promotedPiece;
                            }
                            else
                            {
                                    throw new MalformedMoveException("Wrong piece abr [" + promotedPiece + "]");
                            }
                    } catch (IndexOutOfBoundsException e) {
                            throw new MalformedMoveException(e);
                    }
            }

            if (move.equals("0-0") || move.equals("O-O")) {
                    kingSideCastle = true;
            } else if (move.equals("0-0-0") || move.equals("O-O-O")) {
                    queenSideCastle = true;
            } else if (move.equals("1-0") || move.equals("0-1") || move.equals("1/2-1/2") || move.equals("*")) {
                    this.endGameMarked = true;
                    this.endGameMark = move;
            }

            this.move = move;
    }

    public void printBoard() {
        int i, j;
        
        for (i = 7; i >= 0; i--) {
            for (j = 0; j < 8; j++) {
                BoardCoordinate bc = new BoardCoordinate(i, j, board);
                System.out.print(bc.getPiece() + " \t");
            }
            System.out.println();
        }
        System.out.println();
    }

    // Generate the "term" representation of this board position
    // which is to be saved in the Lucene index.
    @Override
    public String toString() {
        return this.fromSquare + "->" + this.toSquare;
    }
    
    public String graphEncodingForQuery() {
        return analyzeAll(this.board, false);        
    }
    
    public String graphEncoding() {
        return analyzeAll(this.board, true);
    }
    
    public String getFEN() {
        return getFENStatic(board);
    }

    public static String getFENStatic(byte[][] board) {
        StringBuffer buff = new StringBuffer();
        int i, j;
        String piece = null;
        int pieceCode;
        
        for (j = 7; j >= 0; j--) {
            StringBuffer fen = new StringBuffer();
            
            for (i = 0; i < 8; i++) {
                pieceCode = board[i][j];
                if (pieceCode != PGNParser.EMPTY) {
                    piece = BoardCoordinate.pieces[Math.abs(pieceCode)];
                }
                else {
                    piece = "1";
                }
                piece = pieceCode > 0? piece.toLowerCase() : piece;
                
                if (fen.length() > 0) {
                    char lastPiece = fen.charAt(fen.length()-1);
                    if (lastPiece >= '1' && lastPiece <= '7' && pieceCode == PGNParser.EMPTY) {
                        fen.deleteCharAt(fen.length()-1);
                        piece = String.valueOf(lastPiece - '0' + 1);
                    }
                }
                    
                fen.append(piece);
            }
            buff.append(fen).append("/");
        }
        
        buff.deleteCharAt(buff.length()-1);
        buff.append(" w KQkq - 0 1");    // necessary suffix to form a valid FEN
        return buff.toString();        
    }
    
    static public String analyzeAll(byte[][] board, boolean isDoc) {
        StringBuffer buff = new StringBuffer();
        int i, j;
        AbstractChessPiece thisPiece = null;
        
        for (i = 0; i < 8; i++) {
            for (j = 0; j < 8; j++) {
                
                //String pen = BoardPos.indexToPENStr(j, i);
                BoardCoordinate bc = new BoardCoordinate(i, j, board); //new BoardCoordinate(pen, board);
                try {
                    thisPiece = ChessPieceGenerator.createPiece(bc);
                }
                catch (Exception ex) {
                    System.out.println(ex);
                }
                
                if (thisPiece == null)
                    continue;
                
                thisPiece.analyze();
                buff.append(thisPiece.toString(isDoc));
                buff.append("\n");
            }
        }        
        return buff.toString();
    }
        
    public static void main(String[] args) {
        byte[][] boardPos = null;
        BoardCoordinate bc = null;
        
        if (false) {
           byte[][] b = {
                { PGNParser.WHITE_ROOK, PGNParser.WHITE_PAWN, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.BLACK_PAWN, PGNParser.BLACK_ROOK, },
                { PGNParser.WHITE_KNIGHT, PGNParser.WHITE_PAWN, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.BLACK_PAWN, PGNParser.EMPTY, },
                { PGNParser.WHITE_BISHOP, PGNParser.WHITE_PAWN, PGNParser.EMPTY, PGNParser.WHITE_QUEEN, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.BLACK_PAWN, PGNParser.EMPTY, },
                { PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.BLACK_BISHOP, PGNParser.EMPTY, PGNParser.BLACK_QUEEN, },
                { PGNParser.WHITE_ROOK, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.WHITE_PAWN, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, },
                { PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.BLACK_KNIGHT, PGNParser.BLACK_PAWN, PGNParser.BLACK_ROOK, },
                { PGNParser.WHITE_KING, PGNParser.BLACK_BISHOP, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.BLACK_PAWN, PGNParser.BLACK_KING, },
                { PGNParser.EMPTY, PGNParser.WHITE_PAWN, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.EMPTY, PGNParser.BLACK_PAWN, PGNParser.EMPTY }
            };
            boardPos = b;
            
            if (false) {
                try {
                    AbstractChessPiece piece = new Queen(new BoardCoordinate("c4", boardPos));
                    piece.analyze();
                    System.out.println(piece);
                }
                catch (Exception ex) {
                    System.out.println(ex);
                }
            }
            else if (false) {
                try {
                    AbstractChessPiece piece = new Queen(new BoardCoordinate(3, 2, boardPos));
                    piece.analyze();
                    System.out.println(piece);                
                }
                catch (Exception ex) {
                    System.out.println(ex);
                }                
            }
            else if (true)
                System.out.println(PGNMove.analyzeAll(boardPos, true));
        }
        else if (true) {
        
            PENPositionList penPosList = new PENPositionList();
            penPosList.addPiece("a2", PGNParser.WHITE_PAWN);
            penPosList.addPiece("b2", PGNParser.WHITE_PAWN);
            penPosList.addPiece("c2", PGNParser.WHITE_PAWN);
            penPosList.addPiece("e5", PGNParser.WHITE_PAWN);
            penPosList.addPiece("h2", PGNParser.WHITE_PAWN);
            penPosList.addPiece("a1", PGNParser.WHITE_ROOK);
            penPosList.addPiece("b1", PGNParser.WHITE_KNIGHT);
            penPosList.addPiece("c1", PGNParser.WHITE_BISHOP);
            penPosList.addPiece("e1", PGNParser.WHITE_ROOK);
            penPosList.addPiece("g1", PGNParser.WHITE_KING);
            penPosList.addPiece("c4", PGNParser.WHITE_QUEEN);
            
            penPosList.addPiece("a8", PGNParser.BLACK_ROOK);
            penPosList.addPiece("d8", PGNParser.BLACK_QUEEN);
            penPosList.addPiece("f8", PGNParser.BLACK_ROOK);
            penPosList.addPiece("g8", PGNParser.BLACK_KING);
            penPosList.addPiece("d6", PGNParser.BLACK_BISHOP);
            penPosList.addPiece("g2", PGNParser.BLACK_BISHOP);
            penPosList.addPiece("f6", PGNParser.BLACK_KNIGHT);
            penPosList.addPiece("a7", PGNParser.BLACK_PAWN);
            penPosList.addPiece("b7", PGNParser.BLACK_PAWN);
            penPosList.addPiece("c7", PGNParser.BLACK_PAWN);
            penPosList.addPiece("f7", PGNParser.BLACK_PAWN);
            penPosList.addPiece("g7", PGNParser.BLACK_PAWN);
            penPosList.addPiece("h7", PGNParser.BLACK_PAWN);

            System.out.println(penPosList);
            boardPos = penPosList.getBoard();
            
            if (false) {
                try {
                    AbstractChessPiece piece = new Queen(new BoardCoordinate("c4", boardPos));
                    piece.analyze();
                    System.out.println(piece);

                    piece = new Rook(new BoardCoordinate("e1", boardPos));
                    piece.analyze();
                    System.out.println(piece);

                    piece = new Bishop(new BoardCoordinate("c1", boardPos));
                    piece.analyze();
                    System.out.println(piece);

                    piece = new Pawn(new BoardCoordinate("e5", boardPos));
                    piece.analyze();
                    System.out.println(piece);

                    piece = new Knight(new BoardCoordinate("f6", boardPos));
                    piece.analyze();
                    System.out.println(piece);            

                    piece = new King(new BoardCoordinate("g1", boardPos));
                    piece.analyze();
                    System.out.println(piece);
                }
                catch (Exception ex) {
                    System.out.println(ex);
                }                
            }
            
            System.out.println(PGNMove.analyzeAll(boardPos, true));
            System.out.println(PGNMove.getFENStatic(boardPos));
            
        }            
    }
}
