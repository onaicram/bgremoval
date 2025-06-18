package com.deus.bgremoval;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class App extends Application {

    private ImageView imageView = new ImageView();
    private BufferedImage bufferedImage;

    @Override
    public void start(Stage primaryStage) {
        Button loadButton = new Button("Carica");
        Button removeButton = new Button("Rimuovi Sfondo");
        Button saveButton = new Button("Salva");

        loadButton.setOnAction(e -> loadImage(primaryStage));
        removeButton.setOnAction(e -> removeBackground());
        saveButton.setOnAction(e -> saveImage(primaryStage));

        VBox buttonBox = new VBox(10); // 10 = spacing verticale
        buttonBox.setPadding(new Insets(10));
        buttonBox.getChildren().addAll(loadButton, removeButton, saveButton);
        
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);
        imageView.setFitHeight(600);

        BorderPane root = new BorderPane();
        root.setCenter(imageView);
        root.setLeft(buttonBox);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("BG Remover");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadImage(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                bufferedImage = ImageIO.read(file);
                imageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void removeBackground() {
        if (bufferedImage == null) return;

        int bgColor = bufferedImage.getRGB(0, 0);

        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                if (colorsClose(bufferedImage.getRGB(x, y), bgColor)) {
                    bufferedImage.setRGB(x, y, 0x00FFFFFF); // trasparente
                }
            }
        }
        imageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
    }

    private boolean colorsClose(int rgb1, int rgb2) {
        int tolerance = 30; // modificabile
        int r1 = (rgb1 >> 16) & 0xFF, g1 = (rgb1 >> 8) & 0xFF, b1 = rgb1 & 0xFF;
        int r2 = (rgb2 >> 16) & 0xFF, g2 = (rgb2 >> 8) & 0xFF, b2 = rgb2 & 0xFF;
        return Math.abs(r1 - r2) < tolerance &&
               Math.abs(g1 - g2) < tolerance &&
               Math.abs(b1 - b2) < tolerance;
    }

    private void saveImage(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialFileName("output.png");
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                ImageIO.write(bufferedImage, "png", file);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
