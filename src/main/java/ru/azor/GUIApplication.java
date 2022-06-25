package ru.azor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.azor.services.MongoDBService;
import ru.azor.services.RabbitMQService;

import java.io.IOException;

public class GUIApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(GUIApplication.class.getResource("view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Weather Viewer");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> System.exit(1));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}