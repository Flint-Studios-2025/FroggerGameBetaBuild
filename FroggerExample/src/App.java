import javax.swing.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Frogger - Sprites + Tiles");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);

            GamePanel panel = new GamePanel();
            f.setContentPane(panel);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);

            // Make sure keys go to the panel and the timer starts
            panel.requestFocusInWindow();
            panel.start();
        });
    }
}
