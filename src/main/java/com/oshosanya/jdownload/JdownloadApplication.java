package com.oshosanya.jdownload;

import com.oshosanya.jdownload.ui.JdownloadUI;
import com.sun.javafx.application.PlatformImpl;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;


@SpringBootApplication
public class JdownloadApplication implements CommandLineRunner {

	@Autowired
	private DownloadTask downloadTask;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private JdownloadUI jdownloadUI;

	public static void main(String[] args) {
		new SpringApplicationBuilder(JdownloadApplication.class)
				.headless(false)
//				.web(WebApplicationType.NONE)
				.run(args);
//		SpringApplication.run(JdownloadApplication.class, args);
	}

	@Override
	public void run(String... args) {
//		Application.launch(JdownloadUI.class, args);Application.launch(JdownloadUI.class, args);

//		Application.launch(JdownloadUI.class, args);
//		Platform.startup();

		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(2);
		Future ui = executor.submit(() -> {
			PlatformImpl.startup(new Runnable() {
				@Override
				public void run() {
					Stage stage = new Stage();
					try {
						jdownloadUI.start(stage);
					} catch (Exception e) {
						System.out.printf("Ui startup failed: %s", e.getMessage());
					}
				}
			});
		});
		try {
			Thread.sleep(5000);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		Future core = executor.submit(() -> {
			downloadTask.start(args);
		});

		try {
			ui.get();
			core.get();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}


//		System.out.println("Hello");
	}
//	@Override
//	public void start(Stage primaryStage) throws Exception {
//		Parent root = FXMLLoader.load(getClass().getResource("/fxml/root.fxml"));
//		Scene scene = new Scene(root, 300, 250);
//		primaryStage.setTitle("Hello");
//		primaryStage.setScene(scene);
//		primaryStage.show();
//	}
}
