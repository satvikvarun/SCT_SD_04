import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProductDetailExtractor extends JFrame {
    private JTextField urlField = new JTextField(30);
    private JButton scrapeButton = new JButton("Scrape");
    private JLabel statusLabel = new JLabel("Enter the API URL or file path and click Scrape.");

    public ProductDetailExtractor() {
        setTitle("Product Information Extractor");
        setSize(400, 150);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Add URL field
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(urlField, gbc);

        // Add Scrape button below URL field
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        add(scrapeButton, gbc);

        // Add status label
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(statusLabel, gbc);

        scrapeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String input = urlField.getText();
                try {
                    if (isValidURL(input)) {
                        scrapeProductDetailsFromAPI(input);
                    } else {
                        scrapeProductDetailsFromFile(input);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Error: " + ex.getMessage());
                }
            }
        });
    }

    private boolean isValidURL(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return false;
            }
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private void scrapeProductDetailsFromAPI(String apiUrl) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            statusLabel.setText("Failed to connect to API. Response code: " + response.statusCode());
            return;
        }

        String responseBody = response.body();
        JSONArray products = new JSONArray(responseBody);

        try (PrintWriter out = new PrintWriter(new FileWriter("products.csv"));
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("Name", "Price", "Rating", "Description", "Image"))) {

            for (int i = 0; i < products.length(); i++) {
                JSONObject product = products.getJSONObject(i);
                String name = product.getString("title");
                String price = String.valueOf(product.getDouble("price"));
                String rating = String.valueOf(product.getJSONObject("rating").getDouble("rate"));
                String description = product.getString("description");
                String image = product.getString("image");

                printer.printRecord(name, price, rating, description, image);
            }

            statusLabel.setText("Scraping completed. Data saved to products.csv");
        }
    }

    private void scrapeProductDetailsFromFile(String filePath) throws IOException {
        Document doc = Jsoup.parse(new File(filePath), "UTF-8");
        Elements products = doc.select(".product");

        if (products.isEmpty()) {
            statusLabel.setText("No products found. Please check the CSS selector.");
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter("products.csv"));
             CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.withHeader("Name", "Price", "Rating", "Description", "Image"))) {

            for (Element product : products) {
                String name = product.select(".product-name").text();
                String price = product.select(".product-price").text();
                String rating = product.select(".product-rating").text();
                String description = product.select(".product-description").text();
                String image = product.select("img").attr("src");

                printer.printRecord(name, price, rating, description, image);
            }

            statusLabel.setText("Scraping completed. Data saved to products.csv");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProductDetailExtractor().setVisible(true));
    }
}
