package ru.azor.services;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoTimeoutException;
import com.mongodb.client.*;
import com.mongodb.connection.ServerConnectionState;
import org.bson.Document;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MongoDBService {
    private static final String CONNECTION_STRING = "mongodb://azor:azor@localhost:27017";
    private static final String DATABASE_NAME = "weather";
    private static final String CURRENT_WEATHER_ROOT_COLLECTION = "currentWeatherRoot";
    private MongoClient mongoClient;

    private MongoDBService() {
    }

    private static class SingletonHolder {
        public static final MongoDBService HOLDER_INSTANCE = new MongoDBService();
    }

    public static MongoDBService getInstance() {
        return SingletonHolder.HOLDER_INSTANCE;
    }

    public MongoDatabase connectToMongoDB() {
        mongoClient = MongoClients.create(CONNECTION_STRING);
        return mongoClient.getDatabase(DATABASE_NAME);
    }

    public ServerConnectionState getServerConnectionState() {
        return mongoClient.getClusterDescription().getServerDescriptions().get(0).getState();
    }

    public List<String> getAllKeysOfSavedCurrentWeatherFromMongoDB(MongoDatabase database) throws MongoTimeoutException {
        MongoCollection<Document> collection = database
                .getCollection(CURRENT_WEATHER_ROOT_COLLECTION);
        List<String> keysList = new ArrayList<>();
        try (MongoCursor<Document> cur = collection.find().iterator()) {
            while (cur.hasNext()) {
                Document doc = cur.next();
                keysList.add(doc.getString("_id"));
            }
        }
        return keysList;
    }

    public String getSavedCurrentWeatherById(MongoDatabase database, String id) {
        MongoCollection<Document> collection = database
                .getCollection(CURRENT_WEATHER_ROOT_COLLECTION);
        BasicDBObject query = new BasicDBObject("_id", id);
        Document myDoc = collection.find(query).first();
        assert myDoc != null;
        return formatCurrentWeatherString(myDoc);
    }

    public void deleteWeatherDataById(MongoDatabase database, String id) {
        MongoCollection<Document> collection = database
                .getCollection(CURRENT_WEATHER_ROOT_COLLECTION);
        BasicDBObject query = new BasicDBObject("_id", id);
        collection.deleteOne(query);
    }

    public String getImagePath(MongoDatabase database, String id) {
        MongoCollection<Document> collection = database
                .getCollection(CURRENT_WEATHER_ROOT_COLLECTION);
        BasicDBObject query = new BasicDBObject("_id", id);
        Document doc = collection.find(query).first();
        assert doc != null;
        Document docCurrent = (Document) doc.get("current");
        Document docCondition = (Document) docCurrent.get("condition");
        long isDay = docCurrent.getLong("isDay");
        String timeOfDay = isDay == 1 ? "day" : "night";
        String iconUrl = docCondition.getString("icon");
        String imagePath = "weather/" + timeOfDay + "/" + iconUrl.substring(iconUrl.lastIndexOf("/") + 1);
        if (!Files.exists(Path.of("src/main/resources/" + imagePath))) {
            imagePath = "https:" + iconUrl;
        }
        return imagePath;
    }

    private String formatCurrentWeatherString(Document doc) {
        Document docLocation = (Document) doc.get("location");
        Document docCurrent = (Document) doc.get("current");
        Document docCondition = (Document) docCurrent.get("condition");
        String cityName = docLocation.getString("name").toUpperCase();
        String countryName = docLocation.getString("country");
        double lat = docLocation.getDouble("lat");
        double lon = docLocation.getDouble("lon");
        String time = docCurrent.getString("lastUpdated");
        String cond = docCondition.getString("text");
        double tempC = docCurrent.getDouble("tempC");
        double feelsLikeC = docCurrent.getDouble("feelslikeC");
        long cloud = docCurrent.getLong("cloud");
        double windSpeed = docCurrent.getDouble("windKph");
        double windGust = docCurrent.getDouble("gustKph");
        String windDir = windDirectionFormatString(docCurrent.getString("windDir"));
        double pressure = docCurrent.getDouble("pressureMb") * 0.75f;
        double precipitation = docCurrent.getDouble("precipMm");
        long humidity = docCurrent.getLong("humidity");
        double uv = docCurrent.getDouble("uv");
        double visKm = docCurrent.getDouble("visKm");
        return String
                .format("""
                                %s
                                %s
                                Latitude: %.2f, Longitude: %.2f
                                Localtime: %s
                                %s, Temp.: %.1f Feels like: %.1f
                                Cloud: %d
                                Wind speed: %.1f km/h Gusts: %.1f km/h
                                Wind direction: %s
                                Pressure: %.0f mmHg, Precipitation: %.0f mm
                                Humidity: %d %%
                                UV: %.1f Visibility: %.1f km""",
                        cityName, countryName, lat, lon, time, cond, tempC, feelsLikeC, cloud, windSpeed,
                        windGust, windDir, pressure, precipitation, humidity, uv, visKm);
    }

    private String windDirectionFormatString(String windDir) {
        List<String> directions = new ArrayList<>();
        for (int i = 0; i < windDir.length(); i++) {
            switch (windDir.charAt(i)) {
                case 'N' -> directions.add("North");
                case 'S' -> directions.add("South");
                case 'E' -> directions.add("East");
                case 'W' -> directions.add("West");
            }
        }
        return String.join("-", directions);
    }
}
