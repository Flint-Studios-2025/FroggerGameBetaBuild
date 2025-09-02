import javax.swing.*;


public class App {
public static void main(String[] args) {
SwingUtilities.invokeLater(() -> {
JFrame f = new JFrame("Frogger – Java Swing");
f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
f.setResizable(false);
GamePanel panel = new GamePanel();
f.setContentPane(panel);
f.pack();
f.setLocationRelativeTo(null);
f.setVisible(true);
panel.start();
});
}
}
