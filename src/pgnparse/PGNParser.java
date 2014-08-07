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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Deyan Rizov
 *
 */
public class PGNParser {
	
	public static String PAWN = "P";
	
	public static String KNIGHT = "N";
	
	public static String BISHOP = "B";
	
	public static String ROOK = "R";
	
	public static String QUEEN = "Q";
	
	public static String KING = "K";
	
	static final byte WHITE = -1;
	
	static final byte BLACK = 1;
	
	static final byte WHITE_PAWN = -1;
	
	static final byte WHITE_KNIGHT = -2;
	
	static final byte WHITE_BISHOP = -3;
	
	static final byte WHITE_ROOK = -4;
	
	static final byte WHITE_QUEEN = -5;
	
	static final byte WHITE_KING = -6;
	
	static final byte EMPTY = 0;
	
	static final byte BLACK_PAWN = 1;
	
	static final byte BLACK_KNIGHT = 2;
	
	static final byte BLACK_BISHOP = 3;
	
	static final byte BLACK_ROOK = 4;
	
	static final byte BLACK_QUEEN = 5;
	
	static final byte BLACK_KING = 6;
	
	private static String MOVE_TYPE_1_RE = "[a-h][1-8]";
	
	private static final int MOVE_TYPE_1_LENGTH = 2;
	
	private static String MOVE_TYPE_2_RE = "[" + PAWN + KNIGHT + BISHOP + ROOK + QUEEN + KING + "][a-h][1-8]";
	
	private static final int MOVE_TYPE_2_LENGTH = 3;
	
	private static String MOVE_TYPE_3_RE = "[" + PAWN + KNIGHT + BISHOP + ROOK + QUEEN + KING + "][a-h][a-h][1-8]";
	
	private static final int MOVE_TYPE_3_LENGTH = 4;
	
	private static String MOVE_TYPE_4_RE = "[" + PAWN + KNIGHT + BISHOP + ROOK + QUEEN + KING + "][a-h][1-8][a-h][1-8]";
	
	private static final int MOVE_TYPE_4_LENGTH = 5;
	
	private static String MOVE_TYPE_5_RE = "[a-h][a-h][1-8]";
	
	private static String MOVE_TYPE_6_RE = "[" + PAWN + KNIGHT + BISHOP + ROOK + QUEEN + KING + "][1-8][a-h][1-8]";
	
	private static final byte[][] KNIGHT_SEARCH_PATH = { { -1, 2 }, { 1, 2 }, { -1, -2 }, { 1, -2 }, { -2, 1 }, { -2, -1 }, { 2, -1 }, { 2, 1 } };
	
	private static final byte[][] BISHOP_SEARCH_PATH = { {1, 1}, {1, -1}, {-1, -1}, {-1, 1} };
	
	private static final byte[][] ROOK_SEARCH_PATH = { {0, 1}, {1, 0}, {0, -1}, {-1, 0} };
	
	private static final byte[][] QUEEN_KING_SEARCH_PATH = { {1, 1}, {1, -1}, {-1, -1}, {-1, 1}, {0, 1}, {1, 0}, {0, -1}, {-1, 0} };
	
	/**
	 * 
	 * @param pawn
	 * @param knight
	 * @param bishop
	 * @param rook
	 * @param queen
	 * @param king
	 */
	public static void setPieces(String pawn, String knight, String bishop, String rook, String queen, String king) {
		PAWN = pawn;
		KNIGHT = knight;
		BISHOP = bishop;
		ROOK = rook;
		QUEEN = queen;
		KING = king;
	}

	/**
	 * 
	 * @param pgn
	 * @return
	 * @throws PGNParseException
	 * @throws IOException
	 * @throws MalformedMoveException 
	 * @throws NullPointerException 
	 */
	public static List<PGNGame> parse(String pgn) throws PGNParseException, IOException, NullPointerException, MalformedMoveException {
		List<PGNGame> games = new LinkedList<PGNGame>();
		List<String> pgnSources = PGNParser.splitPGN(pgn);
		
		if (pgnSources == null || pgnSources.size() == 0) {
			throw new PGNParseException();
		}
		
		Iterator<String> i = pgnSources.iterator();
		
		while (i.hasNext()) {
			games.add(parsePGNGame(i.next()));
		}
		
		return games;
	}
	
	public static List<PGNGame> parse(String pgn, boolean force) throws PGNParseException, IOException, NullPointerException, MalformedMoveException {
		List<PGNGame> games = new LinkedList<PGNGame>();
		List<String> pgnSources = PGNParser.splitPGN(pgn);
		
		if (pgnSources == null || pgnSources.size() == 0) {
			throw new PGNParseException();
		}
		
		Iterator<String> i = pgnSources.iterator();
		
		while (i.hasNext()) {
			if (force) {
				try {
					games.add(parsePGNGame(i.next()));
				} catch (PGNParseException e) {
					e.printStackTrace();
				}
			} else {
				games.add(parsePGNGame(i.next()));
			}
			
		}
		
		return games;
	}
	
	/**
	 * 
	 * @param pgn
	 * @return
	 * @throws IOException
	 * @throws PGNParseException
	 * @throws MalformedMoveException 
	 * @throws NullPointerException 
	 */
	private static PGNGame parsePGNGame(String pgn) throws IOException, PGNParseException, NullPointerException, MalformedMoveException {
		byte[][] board = createDefaultBoard();
		final int[] color = { WHITE };
		PGNGame game = new PGNGame(pgn);
		BufferedReader br = new BufferedReader(new StringReader(pgn));
		String line;
		StringBuilder buffer = new StringBuilder();

		while ((line = br.readLine()) != null) {
			line = line.trim();

			if (line.startsWith("[")) {
				try {
					String tagName = line.substring(1, line.indexOf(" "));
					String tagValue = line.substring(line.indexOf("\"") + 1,
							line.lastIndexOf("\""));
					game.addTag(tagName, tagValue);
				} catch (IndexOutOfBoundsException e) {

				}
			} else {
				if (!line.isEmpty()) {
					buffer.append(line + " ");
				}
			}
		}

		String[] pairs = buffer.toString().split("\\s*\\d+\\.+\\s*");

		for (String pair : pairs) {
			if (pair.isEmpty()) {
				continue;
			}

			String[] rawMoves;
			
			if (pair.contains("{")) {
				String[] temp = pair.split("\\s+");
				int i = 0;
				ArrayList<String> list = new ArrayList<String>();
				
				while (i < temp.length) {
					if (temp[i].startsWith("{")) {
						StringBuilder b = new StringBuilder();
						
						while (i < temp.length) {
							b.append(temp[i] + " ");
							
							if (temp[i].endsWith("}")) {
								break;
							}
							
							i++;
						}
						
						list.add(b.toString().trim());
					} else {
						list.add(temp[i]);
					}
					
					i++;
				}
				
				rawMoves = list.toArray(new String[0]);
			} else {
				rawMoves = pair.split("\\s+");
			}
			
			try {
				handleRawMoves(rawMoves, game, board, color);
			} catch (PGNParseException e) {
                                e.printStackTrace();
				///throw new PGNParseException(game.toString(), e);
			}
		}
		
		return game;
	}
	
	/**
	 * 
	 * @param rawMoves
	 * @param move
	 * @param game
	 * @param board
	 * @throws MalformedMoveException
	 * @throws PGNParseException
	 */
	private static void handleRawMoves(String[] rawMoves, PGNGame game, byte[][] board, int[] color) throws MalformedMoveException, PGNParseException, NullPointerException {
		PGNMove move = null;
		
		for (int i = 0; i < rawMoves.length; i++) {
			if (rawMoves[i].equals("e.p.")) {
				continue;
			} else if (rawMoves[i].startsWith("{") && rawMoves[i].endsWith("}")) {
				move.setComment(rawMoves[i].substring(1, rawMoves[i].length() - 1));
			} else {
				if (validateMove(move = new PGNMove(rawMoves[i], board))) {
					
					if (color[0] == WHITE) {
						move.setColor(Color.white);
					} else {
						move.setColor(Color.black);
					}
					
					game.addMove(move);
					updateNextMove(move, board);
                                        
                                        ///move.printBoard();
                                        
					switchColor(color);
				}
                                else {
					throw new PGNParseException(move.getFullMove());
				}
			}
		}
	}
	
	/**
	 * 
	 * @param pgn
	 * @return
	 * @throws IOException
	 */
	private static List<String> splitPGN(String pgn) throws IOException {
		List<String> pgnGames = new LinkedList<String>();
		BufferedReader br = new BufferedReader(new StringReader(pgn));
		String line;
		StringBuilder buffer = new StringBuilder();
		
		while ((line = br.readLine()) != null) {
			line = line.trim();
			
			if (!line.isEmpty()) {
				buffer.append(line + "\r\n");
				
				if (line.endsWith("1-0") || line.endsWith("0-1") || line.endsWith("1/2-1/2") || line.endsWith("*")) {
					pgnGames.add(buffer.toString());
					buffer.delete(0, buffer.length());
				}
			}
			
		}
		
		br.close();
		
		return pgnGames;
	}
	
	/**
	 * 
	 * @param move
	 * @param board
	 * @throws PGNParseException 
	 */
	private static void updateNextMove(PGNMove move, byte[][] board) throws PGNParseException {
                ///byte[][] board = move.getBoard();                        
		String strippedMove = move.getMove();
                
		byte color;
		
		if (move.getColor() == Color.white) {
			color = WHITE;
		} else {
			color = BLACK;
		}
		
		if (move.isCastle()) {
			if (move.isKingSideCastle()) {
				move.setKingSideCastle(true);
				
				if (move.getColor() == Color.white) {
					board[6][0] = board[4][0];
					board[5][0] = board[7][0];
					board[4][0] = EMPTY;
					board[7][0] = EMPTY;
				} else {
					board[6][7] = board[4][7];
					board[5][7] = board[7][7];
					board[4][7] = EMPTY;
					board[7][7] = EMPTY;
				}
			} else {
				move.setQueenSideCastle(true);
				
				if (move.getColor() == Color.white) {
					board[2][0] = board[4][0];
					board[3][0] = board[0][0];
					board[4][0] = EMPTY;
					board[0][0] = EMPTY;
				} else {
					board[2][7] = board[4][7];
					board[3][7] = board[0][7];
					board[4][7] = EMPTY;
					board[0][7] = EMPTY;
				}
				
			}
			
		} else if (move.isEndGameMarked()) {
			//Handle situation
		} else {
			switch (strippedMove.length()) {
			case MOVE_TYPE_1_LENGTH :
				handleMoveType1(move, strippedMove, color, board);
				
				break;
			case MOVE_TYPE_2_LENGTH :
				if (strippedMove.matches(MOVE_TYPE_2_RE)) {
					handleMoveType2(move, strippedMove, color, board);
				} else if (strippedMove.matches(MOVE_TYPE_5_RE)) {
					handleMoveType5(move, strippedMove, color, board);
				}
				
				break;
			case MOVE_TYPE_3_LENGTH :
				if (strippedMove.matches(MOVE_TYPE_3_RE)) {
					handleMoveType3(move, strippedMove, color, board);
				} else if (strippedMove.matches(MOVE_TYPE_6_RE)) {
					handleMoveType6(move, strippedMove, color, board);
				}
				
				break;
			case MOVE_TYPE_4_LENGTH :
				handleMoveType4(move, strippedMove, color, board);
				
				break;
			}
			
			try {
				board[getChessATOI(move.getToSquare().charAt(0))][move.getToSquare().charAt(1) - '1'] = board[getChessATOI(move.getFromSquare().charAt(0))][move.getFromSquare().charAt(1) - '1'];
				board[getChessATOI(move.getFromSquare().charAt(0))][move.getFromSquare().charAt(1) - '1'] = EMPTY;
				
				if (move.isEnpassantCapture()) {
					board[getChessATOI(move.getEnpassantPieceSquare().charAt(0))][move.getEnpassantPieceSquare().charAt(1) - '1'] = EMPTY;
				}
				
				if (move.isPromoted()) {
					if (move.getPromotion().equals(PGNParser.QUEEN)) {
						board[getChessATOI(move.getToSquare().charAt(0))][move.getToSquare().charAt(1) - '1'] = (byte)(BLACK_QUEEN * color);
					} else if (move.getPromotion().equals(PGNParser.ROOK)) {
						board[getChessATOI(move.getToSquare().charAt(0))][move.getToSquare().charAt(1) - '1'] = (byte)(BLACK_ROOK * color);
					} else if (move.getPromotion().equals(PGNParser.BISHOP)) {
						board[getChessATOI(move.getToSquare().charAt(0))][move.getToSquare().charAt(1) - '1'] = (byte)(BLACK_BISHOP * color);
					} else if (move.getPromotion().equals(PGNParser.KNIGHT)) {
						board[getChessATOI(move.getToSquare().charAt(0))][move.getToSquare().charAt(1) - '1'] = (byte)(BLACK_KNIGHT * color);
					}
				}
			} catch (IndexOutOfBoundsException e) {
				throw new PGNParseException(move.getFromSquare() + " " + move.getToSquare(), e);
			}
			
		}
	}
	
	/**
	 * 
	 * @param move
	 * @param strippedMove
	 * @param color
	 * @param board
	 * @throws PGNParseException
	 */
	private static void handleMoveType1(PGNMove move, String strippedMove, byte color, byte[][] board) throws PGNParseException {
		int tohPos = getChessATOI(strippedMove.charAt(0));
		int tovPos = strippedMove.charAt(1) - '1';
		byte piece = (byte)(BLACK_PAWN * color);
		int fromvPos = getPawnvPos(tohPos, tovPos, piece, board);
		int fromhPos = tohPos;
		
		if (fromvPos == - 1) {
			throw new PGNParseException(move.getFullMove());
		}
		
		move.setFromSquare(getChessCoords(fromhPos, fromvPos));
		move.setToSquare(getChessCoords(tohPos, tovPos));
	}
	
	/**
	 * 
	 * @param move
	 * @param strippedMove
	 * @param color
	 * @param board
	 * @throws PGNParseException
	 */
	private static void handleMoveType2(PGNMove move, String strippedMove, byte color, byte[][] board) throws PGNParseException {
		byte piece = WHITE_PAWN;
		int tohPos = getChessATOI(strippedMove.charAt(1));
		int tovPos = strippedMove.charAt(2) - '1';
		int fromvPos = -1;
		int fromhPos = -1;
		
		if (strippedMove.charAt(0) == PAWN.charAt(0)) {
			piece = (byte)(BLACK_PAWN * color);
			fromvPos = getPawnvPos(tohPos, tovPos, piece, board);
			fromhPos = tohPos;
		} else if (strippedMove.charAt(0) == KNIGHT.charAt(0)) {
			piece = (byte)(BLACK_KNIGHT * color);
			int[]  fromPos = getSingleMovePiecePos(tohPos, tovPos, piece, board, KNIGHT_SEARCH_PATH);
			
			if (fromPos == null) {
				throw new PGNParseException(move.getFullMove());
			}
			
			fromhPos = fromPos[0];
			fromvPos = fromPos[1];
		} else if (strippedMove.charAt(0) == BISHOP.charAt(0)) {
			piece = (byte)(BLACK_BISHOP * color);
			int[] fromPos = getMultiMovePiecePos(tohPos, tovPos, piece, board, BISHOP_SEARCH_PATH);
			
			if (fromPos == null) {
				throw new PGNParseException(move.getFullMove());
			}
			
			fromhPos = fromPos[0];
			fromvPos = fromPos[1];
		} else if (strippedMove.charAt(0) == ROOK.charAt(0)) {
			piece = (byte)(BLACK_ROOK * color);
			int[] fromPos = getMultiMovePiecePos(tohPos, tovPos, piece, board, ROOK_SEARCH_PATH);
			
			if (fromPos == null) {
				throw new PGNParseException(move.getFullMove());
			}
			
			fromhPos = fromPos[0];
			fromvPos = fromPos[1];
		} else if (strippedMove.charAt(0) == QUEEN.charAt(0)) {
			piece = (byte)(BLACK_QUEEN * color);
			int[] fromPos = getMultiMovePiecePos(tohPos, tovPos, piece, board, QUEEN_KING_SEARCH_PATH);
			
			if (fromPos == null) {
				throw new PGNParseException(move.getFullMove());
			}
			
			fromhPos = fromPos[0];
			fromvPos = fromPos[1];
		} else if (strippedMove.charAt(0) == KING.charAt(0)) {
			piece = (byte)(BLACK_KING * color);
			int[]  fromPos = getSingleMovePiecePos(tohPos, tovPos, piece, board, QUEEN_KING_SEARCH_PATH);
			
			if (fromPos == null) {
				throw new PGNParseException(move.getFullMove());
			}
			
			fromhPos = fromPos[0];
			fromvPos = fromPos[1];
		}
		
		if (fromvPos == - 1 || fromhPos == -1) {
			throw new PGNParseException(move.getFullMove());
		}
		
		move.setFromSquare(getChessCoords(fromhPos, fromvPos));
		move.setToSquare(getChessCoords(tohPos, tovPos));
	}
	
	/**
	 * 
	 * @param move
	 * @param strippedMove
	 * @param color
	 * @param board
	 * @throws PGNParseException
	 */
	private static void handleMoveType3(PGNMove move, String strippedMove, byte color, byte[][] board) throws PGNParseException {
		byte piece = WHITE_PAWN;
		int fromhPos = getChessATOI(strippedMove.charAt(1));
		int tohPos = getChessATOI(strippedMove.charAt(2));
		int tovPos = strippedMove.charAt(3) - '1';
		int fromvPos = -1;
		
		if (strippedMove.charAt(0) == PAWN.charAt(0)) {
			piece = (byte)(BLACK_PAWN * color);
			fromvPos = getPawnvPos(fromhPos, tovPos, piece, board);
		} else if (strippedMove.charAt(0) == KNIGHT.charAt(0)) {
			piece = (byte)(BLACK_KNIGHT * color);
			fromvPos = getSingleMovePiecevPos(tohPos, tovPos, fromhPos, piece, board, KNIGHT_SEARCH_PATH);
		} else if (strippedMove.charAt(0) == BISHOP.charAt(0)) {
			piece = (byte)(BLACK_BISHOP * color);
			fromvPos = getMultiMovePiecevPos(tohPos, tovPos, fromhPos, piece, board, BISHOP_SEARCH_PATH);
		} else if (strippedMove.charAt(0) == ROOK.charAt(0)) {
			piece = (byte)(BLACK_ROOK * color);
			fromvPos = getMultiMovePiecevPos(tohPos, tovPos, fromhPos, piece, board, ROOK_SEARCH_PATH);
		} else if (strippedMove.charAt(0) == QUEEN.charAt(0)) {
			piece = (byte)(BLACK_QUEEN * color);
			fromvPos = getMultiMovePiecevPos(tohPos, tovPos, fromhPos, piece, board, QUEEN_KING_SEARCH_PATH);
		} else if (strippedMove.charAt(0) == KING.charAt(0)) {
			piece = (byte)(BLACK_KING * color);
			fromvPos = getSingleMovePiecevPos(tohPos, tovPos, fromhPos, piece, board, QUEEN_KING_SEARCH_PATH);
		}
		
		if (fromvPos == - 1 || fromhPos == -1) {
			throw new PGNParseException(move.getFullMove());
		}
		
		move.setFromSquare(getChessCoords(fromhPos, fromvPos));
		move.setToSquare(getChessCoords(tohPos, tovPos));
	}
	
	/**
	 * 
	 * @param move
	 * @param strippedMove
	 * @param color
	 * @param board
	 * @throws PGNParseException
	 */
	private static void handleMoveType4(PGNMove move, String strippedMove, byte color, byte[][] board) throws PGNParseException {
		byte piece = WHITE_PAWN;
		int fromhPos = getChessATOI(strippedMove.charAt(1));
		int fromvPos = strippedMove.charAt(2) - '1';
		int tohPos = getChessATOI(strippedMove.charAt(3));
		int tovPos = strippedMove.charAt(4) - '1';
		
		if (strippedMove.charAt(0) == PAWN.charAt(0)) {
			piece = (byte)(BLACK_PAWN * color);
		} else if (strippedMove.charAt(0) == KNIGHT.charAt(0)) {
			piece = (byte)(BLACK_KNIGHT * color);
		} else if (strippedMove.charAt(0) == BISHOP.charAt(0)) {
			piece = (byte)(BLACK_BISHOP * color);
		} else if (strippedMove.charAt(0) == ROOK.charAt(0)) {
			piece = (byte)(BLACK_ROOK * color);
		} else if (strippedMove.charAt(0) == QUEEN.charAt(0)) {
			piece = (byte)(BLACK_QUEEN * color);
		} else if (strippedMove.charAt(0) == KING.charAt(0)) {
			piece = (byte)(BLACK_KING * color);
		}
		
		if (fromvPos == - 1 || fromhPos == -1) {
			throw new PGNParseException(move.getFullMove());
		}
		
		if (board[fromhPos][fromvPos] != piece) {
			throw new PGNParseException("Piece does not match");
		}
		
		move.setFromSquare(getChessCoords(fromhPos, fromvPos));
		move.setToSquare(getChessCoords(tohPos, tovPos));
	}
	
	/**
	 * 
	 * @param move
	 * @param strippedMove
	 * @param color
	 * @param board
	 * @throws PGNParseException
	 */
	private static void handleMoveType5(PGNMove move, String strippedMove, byte color, byte[][] board) throws PGNParseException {
		int fromhPos = getChessATOI(strippedMove.charAt(0));
		int tohPos = getChessATOI(strippedMove.charAt(1));
		int tovPos = strippedMove.charAt(2) - '1';
		byte piece = (byte)(BLACK_PAWN * color);
		int fromvPos = getPawnvPos(fromhPos, tovPos, piece, board);
		
		if (fromvPos == - 1) {
			throw new PGNParseException(move.getFullMove());
		}
		
		if (move.isCaptured()) {
			if (board[tohPos][tovPos] == EMPTY) {
				int enPassanthPos = tohPos;
				int enPassantvPos = tovPos - (tovPos - fromvPos);
				
				if (board[enPassanthPos][enPassantvPos] == (byte)(-1 * BLACK_PAWN * color)) {
					move.setEnpassantCapture(true);
					move.setEnpassantPieceSquare(getChessCoords(enPassanthPos, enPassantvPos));
				} else {
					throw new PGNParseException(move.getFullMove() + " : " + "Enpassant capture expected!");
				}
			}
		}
		
		move.setFromSquare(getChessCoords(fromhPos, fromvPos));
		move.setToSquare(getChessCoords(tohPos, tovPos));
	}
	
	/**
	 * 
	 * @param move
	 * @param strippedMove
	 * @param color
	 * @param board
	 * @throws PGNParseException
	 */
	private static void handleMoveType6(PGNMove move, String strippedMove, byte color, byte[][] board) throws PGNParseException {
		byte piece = WHITE_PAWN;
		int fromvPos = strippedMove.charAt(1) - '1';
		int tohPos = getChessATOI(strippedMove.charAt(2));
		int tovPos = strippedMove.charAt(3) - '1';
		int fromhPos = -1;
		
		if (strippedMove.charAt(0) == PAWN.charAt(0)) {
			throw new PGNParseException(strippedMove + " : pawn found");
		} else if (strippedMove.charAt(0) == KNIGHT.charAt(0)) {
			piece = (byte)(BLACK_KNIGHT * color);
			fromhPos = getSingleMovePiecehPos(tohPos, tovPos, fromvPos, piece, board, KNIGHT_SEARCH_PATH);
		} else if (strippedMove.charAt(0) == BISHOP.charAt(0)) {
			piece = (byte)(BLACK_BISHOP * color);
			fromhPos = getMultiMovePiecehPos(tohPos, tovPos, fromvPos, piece, board, BISHOP_SEARCH_PATH);
		} else if (strippedMove.charAt(0) == ROOK.charAt(0)) {
			piece = (byte)(BLACK_ROOK * color);
			fromhPos = getMultiMovePiecehPos(tohPos, tovPos, fromvPos, piece, board, ROOK_SEARCH_PATH);
		} else if (strippedMove.charAt(0) == QUEEN.charAt(0)) {
			piece = (byte)(BLACK_QUEEN * color);
			fromhPos = getMultiMovePiecehPos(tohPos, tovPos, fromvPos, piece, board, QUEEN_KING_SEARCH_PATH);
		} else if (strippedMove.charAt(0) == KING.charAt(0)) {
			piece = (byte)(BLACK_KING * color);
			fromhPos = getSingleMovePiecehPos(tohPos, tovPos, fromvPos, piece, board, QUEEN_KING_SEARCH_PATH);
		}
		
		if (fromvPos == -1 || fromhPos == -1) {
			throw new PGNParseException(move.getFullMove());
		}
		
		move.setFromSquare(getChessCoords(fromhPos, fromvPos));
		move.setToSquare(getChessCoords(tohPos, tovPos));
	}
	
	/**
	 * 
	 * @param color
	 * @return
	 */
	private static void switchColor(int[] color) {
		if (color[0] < 0) {
			color[0] = BLACK;
		} else {
			color[0] = WHITE;
		}
	}
	
	/**
	 * 
	 * @param alfa
	 * @return
	 */
	private static int getChessATOI(char alfa) {
		return alfa - 'a';
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @return
	 */
	private static String getChessCoords(int hPos, int vPos) {
		return (char)('a' + hPos) + "" + (vPos + 1);
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param color
	 * @param board
	 * @return
	 */
	private static int getPawnvPos(int hPos, int vPos, byte piece, byte[][] board) {
		if (board[hPos][vPos + piece] == piece) {
			return vPos + piece;
		} else if (board[hPos][vPos + 2 * piece] == piece) {
			return vPos + 2 * piece;
		}
		
		return -1;
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param piece
	 * @param board
	 * @param moveData
	 * @return
	 */
	private static int[] getSingleMovePiecePos(int hPos, int vPos, byte piece, byte[][] board, byte[][] moveData) {
		for (int i = 0; i < moveData.length; i++) {
			try {
				if (board[hPos + moveData[i][0]][vPos + moveData[i][1]] == piece) {
					if (Math.abs(piece) != BLACK_KING) {
						if (isKingInCheckAfterMove(board, (byte)(piece / Math.abs(piece)), hPos + moveData[i][0], vPos + moveData[i][1], hPos, vPos)) {
							continue;
						}
					}
					
					return new int[] { hPos + moveData[i][0], vPos + moveData[i][1] };
				}
			} catch (IndexOutOfBoundsException e) {}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param fromhPos
	 * @param piece
	 * @param board
	 * @param moveData
	 * @return
	 */
	private static int getSingleMovePiecevPos(int hPos, int vPos, int fromhPos, byte piece, byte[][] board, byte[][] moveData) {
		for (int i = 0; i < moveData.length; i++) {
			try {
				if (board[hPos + moveData[i][0]][vPos + moveData[i][1]] == piece && hPos + moveData[i][0] == fromhPos) {
					if (Math.abs(piece) != BLACK_KING) {
						if (isKingInCheckAfterMove(board, (byte)(piece / Math.abs(piece)), fromhPos, vPos + moveData[i][1], hPos, vPos)) {
							continue;
						}
					}
					
					return vPos + moveData[i][1];
				}
			} catch (IndexOutOfBoundsException e) {}
		}
		
		return -1;
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param fromhPos
	 * @param piece
	 * @param board
	 * @param moveData
	 * @return
	 */
	private static int getSingleMovePiecehPos(int hPos, int vPos, int fromvPos, byte piece, byte[][] board, byte[][] moveData) {
		for (int i = 0; i < moveData.length; i++) {
			try {
				if (board[hPos + moveData[i][0]][vPos + moveData[i][1]] == piece && vPos + moveData[i][1] == fromvPos) {
					if (Math.abs(piece) != BLACK_KING) {
						if (isKingInCheckAfterMove(board, (byte)(piece / Math.abs(piece)), hPos + moveData[i][0], vPos + moveData[i][1], hPos, vPos)) {
							continue;
						}
					}
					
					return hPos + moveData[i][0];
				}
			} catch (IndexOutOfBoundsException e) {}
		}
		
		return -1;
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param piece
	 * @param board
	 * @param moveData
	 * @return
	 */
	private static int[] getMultiMovePiecePos(int hPos, int vPos, byte piece, byte[][] board, byte[][] moveData) {
		for (int i = 0; i < moveData.length; i++) {
			int[] position = getMultiMovePiecePosRec(hPos, vPos, hPos, vPos, moveData[i][0], moveData[i][1], piece, board);
			
			if (position != null) {
				return position;
			}
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param hAdd
	 * @param vAdd
	 * @param piece
	 * @param board
	 * @return
	 */
	private static int[] getMultiMovePiecePosRec(int originalhPos, int originalvPos, int hPos, int vPos, int hAdd, int vAdd, byte piece, byte[][] board) {
		hPos += hAdd;
		vPos += vAdd;
		
		try {
			if (board[hPos][vPos] == piece) {
				if (Math.abs(piece) != BLACK_KING) {
					if (isKingInCheckAfterMove(board, (byte)(piece / Math.abs(piece)), hPos, vPos, originalhPos, originalvPos)) {
						return null;
					}
				}
				
				return new int[] { hPos, vPos };
			} else if (board[hPos][vPos] != EMPTY) {
				return null;
			}
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
		
		return getMultiMovePiecePosRec(originalhPos, originalvPos, hPos, vPos, hAdd, vAdd, piece, board);
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param fromhPos
	 * @param piece
	 * @param board
	 * @param moveData
	 * @return
	 */
	private static int getMultiMovePiecevPos(int hPos, int vPos, int fromhPos, byte piece, byte[][] board, byte[][] moveData) {
		for (int i = 0; i < moveData.length; i++) {
			int fromvPos = getMultiMovePiecevPosRec(hPos, vPos, hPos, vPos, moveData[i][0], moveData[i][1], fromhPos, piece, board);
			
			if (fromvPos != -1) {
				return fromvPos;
			}
		}
		
		return -1;
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param hAdd
	 * @param vAdd
	 * @param fromhPos
	 * @param piece
	 * @param board
	 * @return
	 */
	private static int getMultiMovePiecevPosRec(int originalhPos, int originalvPos, int hPos, int vPos, int hAdd, int vAdd, int fromhPos, byte piece, byte[][] board) {
		hPos += hAdd;
		vPos += vAdd;
		
		try {
			if (board[hPos][vPos] == piece && hPos == fromhPos) {
				if (Math.abs(piece) != BLACK_KING) {
					if (isKingInCheckAfterMove(board, (byte)(piece / Math.abs(piece)), hPos, vPos, originalhPos, originalvPos)) {
						return -1;
					}
				}
				
				return vPos;
			} else if (board[hPos][vPos] != EMPTY) {
				return -1;
			}
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
		
		return getMultiMovePiecevPosRec(originalhPos, originalvPos, hPos, vPos, hAdd, vAdd, fromhPos, piece, board);
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param fromvPos
	 * @param piece
	 * @param board
	 * @param moveData
	 * @return
	 */
	private static int getMultiMovePiecehPos(int hPos, int vPos, int fromvPos, byte piece, byte[][] board, byte[][] moveData) {
		for (int i = 0; i < moveData.length; i++) {
			int fromhPos = getMultiMovePiecehPosRec(hPos, vPos, hPos, vPos, moveData[i][0], moveData[i][1], fromvPos, piece, board);

			if (fromhPos != -1) {
				return fromhPos;
			}
		}
		
		return -1;
	}
	
	/**
	 * 
	 * @param hPos
	 * @param vPos
	 * @param hAdd
	 * @param vAdd
	 * @param fromvPos
	 * @param piece
	 * @param board
	 * @return
	 */
	private static int getMultiMovePiecehPosRec(int originalhPos, int originalvPos, int hPos, int vPos, int hAdd, int vAdd, int fromvPos, byte piece, byte[][] board) {
		hPos += hAdd;
		vPos += vAdd;
		
		try {
			if (board[hPos][vPos] == piece && vPos == fromvPos) {
				if (Math.abs(piece) != BLACK_KING) {
					if (isKingInCheckAfterMove(board, (byte)(piece / Math.abs(piece)), hPos, vPos, originalhPos, originalvPos)) {
						return -1;
					}
				}
				
				return hPos;
			} else if (board[hPos][vPos] != EMPTY) {
				return -1;
			}
		} catch (IndexOutOfBoundsException e) {
			return -1;
		}
		
		return getMultiMovePiecehPosRec(originalhPos, originalvPos, hPos, vPos, hAdd, vAdd, fromvPos, piece, board);
	}
	
	/**
	 * 
	 * @param move
	 * @return
	 */
	private static boolean validateMove(PGNMove move) {
		String strippedMove = move.getMove();
		
		if (move.isCastle()) {
			return true;
		} else if (move.isEndGameMarked()) {
			return true;
		} else if (strippedMove.length() == MOVE_TYPE_1_LENGTH) {
			return strippedMove.matches(MOVE_TYPE_1_RE);
		} else if (strippedMove.length() == MOVE_TYPE_2_LENGTH) {
			return strippedMove.matches(MOVE_TYPE_2_RE) || strippedMove.matches(MOVE_TYPE_5_RE);
		} else if (strippedMove.length() == MOVE_TYPE_3_LENGTH) {
			return strippedMove.matches(MOVE_TYPE_3_RE) || strippedMove.matches(MOVE_TYPE_6_RE);
		} else if (strippedMove.length() == MOVE_TYPE_4_LENGTH) {
			return strippedMove.matches(MOVE_TYPE_4_RE);
		}
		
		return false;
	}
	
	private static boolean isKingInCheckAfterMove(byte[][] board, byte color, int hPos, int vPos, int tohPos, int tovPos) {
		try {
			byte king = (byte)(BLACK_KING * color);
			int kinghPos = -1;
			int kingvPos = -1;
			
			OUT : {
				for (int i = 0; i < 8; i++) {
					for (int j = 0; j < 8; j++) {
						if (board[j][i] == king) {
							kinghPos = j;
							kingvPos = i;
							break OUT;
						}
					}
				}
			}
			
			if (kinghPos == -1 || kingvPos == -1) {
				return false;
			}
			
			byte piece = (byte)(-1 * color * BLACK_BISHOP);
			
			for (int i = 0; i < BISHOP_SEARCH_PATH.length; i++) {
				if (isKingInCheckAfterMoveRec(board, piece, kinghPos, kingvPos, hPos, vPos, tohPos, tovPos, BISHOP_SEARCH_PATH[i][0], BISHOP_SEARCH_PATH[i][1])) {
					return true;
				}
			}
			
			piece = (byte)(-1 * color * BLACK_ROOK);
			
			for (int i = 0; i < ROOK_SEARCH_PATH.length; i++) {
				if (isKingInCheckAfterMoveRec(board, piece, kinghPos, kingvPos, hPos, vPos, tohPos, tovPos, ROOK_SEARCH_PATH[i][0], ROOK_SEARCH_PATH[i][1])) {
					return true;
				}
			}
			
			piece = (byte)(-1 * color * BLACK_QUEEN);
			
			for (int i = 0; i < QUEEN_KING_SEARCH_PATH.length; i++) {
				if (isKingInCheckAfterMoveRec(board, piece, kinghPos, kingvPos, hPos, vPos, tohPos, tovPos, QUEEN_KING_SEARCH_PATH[i][0], QUEEN_KING_SEARCH_PATH[i][1])) {
					return true;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return false;
		
	}
	
	private static boolean isKingInCheckAfterMoveRec(byte[][] board, byte piece, int hPos, int vPos, int skiphPos, int skipvPos, int tohPos, int tovPos, int hAdd, int vAdd) {
		hPos += hAdd;
		vPos += vAdd;
		
		if (hPos < 0 || hPos > 7 || vPos < 0 || vPos > 7 || (hPos == tohPos && vPos == tovPos)) {
			return false;
		}
		
		if (board[hPos][vPos] == EMPTY || (skiphPos == hPos && skipvPos == vPos)) {
			return isKingInCheckAfterMoveRec(board, piece, hPos, vPos, skiphPos, skipvPos, tohPos, tovPos, hAdd, vAdd);
		}
		
		return board[hPos][vPos] == piece;
	}
	
	/**
	 * 
	 * @return
	 */
	private static byte[][] createDefaultBoard() {
		return new byte[][] {
				{ WHITE_ROOK, WHITE_PAWN, EMPTY, EMPTY, EMPTY, EMPTY, BLACK_PAWN, BLACK_ROOK, },
				{ WHITE_KNIGHT, WHITE_PAWN, EMPTY, EMPTY, EMPTY, EMPTY, BLACK_PAWN, BLACK_KNIGHT, },
				{ WHITE_BISHOP, WHITE_PAWN, EMPTY, EMPTY, EMPTY, EMPTY, BLACK_PAWN, BLACK_BISHOP, },
				{ WHITE_QUEEN, WHITE_PAWN, EMPTY, EMPTY, EMPTY, EMPTY, BLACK_PAWN, BLACK_QUEEN, },
				{ WHITE_KING, WHITE_PAWN, EMPTY, EMPTY, EMPTY, EMPTY, BLACK_PAWN, BLACK_KING, },
				{ WHITE_BISHOP, WHITE_PAWN, EMPTY, EMPTY, EMPTY, EMPTY, BLACK_PAWN, BLACK_BISHOP, },
				{ WHITE_KNIGHT, WHITE_PAWN, EMPTY, EMPTY, EMPTY, EMPTY, BLACK_PAWN, BLACK_KNIGHT, },
				{ WHITE_ROOK, WHITE_PAWN, EMPTY, EMPTY, EMPTY, EMPTY, BLACK_PAWN, BLACK_ROOK, },
		};
	}
        
        public static void main(String[] args) {
            File file = new File("test.pgn");

            if (!file.exists()) {
                System.out.println("File does not exist!");
                return;
            }

            try {
                PGNSource source = new PGNSource(file);
                Iterator<PGNGame> i = source.listGames().iterator();

                while (i.hasNext()) {
                    PGNGame game = i.next();

                    System.out.println("############################");
                    Iterator<String> tagsIterator = game.getTagKeysIterator();

                    while (tagsIterator.hasNext()) {
                        String key = tagsIterator.next();
                        System.out.println(key + " {" + game.getTag(key) + "}");
                    }

                    System.out.println();
                    Iterator<PGNMove> movesIterator = game.getMovesIterator();
                    int num = 1;

                    while (movesIterator.hasNext()) {
                        PGNMove move = movesIterator.next();

                        if (num % 2 == 1 && !move.isEndGameMarked()) {
                            System.out.print((num + 1) / 2 + ". ");
                        }

                        num++;

                        if (move.isEndGameMarked()) {
                            System.out.print("(" + move.getMove() + ")");
                        } else if (move.isKingSideCastle()) {
                            System.out.print("[O-O] ");
                        } else if (move.isQueenSideCastle()) {
                            System.out.print("[O-O-O] ");
                        } else {
                            System.out.print("[" + move.getFromSquare() + "]->[" + move.getToSquare() + "] ");
                        }
                    }

                    System.out.println();
                }
            }            
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }	
}
