package com.ra4king.circuitsim.gui;

import java.util.List;

import com.ra4king.circuitsim.gui.Connection.PortConnection;
import com.ra4king.circuitsim.simulator.CircuitState;
import com.ra4king.circuitsim.simulator.Component;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Roi Atalla
 */
public abstract class ComponentPeer<C extends Component> extends GuiElement {
	private C component;
	private Properties properties;
	private List<PortConnection> connections;
	
	public ComponentPeer(int x, int y, int width, int height) {
		super(x, y, width, height);
	}
	
	protected final void init(C component, Properties properties, List<PortConnection> connections) {
		if (this.component != null) {
			throw new IllegalStateException("ComponentPeer already initialized.");
		}
		
		this.component = component;
		this.properties = properties;
		this.connections = connections;
	}
	
	public @NotNull C getComponent() {
		return component;
	}
	
	public Properties getProperties() {
		return properties;
	}
	
	@NotNull
	@Override
	public List<PortConnection> getConnections() {
		return connections;
	}

	@Override
	public void paint(GraphicsContext graphics, @Nullable CircuitState circuitState) {
		GuiUtils.drawName(graphics, this, getProperties().getValue(Properties.LABEL_LOCATION));
		graphics.setFill(Color.WHITE);
		graphics.setStroke(Color.BLACK);
		GuiUtils.drawShape(graphics::fillRect, this);
		GuiUtils.drawShape(graphics::strokeRect, this);
	}

	@Override
	public String toString() {
		return getComponent().toString();
	}
}
