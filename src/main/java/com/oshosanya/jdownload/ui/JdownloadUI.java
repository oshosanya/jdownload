package com.oshosanya.jdownload.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class JdownloadUI {

    @Autowired
    private ApplicationContext context;

    public void start(Stage primaryStage) throws Exception {
//        Parent root = FXMLLoader.load(getClass().getResource("/fxml/root.fxml"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/root.fxml"));
        loader.setController(context.getBean("root"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        primaryStage.setTitle("Hello");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);

        primaryStage.show();
    }

}
