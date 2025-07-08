package com.ra4king.circuitsim.gui.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.ra4king.circuitsim.gui.Properties.PropertyValidator;

import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * @author Roi Atalla
 */
public class PropertyListValidator<T> implements PropertyValidator<T> {
	private final List<T> validValues;
	private final Function<T, String> toString;
	
	public PropertyListValidator(T[] validValues) {
		this(Arrays.asList(validValues));
	}
	
	public PropertyListValidator(List<T> validValues) {
		this(validValues, T::toString);
	}
	
	public PropertyListValidator(T[] validValues, Function<T, String> toString) {
		this(Arrays.asList(validValues), toString);
	}
	
	public PropertyListValidator(List<T> validValues, Function<T, String> toString) {
		this.validValues = Collections.unmodifiableList(validValues);
		this.toString = toString;
	}
	
	public List<T> getValidValues() {
		return validValues;
	}
	
	@Override
	public int hashCode() {
		List<String> values = validValues.stream().map(toString).collect(Collectors.toList());
		return validValues.hashCode() ^ values.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof PropertyListValidator) {
			PropertyListValidator<?> validator = (PropertyListValidator<?>)other;
			return this.validValues.equals(validator.validValues);
		}
		
		return true;
	}
	
	@Override
	public T parse(String value) {
		for (T t : validValues) {
			if (toString.apply(t).equals(value)) {
				return t;
			}
		}
		
		throw new IllegalArgumentException("Value not found: " + value);
	}
	
	@Override
	public String toString(T value) {
		return value == null ? "" : toString.apply(value);
	}
	
	@Override
	public Node createGui(Stage stage, T value, Consumer<T> onAction) {
		if (validValues.size() == 32 && value instanceof Integer) {
			HashSet<Integer> commonValues = new HashSet<>(Arrays.asList(1, 2, 4, 8, 16, 32));
			AtomicBoolean useEllipsis = new AtomicBoolean(true);
			ComboBox<String> dropdown = createDropdown(value, onAction);
			dropdown.setConverter(new StringConverter<>() {
                @Override
                public String toString(String value) {
                    if (value == null) return "";
                    int num = Integer.parseInt(value);
                    return useEllipsis.get() && commonValues.contains(num) ? "..." : value;
                }

                @Override
                public String fromString(String string) {
                    return string == null ? "1" : string;
                }
            });
			dropdown.setOnAction(event -> {
				useEllipsis.set(false);
			});

			HBox hBox = new HBox();
			for (int i = 1; i <= 32; i<<=1) {
				String selected = Integer.toString(i);
				ToggleButton button = new ToggleButton(selected);
				button.getStyleClass().add("property-list-validator-button");
				if (i == 1) button.getStyleClass().add("property-list-validator-button-first");
				else button.getStyleClass().add("property-list-validator-button-middle");
				button.setSelected(toString(value).equals(selected));
				final Integer finalI = i;
				button.setOnAction(event -> {
					useEllipsis.set(true);
					dropdown.setConverter(dropdown.getConverter());
					try {
						onAction.accept((T) finalI);
					} catch (Exception exc) {
						exc.printStackTrace();
					}
					hBox.getChildren().forEach(node -> {
						if (node instanceof ToggleButton toggleButton) {
							toggleButton.setSelected(toggleButton == button);
						}
					});
				});
				hBox.getChildren().add(button);
			}
			hBox.getChildren().add(dropdown);
			return hBox;
		}
		return validValues.size() > 8 ? createDropdown(value, onAction) : createHorizontalSelect(value, onAction);
	}

	ComboBox<String> createDropdown(T value, Consumer<T> onAction) {
		HashSet<String> commonValues = new HashSet<>(Arrays.asList("1", "2", "4", "8", "16", "32"));
		ComboBox<String> valueList = new ComboBox<>();

		for (T t : validValues) {
			if (!commonValues.contains(t))
				valueList.getItems().add(toString.apply(t));
		}

		valueList.getStyleClass().add("property-list-validator-dropdown");

		valueList.setValue(toString(value));
		valueList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.equals(oldValue)) {
				try {
					onAction.accept(parse(newValue));
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		});
		return valueList;
	}

	Node createHorizontalSelect(T value, Consumer<T> onAction) {
		HBox valueList = new HBox();
		String selected = toString(value);
		for (int i = 0; i < validValues.size(); i++) {
			T t = validValues.get(i);
			String text = toString.apply(t);
			ToggleButton button = new ToggleButton(text);
			valueList.getChildren().add(button);
			button.setSelected(text.equals(selected));
			button.setOnAction(event -> {
				try {
					onAction.accept(t);
				} catch (Exception exc) {
					exc.printStackTrace();
				}
				valueList.getChildren().forEach(node -> {
					ToggleButton toggleButton = (ToggleButton) node;
					toggleButton.setSelected(toggleButton == button);
				});
			});
			button.getStyleClass().add("property-list-validator-button");
			if (i == 0) button.getStyleClass().add("property-list-validator-button-first");
			else if (i == validValues.size() - 1) button.getStyleClass().add("property-list-validator-button-last");
			else button.getStyleClass().add("property-list-validator-button-middle");
		}
//		valueList.getStyleClass().add("property-list-validator");

		return valueList;
	}
}
