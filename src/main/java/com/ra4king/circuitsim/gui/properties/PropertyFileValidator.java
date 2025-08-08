package com.ra4king.circuitsim.gui.properties;

import com.ra4king.circuitsim.gui.Properties;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import com.ra4king.circuitsim.gui.properties.PropertyFileValidator.FileWrapper;

public class PropertyFileValidator implements Properties.PropertyValidator<FileWrapper> {
    @Override
    public FileWrapper parse(String value) {
        return new FileWrapper(value.isEmpty() ? null : new File(value));
    }

    @Override
    public String toString(FileWrapper value) {
        return value == null || value.file == null ? "" : value.toString();
    }

    @Override
    public Node createGui(Stage stage, FileWrapper
            value, Consumer<FileWrapper> onAction) {
        Button button = new Button(value == null ? "Choose source file" : value.toString());
        button.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open Source File");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*"),
                    new FileChooser.ExtensionFilter("Data Files", "*.dat"),
                    new FileChooser.ExtensionFilter("Binary Files", "*.bin"),
                    new FileChooser.ExtensionFilter("Hex Files", "*.hex"),
                    new FileChooser.ExtensionFilter("ROM Data", "*.rom"),
                    new FileChooser.ExtensionFilter("Disk Image", "*.img")
            );
            File selectedFile = fileChooser.showOpenDialog(stage);
            onAction.accept(new FileWrapper(null));
            onAction.accept(new FileWrapper(selectedFile));
        });
        button.setTextOverrun(OverrunStyle.LEADING_ELLIPSIS);
        Button clear = new Button("Clear");
        clear.setOnAction(event -> onAction.accept(new FileWrapper(null)));
        clear.setMinWidth(Button.USE_PREF_SIZE);
        Separator separator = new Separator(Orientation.VERTICAL);
        separator.setPrefWidth(2);
        HBox.setHgrow(clear, Priority.ALWAYS);
        HBox.setMargin(separator, new Insets(0, 8, 0, 8));
        return value == null || value.file == null ? button : new HBox(button, separator, clear);
    }

    public record FileWrapper(@Nullable File file) {

        @NotNull
        @Override
        public String toString() {
            try {
                return file == null ? "Choose source file" : file.getCanonicalPath();
            } catch (IOException e) {
                return "Choose source file";
            }
        }
    }
}
