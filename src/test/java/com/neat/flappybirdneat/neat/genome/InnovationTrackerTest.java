package com.neat.flappybirdneat.neat.genome;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InnovationTrackerTest {

    @Test
    void nodeIdsAreSequentialAndUnique() {
        InnovationTracker tracker = new InnovationTracker();

        assertEquals(0, tracker.nextNodeId());
        assertEquals(1, tracker.nextNodeId());
        assertEquals(2, tracker.nextNodeId());
    }

    @Test
    void sameConnectionPairReturnsSameInnovationNumber() {
        InnovationTracker tracker = new InnovationTracker();

        int first = tracker.getInnovationNumber(1, 5);
        int again = tracker.getInnovationNumber(1, 5);

        assertEquals(first, again);
    }

    @Test
    void differentConnectionPairsGetDifferentInnovationNumbers() {
        InnovationTracker tracker = new InnovationTracker();

        int ab = tracker.getInnovationNumber(1, 5);
        int ba = tracker.getInnovationNumber(5, 1);
        int ac = tracker.getInnovationNumber(1, 6);

        assertNotEquals(ab, ba, "La dirección de la conexión importa: (1,5) != (5,1)");
        assertNotEquals(ab, ac);
    }

    @Test
    void innovationNumbersAreAssignedInDiscoveryOrder() {
        InnovationTracker tracker = new InnovationTracker();

        int first = tracker.getInnovationNumber(0, 3);
        int second = tracker.getInnovationNumber(1, 3);
        int firstAgain = tracker.getInnovationNumber(0, 3);

        assertEquals(0, first);
        assertEquals(1, second);
        assertEquals(0, firstAgain);
    }
}
