package com.ra4king.circuitsim.simulator;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ra4king.circuitsim.simulator.CircuitState.LinkState;
import com.ra4king.circuitsim.simulator.Port.Link;

/**
 * @author Roi Atalla
 */
public class LinkStateTest {
	private static final Port PORT1 = mock(Port.class);
	private static final Port PORT2 = mock(Port.class);
	private static final HashSet<Port> PORTS12 = new HashSet<>(Set.of(PORT1, PORT2));
	
	private static final int BITSIZE = 3;
	
	private final CircuitState state = mock(CircuitState.class);
	private final Link link1 = mock(Link.class);
	
	private LinkState linkState;
	
	@Test
	public void testLinkInit() {
		initLink(link1, BITSIZE, PORTS12);
		
		assertThat(linkState.getLink()).isEqualTo(link1);
		assertThat(linkState.getParticipants().keySet()).containsExactlyElementsIn(PORTS12);
		assertThat(linkState.getLastPushed(PORT1).getBitSize()).isEqualTo(BITSIZE);
		assertThat(linkState.getLastReceived(PORT1).getBitSize()).isEqualTo(BITSIZE);
		assertThat(linkState.getMergedValue().getBitSize()).isEqualTo(BITSIZE);
	}
	
	private void initLink(Link link, int bitSize, HashSet<Port> ports) {
		when(link.getParticipants()).thenReturn(ports);
		when(link.getBitSize()).thenReturn(bitSize);
		linkState = state.new LinkState(link);
	}
}
