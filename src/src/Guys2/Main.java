package Guys2;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    public static void main(String[] args) {
        Application.launch(Main.class, args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        int WIDTH  = inizialisierung.WIDTH;
        int HEIGHT = inizialisierung.HEIGHT;

        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        StackPane root = new StackPane(canvas);

        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.setFill(javafx.scene.paint.Color.BLACK);

        multiplayer game = new multiplayer(canvas);

        scene.addEventFilter(KeyEvent.KEY_PRESSED,  game::onKeyPressed);
        scene.addEventFilter(KeyEvent.KEY_RELEASED, game::onKeyReleased);

        stage.setTitle("TWO GUYS — Plattform Kampf");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        canvas.requestFocus();
        game.start();
    }
}