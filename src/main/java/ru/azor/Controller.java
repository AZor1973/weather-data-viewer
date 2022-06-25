package ru.azor;

import com.mongodb.MongoTimeoutException;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ServerConnectionState;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import lombok.extern.slf4j.Slf4j;
import ru.azor.enums.ErrorValues;
import ru.azor.services.ActiveMQArtemisService;
import ru.azor.services.MongoDBService;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

@Slf4j
public class Controller implements Initializable {
    @FXML
    public ListView<String> currentWeatherList;
    @FXML
    public ListView<String> currentWeatherItem;
    @FXML
    public ImageView imageView;
    @FXML
    public Button updateListButton;
    @FXML
    public Button updateCityNAmeButton;
    @FXML
    public Button selectCityNameButton;
    @FXML
    public Label currentCity;
    @FXML
    public Label keyLabel;


    private MongoDatabase database;
    private MongoDBService mongoDBService;
    private ActiveMQArtemisService artemisService;
    private Alert alert;
    public static final String MONGO_TIMEOUT_EXCEPTION_MESSAGE
            = "Timed out after 30000 ms while waiting to connect.";
    public static final String MONGO_TIMEOUT_EXCEPTION = "MONGO TIMEOUT EXCEPTION";
    public static final String MONGO_CONNECTION = "MONGO CONNECTION";
    public static final String MONGO_CONNECTION_MESSAGE
            = "Is in the process of connecting to MongoDB ...";
    public static final String NO_CONNECTION = "No connection. Update";
    public static final String UPDATE_LIST = "UPDATE LIST";
    public static final String WAIT_CONNECTION = "Wait connection";
    public static final String SET_CITY_NAME_TITLE = "Entering a city name";
    public static final String SET_CITY_NAME_HEADER = "Entering a city name";
    public static final String SET_CITY_NAME_CONTENT = "Enter the name of the city";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mongoDBService = MongoDBService.getInstance();
        artemisService = ActiveMQArtemisService.getInstance();
        database = mongoDBService.connectToMongoDB();
        updateCurrentCityName();
        Platform.runLater(this::updateCurrentWeatherListWithoutConnection);
        ContextMenu listContextMenu = new ContextMenu();
        currentWeatherList.setContextMenu(listContextMenu);
        MenuItem deleteMenuItem = new MenuItem("Delete");
        listContextMenu.getItems().add(deleteMenuItem);
        deleteMenuItem.setOnAction(event -> deleteSelectedWeatherData());
    }

    private void deleteSelectedWeatherData() {
        String id = currentWeatherList.getSelectionModel().getSelectedItem();
        if (id != null) {
            if (deleteItemAlert(id)) {
                mongoDBService.deleteWeatherDataById(database, id);
                updateCurrentWeatherListByButton();
            }
        }
    }

    public void selectCurrentWeatherListItem(MouseEvent mouseEvent) {
        String id = currentWeatherList.getSelectionModel().getSelectedItem();
        if (mouseEvent.getClickCount() == 2) {
            keyLabel.setText(id);
            currentWeatherItem.getItems().clear();
            currentWeatherItem.getItems().add(mongoDBService
                    .getSavedCurrentWeatherById(database, id));
            String url = mongoDBService.getImagePath(database, id);
            imageView.setImage(new Image(url));
        }
    }

    private void updateCurrentWeatherListWithoutConnection() {
        new Thread(() -> {
            Platform.runLater(() -> {
                currentWeatherList.getItems().clear();
                alert = showAlert(MONGO_CONNECTION,
                        MONGO_CONNECTION_MESSAGE, Alert.AlertType.INFORMATION);
                updateListButton.setText(WAIT_CONNECTION);
                currentWeatherItem.getItems().add(MONGO_CONNECTION_MESSAGE);
            });
            try {
                currentWeatherList.getItems()
                        .addAll(mongoDBService
                                .getAllKeysOfSavedCurrentWeatherFromMongoDB(database));
            } catch (MongoTimeoutException ex) {
                Platform.runLater(() -> {
                    showAndWaitAlert(MONGO_TIMEOUT_EXCEPTION,
                            MONGO_TIMEOUT_EXCEPTION_MESSAGE, Alert.AlertType.ERROR);
                    currentWeatherList.getItems().clear();
                });
            } finally {
                Platform.runLater(() -> {
                    currentWeatherItem.getItems().clear();
                    String updateButtonText = currentWeatherList.getItems().size()
                            == 0 ? NO_CONNECTION : UPDATE_LIST;
                    updateListButton.setText(updateButtonText);
                    alert.close();
                });
            }
        }).start();
    }

    private void showAndWaitAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private Alert showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.show();
        return alert;
    }

    private boolean deleteItemAlert(String item) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Deletion");
        alert.setHeaderText("Deletion");
        alert.setContentText("Are you sure?\n" + item + " will be deleted!");
        alert.showAndWait();
        return alert.getResult() == ButtonType.OK;
    }

    public void updateCurrentWeatherListByButton() {
        if (mongoDBService.getServerConnectionState()
                == ServerConnectionState.CONNECTED) {
            currentWeatherList.getItems().clear();
            currentWeatherList.getItems()
                    .addAll(mongoDBService
                            .getAllKeysOfSavedCurrentWeatherFromMongoDB(database));
            updateListButton.setText(UPDATE_LIST);
        } else {
            updateCurrentWeatherListWithoutConnection();
        }
    }

//    public void selectCityNameToGetTheCurrentWeather() {
//        String cityName = getNewNameFromDialog(SET_CITY_NAME_TITLE,
//        SET_CITY_NAME_HEADER, SET_CITY_NAME_CONTENT);
//        kafkaService.sendCityNameToKafka(1, cityName);
//        updateCurrentCityName();
//    }

//    public void selectCityNameToGetTheCurrentWeather() throws IOException, TimeoutException {
//        String cityName = getNewNameFromDialog(SET_CITY_NAME_TITLE,
//                SET_CITY_NAME_HEADER, SET_CITY_NAME_CONTENT);
//        rabbitMQService.sendCityNameToRabbitMQ(cityName);
//        updateCurrentCityName();
//    }

    public void selectCityNameToGetTheCurrentWeather() throws Exception {
        String cityName = getNewNameFromDialog(SET_CITY_NAME_TITLE,
                SET_CITY_NAME_HEADER, SET_CITY_NAME_CONTENT);
        artemisService.sendCityNameToArtemis(cityName);
        updateCurrentCityName();
    }

//    private void updateCurrentCityName() {
//        new Thread(() -> {
//            String cityName = kafkaService.readConfirmationOfCityNameFromKafka();
//            Platform.runLater(() -> {
//                if (!cityName.isBlank()) {
//                    currentCity.setText("City to get current weather: " + cityName);
//                }
//            });
//        }).start();
//    }

//    private void updateCurrentCityName() {
//        new Thread(() -> {
//            try {
//                rabbitMQService.readConfirmationOfCityNameFromRabbitMQ();
//                Thread.sleep(3000);
//                String cityName = rabbitMQService.getResponse();
//                if (cityName != null) {
//                    Platform.runLater(() -> {
//                        currentCity.setText("City to get current weather: " + cityName);
//                    });
//                }
//            } catch (IOException | TimeoutException | InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }).start();
//    }

    private void updateCurrentCityName() {
        new Thread(() -> {
            String cityName;
            String error;
            String errorText = "";
            try {
                cityName = artemisService.readConfirmationOfCityNameFromArtemis();
                if (cityName.equals(ErrorValues.NO_SERVICE.name())) {
                    artemisService.sendCityNameToArtemis(ErrorValues.NO_SERVICE.name());
                }
                int count = 0;
                while (cityName.equals(ErrorValues.NO_SERVICE.name())) {
                    count++;
                    if (count == 3) {
                        break;
                    }
                    cityName = artemisService.readConfirmationOfCityNameFromArtemis();
                }
                error = artemisService.readErrorFromActiveMQ();
                if (error != null && !error.isBlank()) {
                    errorText = ", Given " + error;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (!cityName.isBlank()) {
                String finalErrorText = errorText;
                String finalCityName = cityName;
                Platform.runLater(() -> {
                    currentCity.setText("City to get current weather: " +
                            finalCityName.toUpperCase() + finalErrorText);
                    if (mongoDBService.getServerConnectionState()
                            == ServerConnectionState.CONNECTED){
                        updateCurrentWeatherListByButton();
                    }
                });
            }
        }).start();
    }

    private String getNewNameFromDialog(String title, String header, String content) {
        TextInputDialog editDialog = new TextInputDialog();
        editDialog.setTitle(title);
        editDialog.setHeaderText(header);
        editDialog.setContentText(content);
        Optional<String> optName = editDialog.showAndWait();
        return optName.orElse("");
    }

    public void updateCityName() throws Exception {
        artemisService.sendCityNameToArtemis(ErrorValues.NO_SERVICE.name());
        log.info("Send update city name");
        updateCurrentCityName();
    }
}