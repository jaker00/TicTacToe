import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
/**
 * Created by jakereisner on 11/11/16.
 */
public class TicTacToeServer {
    public static void main(String[] args) throws Exception{
        ServerSocket listener = new ServerSocket(8901);
        System.out.println("Tic Tac Toe server is running");
        try{
            while (true) {
                Game g = new Game();
                Game.Player pX = g.new Player(listener.accept(), 'X');
                Game.Player pO = g.new Player(listener.accept(), 'O');
                pX.setOpponent(pO);
                pO.setOpponent(pX);
                g.currentPlayer = pX;
                pX.start();
                pO.start();
            }
        } finally{
            listener.close();
        }
    }
}
class Game{
    private Player[] board = {
            null,null,null,
            null,null,null,
            null,null,null
    };
    Player currentPlayer;

    public boolean hasWinner() {
        return
                (board[0] != null && board[0] == board[1] && board[0] == board[2])
                        ||(board[3] != null && board[3] == board[4] && board[3] == board[5])
                        ||(board[6] != null && board[6] == board[7] && board[6] == board[8])
                        ||(board[0] != null && board[0] == board[3] && board[0] == board[6])
                        ||(board[1] != null && board[1] == board[4] && board[1] == board[7])
                        ||(board[2] != null && board[2] == board[5] && board[2] == board[8])
                        ||(board[0] != null && board[0] == board[4] && board[0] == board[8])
                        ||(board[2] != null && board[2] == board[4] && board[2] == board[6]);
    }
    public boolean boardFilledUp() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == null) {
                return false;
            }
        }
        return true;
    }
    public synchronized boolean legalMove(int location, Player player) {
        if (player == currentPlayer && board[location] == null) {
            board[location] = currentPlayer;
            currentPlayer = currentPlayer.opponent;
            currentPlayer.otherPlayerMoved(location);
            return true;
        }
        return false;
    }


class Player extends Thread {
    char mark;
    Player opponent;
    Socket socket;
    BufferedReader input;
    PrintWriter output;

    /**
     * Constructs a handler thread for a given socket and mark
     * initializes the stream fields, displays the first two
     * welcoming messages.
     */
    public Player(Socket socket, char mark) {
        this.socket = socket;
        this.mark = mark;
        try {
            input = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            output.println("WELCOME " + mark);
            output.println("MESSAGE Waiting for opponent to connect");
        } catch (IOException e) {
            System.out.println("Player died: " + e);
        }
    }

    /**
     * Accepts notification of who the opponent is.
     */
    public void setOpponent(Player opponent) {
        this.opponent = opponent;
    }

    /**
     * Handles the otherPlayerMoved message.
     */
    public void otherPlayerMoved(int location) {
        output.println("OPPONENT_MOVED " + location);
        output.println(
                hasWinner() ? "DEFEAT" : boardFilledUp() ? "TIE" : "");
    }

    /**
     * The run method of this thread.
     */
    public void run() {
        try {
            // The thread is only started after everyone connects.
            output.println("MESSAGE All players connected");

            // Tell the first player that it is her turn.
            if (mark == 'X') {
                output.println("MESSAGE Your move");
            }

            // Repeatedly get commands from the client and process them.
            while (true) {
                String command = input.readLine();
                if (command.startsWith("MOVE")) {
                    int location = Integer.parseInt(command.substring(5));
                    if (legalMove(location, this)) {
                        output.println("VALID_MOVE");
                        output.println(hasWinner() ? "VICTORY"
                                : boardFilledUp() ? "TIE"
                                : "");
                    } else {
                        output.println("MESSAGE ?");
                    }
                } else if (command.startsWith("QUIT")) {
                    return;
                }
            }
        } catch (IOException e) {
            System.out.println("Player died: " + e);
        } finally {
            try {socket.close();} catch (IOException e) {}
        }
    }
}
}
