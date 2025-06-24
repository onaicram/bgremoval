package com.deus.bgremoval;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javax.imageio.ImageIO;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Stack;

public class App extends Application {

    private Stack<BufferedImage> undoStack = new Stack<>();

    private ImageView imageView = new ImageView();
    private BufferedImage bufferedImage;
    private Rectangle pickerMarker = new Rectangle(5, 5, Color.TRANSPARENT);
    private int pickedBgColor = -1; 
    private TextField colorField = new TextField("Nessun colore selezionato");
    private Canvas checkerCanvas = new Canvas(600, 600);
    private Canvas gridCanvas = new Canvas(600, 600);  
    private TextField gridSizeField = new TextField("16");


    @Override
    public void start(Stage primaryStage) {

        // Disegna il pattern a scacchiera
        createCheckerboard();

        // Buttons
        Button loadButton = new Button("Carica");
        Button removeButton = new Button("Rimuovi Sfondo");
        Button saveButton = new Button("Salva");
        Button undoButton = new Button("Annulla");
        Button copyButton = new Button("Copia");
        Button gridButton = new Button("Attiva Griglia");

        loadButton.setOnAction(e -> loadImage(primaryStage));
        removeButton.setOnAction(e -> removeBackground());
        saveButton.setOnAction(e -> saveImage(primaryStage));
        undoButton.setOnAction(e -> undo());

        pickerMarker.setStroke(Color.RED);
        pickerMarker.setStrokeWidth(1);
        pickerMarker.setVisible(false);

        colorField.setEditable(false);
        colorField.setPrefWidth(200);

        gridCanvas.setVisible(false); 

        // Main Pane
        StackPane imagePane = new StackPane(checkerCanvas, imageView, gridCanvas, pickerMarker);
        imagePane.setAlignment(Pos.CENTER);


        // Buttons layout
        HBox buttonBox = new HBox(10); 
        buttonBox.setPadding(new Insets(10));
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getChildren().addAll(loadButton, removeButton, saveButton, gridButton, gridSizeField, undoButton);

        HBox colorBox = new HBox(5, colorField, copyButton);
        colorBox.setAlignment(Pos.CENTER);

        VBox bottomBox = new VBox(5, colorBox, buttonBox);
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.CENTER);

        VBox centerBox = new VBox(10, imagePane);
        centerBox.setPadding(new Insets(10));
        centerBox.setAlignment(Pos.CENTER);

        ScrollPane scrollPane = new ScrollPane(centerBox);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        copyButton.setOnAction(e -> {
            String colorCode = colorField.getText().trim();
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            final ClipboardContent content = new ClipboardContent();
            content.putString(colorCode);
            clipboard.setContent(content);
            System.out.println("Colore copiato: " + colorCode);
        });

        
        gridButton.setOnAction(e -> drawGrid(e.getSource(),gridCanvas, gridSizeField, imagePane));

        scrollPane.setOnScroll(event -> {
            double delta = 1.1;
            double scale = imagePane.getScaleX();
            if (event.getDeltaY() > 0) {
                scale *= delta;
            } else {
                scale /= delta;
            }

            // Limiti allo zoom
            scale = Math.max(0.1, Math.min(scale, 10));
            imagePane.setScaleX(scale);
            imagePane.setScaleY(scale);

            event.consume();
        });


        imageView.setPreserveRatio(true);
        imageView.setFitWidth(600);
        imageView.setFitHeight(600);
        imageView.setPickOnBounds(true);
        imageView.setSmooth(false);

        gridCanvas.setMouseTransparent(true); 
        gridSizeField.setPrefWidth(50);

        checkerCanvas.setWidth(imageView.getFitWidth());
        checkerCanvas.setHeight(imageView.getFitHeight());

        imageView.setOnMouseEntered(event -> pickerMarker.setVisible(true));

        imageView.setOnMouseMoved(event -> {
            double viewWidth = imageView.getBoundsInLocal().getWidth();
            double viewHeight = imageView.getBoundsInLocal().getHeight();

            pickerMarker.setTranslateX(event.getX() - viewWidth / 2);
            pickerMarker.setTranslateY(event.getY() - viewHeight / 2);
        });

        imageView.setOnMouseExited(event -> pickerMarker.setVisible(false));

        imageView.setOnMousePressed(event -> {

            if (bufferedImage == null) return;

            Bounds bounds = imageView.getLayoutBounds();

            double imgWidth = imageView.getImage().getWidth();
            double imgHeight = imageView.getImage().getHeight();
            double viewWidth = bounds.getWidth();
            double viewHeight = bounds.getHeight();

            double scaleX = imgWidth / viewWidth;
            double scaleY = imgHeight / viewHeight;

            int x = (int) (event.getX() * scaleX);
            int y = (int) (event.getY() * scaleY);

            // Clamp
            x = Math.max(0, Math.min(x, bufferedImage.getWidth() - 1));
            y = Math.max(0, Math.min(y, bufferedImage.getHeight() - 1));

            pickedBgColor = bufferedImage.getRGB(x, y);
            String hexColor = String.format("#%06X", (pickedBgColor & 0xFFFFFF));
            colorField.setText(hexColor);

            // Fissa il marker sul punto selezionato
            pickerMarker.setTranslateX(event.getX() - viewWidth / 2);
            pickerMarker.setTranslateY(event.getY() - viewHeight / 2);
        });

        BorderPane root = new BorderPane();
        root.setBottom(bottomBox);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 800, 720);
        primaryStage.setTitle("BG Removal");
        primaryStage.setScene(scene);
        primaryStage.show();

        Platform.runLater(() -> {
            scrollPane.setHvalue(scrollPane.getHmax() / 2);
            scrollPane.setVvalue(scrollPane.getVmax() / 2);
        });
    }

    private void createCheckerboard() {
        GraphicsContext gc = checkerCanvas.getGraphicsContext2D();
        double squareSize = 6;

        for (int y = 0; y < checkerCanvas.getHeight(); y += squareSize) {
            for (int x = 0; x < checkerCanvas.getWidth(); x += squareSize) {
                if ((x / squareSize + y / squareSize) % 2 == 0) {
                    gc.setFill(Color.LIGHTGRAY);
                } else {
                    gc.setFill(Color.DARKGRAY);
                }
                gc.fillRect(x, y, squareSize, squareSize);
            }
        }
    }

    private void drawGrid(Object gridButton, Canvas gridCanvas, TextField gridSizeField, StackPane imagePane) {
        int cellSize;
        try {
            cellSize = Integer.parseInt(gridSizeField.getText());
            if (cellSize <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            System.out.println("Dimensione cella non valida, uso default 16");
            cellSize = 16;
        }

        Button button = (Button) gridButton;
        System.out.println("Attivazione griglia con dimensione cella: " + cellSize);

        if (gridCanvas.isVisible()) {
            gridCanvas.setVisible(false);
            button.setText("Mostra griglia");
        } else {
            gridCanvas.setVisible(true);
            button.setText("Nascondi griglia");

            if (imageView.getImage() == null) return;

            Bounds bounds = imageView.getBoundsInParent();
            double viewWidth = bounds.getWidth();
            double viewHeight = bounds.getHeight();

            gridCanvas.setWidth(viewWidth);
            gridCanvas.setHeight(viewHeight);

            GraphicsContext gc = gridCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, viewWidth, viewHeight);

            gc.setStroke(Color.RED);
            gc.setLineWidth(0.5);

            double scaleX = viewWidth / imageView.getImage().getWidth();
            double scaleY = viewHeight / imageView.getImage().getHeight();

            for (double x = 0; x <= imageView.getImage().getWidth(); x += cellSize) {
                double posX = x * scaleX;
                gc.strokeLine(Math.round(posX) + 0.5, 0, Math.round(posX) + 0.5, viewHeight);

            }

            for (double y = 0; y <= imageView.getImage().getHeight(); y += cellSize) {
                double posY = y * scaleY;
                gc.strokeLine(0, Math.round(posY) + 0.5, viewWidth, Math.round(posY) + 0.5);
            }
        }
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

    private void undo() {
        if (!undoStack.isEmpty()) {
            bufferedImage = undoStack.pop();
            imageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
        } else {
            System.out.println("Nessuno stato da annullare");
        }
    }

    private void removeBackground() {
        if (bufferedImage == null) return;

        saveForUndo();

        int bgColor = (pickedBgColor != -1) 
            ? pickedBgColor 
            : bufferedImage.getRGB(0, 0);

        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                if (colorsClose(bufferedImage.getRGB(x, y), bgColor)) {
                    bufferedImage.setRGB(x, y, 0x00FFFFFF);
                }
            }
        }
        imageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));

    }

    private void saveForUndo() {
        BufferedImage backup = new BufferedImage(
            bufferedImage.getWidth(),
            bufferedImage.getHeight(),
            bufferedImage.getType()
        );
        Graphics2D g2d = backup.createGraphics();
        g2d.drawImage(bufferedImage, 0, 0, null);
        g2d.dispose();

        undoStack.push(backup);
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
