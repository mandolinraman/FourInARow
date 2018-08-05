/*----------------------------------------------------------------
 *  Author:        Raman Venkataramani
 *  Written:       5/24/2018
 *  Last updated:  5/24/2018
 *
 *  Compilation:   javac FourInARow.java
 *  Execution:     java FourInARow
 *
 *  Implements the FourInARow class
 *----------------------------------------------------------------*/

import java.awt.Font;
import java.awt.Color;
import java.util.Scanner;
// import edu.princeton.cs.algs4.StdDraw;

public class FourInARow {
	private final static String RED = "\u001B[31m";
	private final static String GRN = "\u001B[32m";
	private final static String YEL = "\u001B[33m";
	private final static String BLU = "\u001B[34m";
	private final static String MAG = "\u001B[35m";
	private final static String CYN = "\u001B[36m";
	private final static String WHT = "\u001B[37m";
	private final static String RESET = "\u001B[0m";

	public final static int BigNum = 500;
	public final static int Inf = 9999;

	private int nx, ny, levels, computerColor, humanColor;
	private int[][]	board;
	private int[] height;
	private boolean firsttime, useGui;
	private int winner;

	private final class MyResult {
		private final int first;
		private final int second;

		public MyResult(int first, int second) {
			this.first = first;
			this.second = second;
		}

		public int getFirst() {
			return first;
		}

		public int getSecond() {
			return second;
		}
	}

	// constructor
	public FourInARow(int nx, int ny, int levels, boolean gui) {
		this.nx = nx;
		this.ny = ny;
		this.levels = levels;

		board = new int[nx][ny];
		height = new int[nx];

		this.computerColor = 1;
		this.humanColor = 2;
		firsttime = true;
		useGui = gui;
		winner = 0;
	}

	public void setComputerColor(int computerColor) {
		this.computerColor = computerColor;
		this.humanColor = 3 - computerColor;
	}

	public void play(int color, int column) {
		if (column < 0 || column >= nx || height[column] == ny) {
			throw new IllegalArgumentException("Invalid move.");
		}

		board[column][height[column]] = color;
		height[column]++;
	}

	public void actualPlay(int color, int column) {
		play(color, column);

		if (deltaAdvantage(column) == FourInARow.BigNum) {
			winner = color;
		}
	}

	public void retract(int column) {
		if (column < 0 || column >= nx || height[column] == 0) {
			throw new IllegalArgumentException("Invalid move.");
		}

		height[column]--;
		board[column][height[column]] = 0;
	}

	public int getHumanMove() {
		if (useGui) {
			while (true) {
				// detected mouse click
				if (StdDraw.isMousePressed()) {
					// screen coordinates
					double x = StdDraw.mouseX();
					double y = StdDraw.mouseY();
					int move = (int) (Math.floor(x));
					int row = (int) (Math.floor(y));

					if (0 <= row && row < ny) {
						if (0 <= move && move < nx && height[move] < ny) {
							return move;
						}
					}
				}
			}
		} else {
			Scanner in = new Scanner(System.in);
			while (true) {
				System.out.print("Your move: column ");
				int move = in.nextInt() - 1;
				if (move < 0 || move >= nx || height[move] == ny) {
					System.out.println("Invalid move, try again!");
				} else {
					return move;
				}
			}
		}
	}

	public int getWinner() {
		return winner;
	}

	private int howMany(int x, int y, int u, int v, int color) {
		int num = 0;
		while (0 <= x && x < nx && 0 <= y && y < ny && board[x][y] == color) {
			num++;
			x += u;
			y += v;
		}
		return num;
	}

	private int deltaAdvantage(int column) {
		int row = height[column] - 1;
		int color = board[column][row];
		int h = howMany(column, row, 1, 0, color) + howMany(column, row, -1, 0, color) - 1;
		int v = howMany(column, row, 0, 1, color) + howMany(column, row, 0, -1, color) - 1;
		int d1 = howMany(column, row, 1, 1, color) + howMany(column, row, -1, -1, color) - 1;
		int d2 = howMany(column, row, 1, -1, color) + howMany(column, row, -1, 1, color) - 1;

		if (h >= 4 || v >= 4 || d1 >= 4 || d2 >= 4) {
			return BigNum;
		} else {
			return h + v + d1 + d2;
		}
	}

	private MyResult minimax(int mode, int lev, int previousSmallest, int advSoFar) {
		// "mode = +1 for max, and -1 for min"

		int bestMove = 0;
		if (lev == 0) {
			return new MyResult(bestMove, advSoFar);
		} else {
			int biggestYet = -Inf*mode;
			int color = (mode == 1 ? computerColor : humanColor);
			for (int column = 0; column < nx; column++) {
				if (height[column] != ny) {
					play(color, column);
					int delta = deltaAdvantage(column);
					if (delta == BigNum) {
						retract(column);
						biggestYet = mode*delta; // add adv_so_far?
						bestMove = column;
						break;
					}
					int newAdv = advSoFar + mode*delta;
					MyResult temp = minimax(-mode, lev-1, biggestYet, newAdv);
					int move = temp.getFirst();
					int smallestFound = temp.getSecond();

					if (mode*smallestFound > mode*biggestYet) {
						// Very important: >= won"t work
						biggestYet = smallestFound;
						bestMove = column;
					}
					retract(column);
					if (mode*biggestYet >= mode*previousSmallest) {
						// ... we change this one to a >
						break;
					}
				}
			}
			biggestYet++;  // ..??..
			return new MyResult(bestMove, biggestYet);
		}
	}

	public void display(int lastMove) {
		if (useGui) {
			if (firsttime) {
				firsttime = false;
				StdDraw.setCanvasSize(100*nx+10, 100*ny+10);
				StdDraw.setXscale(-0.3, nx + 0.3);
				StdDraw.setYscale(-0.3, ny + 0.3);   // leave a border to write text
			}

			// initial conditions for game piece
			double g = 0.04; // gravity
			double attenuation = 0.5;
			double y = ny;
			double v = 0.0;
			int bounces = 0;
			int maxBounces = 3;
			double ymin = 0; // final resting position of last move piece
			if (lastMove >=0 && lastMove < nx) {
				ymin = height[lastMove]-1;
			}

			while (true) {
				StdDraw.clear();
				// StdDraw.setPenColor(StdDraw.LIGHT_GRAY);
				// StdDraw.filledRectangle(nx/2.0, ny/2.0, nx/2.0 + 0.3, ny/2.0 + 0.3);
				StdDraw.picture(nx/2.0, ny/2.0, "aria2.jpg", nx + 0.6, ny + 0.6);

				for (int i = 0; i < nx; i++) {
					for (int j = 0; j < ny; j++) {
						StdDraw.setPenColor(StdDraw.WHITE);
						StdDraw.filledSquare(i + 0.5, j + 0.5, 0.48);
						StdDraw.setPenColor(StdDraw.PRINCETON_ORANGE);
						StdDraw.square(i + 0.5, j + 0.5, 0.48);
					}
				}

				double r1 = 0.45;
				double r2 = 0.30;
				double r3 = 0.2;
				Color c1 = StdDraw.WHITE, c2 = StdDraw.WHITE;
				for (int i = 0; i < nx; i++) {
					for (int j = 0; j < height[i]; j++) {
						if (board[i][j] == 1) {
							c1 = StdDraw.BOOK_RED;
							c2 = StdDraw.RED;
						} else if (board[i][j] == 2) {
							c1 = StdDraw.BOOK_BLUE;
							c2 = StdDraw.BOOK_LIGHT_BLUE;
						}

						double ypos = j;
						if (i == lastMove && j == height[i]-1) {
							ypos = y; // for animation
						}
						StdDraw.setPenColor(c1);
						StdDraw.filledCircle(i + 0.5, ypos + 0.5, r1);
						StdDraw.setPenColor(c2);
						StdDraw.filledCircle(i + 0.5, ypos + 0.5, r2);
						StdDraw.setPenColor(c1);
						StdDraw.filledCircle(i + 0.5, ypos + 0.5, r3);
					}
				}

				// write status text
				StdDraw.setFont(new Font("SansSerif", Font.PLAIN, 72));
				StdDraw.setPenColor(StdDraw.BLACK);
				// for (int i = 0; i < nx; i++) {
				// 	StdDraw.text(i + 0.5, -0.3, "" + (i+1));
				// }

				StdDraw.setPenColor(StdDraw.BLACK);
				if (winner == computerColor) {
					StdDraw.text(nx/2.0, ny/2.0, "I win!");
				} else if (winner == humanColor) {
					StdDraw.text(nx/2.0, ny/2.0, "You win!");
				}
				StdDraw.show();

				if (lastMove < 0 || lastMove >= nx || bounces == maxBounces) {
					break;
				}

				// update positions
				v += g;
				y -= v + 0.5*g;
				if (y <= ymin) {
					bounces++;
					if (bounces == maxBounces) {
						y = ymin;
					} else {
						y = 2*ymin - y;
					}
					v = -attenuation*v;
				}
			}
			StdDraw.pause(20);
		} else {
			if (lastMove >=0 && lastMove < nx && board[lastMove][height[lastMove]-1] == computerColor) {
				System.out.println("My move is column " + (lastMove + 1));
			}
			String[][] ch = {{"      ", "      ", "      ", "      "},
							 {" **** ", "******", "******", " **** "},
							 {" OOOO ", "OOOOOO", "OOOOOO", " OOOO "}};

			String line = "+";
			for (int i = 0; i < nx; i++) {
				line += "------+";
			}

			System.out.println(line);
			for (int k = ny - 1; k >= 0; k--) {
				for (int r = 0; r < 4; r++) {
					System.out.print("|");
					for (int j = 0; j < nx; j++) {
						if (board[j][k] == 1) {
							System.out.print(RED + ch[1][r] + RESET + "|");
						} else if (board[j][k] == 2) {
							System.out.print(BLU + ch[2][r] + RESET + "|");
						} else {
							System.out.print(ch[0][r] + "|");
						}
					}
					System.out.println();
				}
				System.out.println(line);
			}
			for (int j = 1; j <= nx; j++) {
				System.out.printf("    %d  ", j);
			}
			System.out.println();
			if (winner == computerColor) {
				System.out.println("I win!");
			} else if (winner == humanColor) {
				System.out.println("You win!");
			}
		}
	}

	public int getComputerMove() {
		MyResult temp = minimax(1, levels, Inf, 0);
		return temp.getFirst(); // move
	}

	public void incrementLevels() {
		levels++;
	}

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);

		// turn on animation mode
		StdDraw.enableDoubleBuffering();


		int nx = 7;
		int ny = 6;
		boolean gui = true;

		FourInARow board = new FourInARow(nx, ny, 9, gui);
		board.display(-1);

		System.out.println("Who plays first?");
		int computerColor = 0;
		while (computerColor != 1 && computerColor != 2) {
			System.out.print("1 = computer, 2 = human: ");
			computerColor = in.nextInt();
		}
		int humanColor = 3 - computerColor;
		board.setComputerColor(computerColor);

		for (int i = 0; i < nx*ny; i++) {
			if (i % 8 == 0) {
				board.incrementLevels();
			}

			if (1 + i %2 == computerColor) {
				// System.out.println("Thinking...");
				int computerMove = board.getComputerMove();
				board.actualPlay(computerColor, computerMove);
				board.display(computerMove);
			} else {
				int humanMove = board.getHumanMove();
				board.actualPlay(humanColor, humanMove);
				board.display(humanMove);
			}
			if (board.getWinner() != 0) {
				break;
			}
		}

		if (board.getWinner() == 0) {
			System.out.println("It's a draw!");
		}
	}
}
