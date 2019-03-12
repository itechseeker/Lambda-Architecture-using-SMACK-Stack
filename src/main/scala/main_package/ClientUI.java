package main_package;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static sun.awt.X11.XConstants.Success;


public class ClientUI {
    private JPanel mainPanel;
    private JPanel rightPanel;
    private JPanel leftPanel;
    private JButton topHashTagButton;
    private JTable hashtagTable;

    public ClientUI() {

        String[] columnNames = {"Hashtag", "Count"};
        Object[][] data =
                {
                        {"hello", "BACK"},
                        {"big", "EXIT"},
                        {"data", "FORWARD"},
                };

        //Add data to the table
        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);
        hashtagTable.setModel(tableModel);
        hashtagTable.setRowHeight(25);

        //Set Table header to be center
        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer)hashtagTable.getTableHeader().getDefaultRenderer();
        renderer.setHorizontalAlignment(JLabel.CENTER);

        //Set text of the row to be centered
        DefaultTableCellRenderer centerRender = new DefaultTableCellRenderer();
        centerRender.setHorizontalAlignment(JLabel.CENTER);
        for (int columnIndex = 0; columnIndex < tableModel.getColumnCount(); columnIndex++)
        {
            hashtagTable.getColumnModel().getColumn(columnIndex).setCellRenderer(centerRender);
        }

        //Update data when click the button
        topHashTagButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                //JOptionPane.showMessageDialog(null,"Hello");
                //DefaultTableModel tableUpdate= (DefaultTableModel) hashtagTable.getModel();
                String[] columnNames = {"Hashtag", "Count"};
                Object[][] data =
                        {
                                {"hello1", "BACK"},
                                {"big1", "EXIT"},
                                {"data1", "FORWARD"},
                        };

                DefaultTableModel tableModel = new DefaultTableModel(data, columnNames);


                hashtagTable.setModel(tableModel);


                /*String url = "http://localhost:8080/getAll";
                HttpURLConnection con = null;
                try {

                    URL myurl = new URL(url);
                    con = (HttpURLConnection) myurl.openConnection();

                    con.setRequestMethod("GET");

                    StringBuilder content;

                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

                        String line;
                        content = new StringBuilder();

                        while ((line = in.readLine()) != null) {
                            content.append(line);
                            content.append("\n");
                        }


                    System.out.println(content.toString());

                }  catch (Exception e) {
                    e.printStackTrace();
                }

                finally {

                    con.disconnect();
                }*/

            }
        });
    }

    public static void main(String[] args) {

        //Use NimbusLookAndFeel for UI
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Create frame
        JFrame frame = new JFrame("Hashtag Analyzer");
        frame.setContentPane(new ClientUI().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Set size and location of the frame
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setSize(500,500);
        frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);

        frame.setVisible(true);
    }
}
