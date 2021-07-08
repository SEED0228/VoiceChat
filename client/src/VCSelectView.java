import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

class VCSelectView extends JFrame implements ActionListener{
  JTextField ip_address;
  static VCSelectView frame;

  public static void main(String args[]){
    frame = new VCSelectView();
    frame.setVisible(true);
  }

  VCSelectView(){
    setTitle("VoiceChat(ip)");
    setBounds(100, 100, 300, 200);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    JLabel label = new JLabel("Enter ip address");

    ip_address = new JTextField(15);

    JButton button = new JButton("Check");
    button.addActionListener(this);

    JPanel p = new JPanel();
    p.add(label);
    p.add(ip_address);
    p.add(button);

    Container contentPane = getContentPane();
    contentPane.add(p, BorderLayout.CENTER);
  }

  public void actionPerformed(ActionEvent e){
    String ip_add = new String(ip_address.getText());
    VCClient window = new VCClient(ip_add);
    window.setSize(800, 600);
    window.setVisible(true);
    frame.setVisible(false);
  }
}