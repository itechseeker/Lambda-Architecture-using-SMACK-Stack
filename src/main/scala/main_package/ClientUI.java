package main_package;

import com.google.gson.Gson;

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

// Define Hashtag Class
class Hashtag {
    String value;
    int count;
}


public class ClientUI {
    private JPanel mainPanel;
    private JPanel rightPanel;
    private JPanel leftPanel;
    private JButton topHashTagButton;
    private JTable hashtagTable;

    public ClientUI() {

        // Name of table's column
        final String[] columnNames = {"Hashtag", "Count"};

        // Define table model
        final DefaultTableModel tableModel = new DefaultTableModel(new Object[20][], columnNames);
        hashtagTable.setModel(tableModel);
        hashtagTable.setRowHeight(30);

        // Set font for elements
        hashtagTable.setFont(new Font("Times New Roman", Font.PLAIN, 20));
        topHashTagButton.setFont(new Font("Times New Roman", Font.PLAIN, 20));
        hashtagTable.getTableHeader().setFont(new Font("Times New Roman", Font.PLAIN, 20));

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
                //DefaultTableModel tableModel = new DefaultTableModel(new Object[10][2], columnNames);
                // Get the hashtag array
                Hashtag[] hashtags=getData("http://localhost:8080/getAll");
                tableModel.setRowCount(0);
                for(int i=0;i<hashtags.length;i++)
                {
                    tableModel.addRow(new Object[]{hashtags[i].value,hashtags[i].count});
                }
                tableModel.fireTableDataChanged();

            }
        });
    }

    public Hashtag[] getData(String urlString)
    {
        HttpURLConnection connection = null;
        Hashtag[] hashtags = new Hashtag[0];
        try {

            // Create connection
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            // Set GET request
            connection.setRequestMethod("GET");

            // Read the response data
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            String line;
            StringBuilder content= new StringBuilder();

            while ((line = in.readLine()) != null) {
                content.append(line);
                content.append("\n");
            }

            //Convert json String to Hashtag object
            Gson gson=new Gson();
            hashtags=gson.fromJson(content.toString(),Hashtag[].class);

        }  catch (Exception e) {
            e.printStackTrace();
        }finally {
            connection.disconnect();
        }
        return hashtags;

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
        frame.setSize(700,700);
        frame.setLocation(dim.width/2-frame.getSize().width/2, dim.height/2-frame.getSize().height/2);

        frame.setVisible(true);
    }
}
