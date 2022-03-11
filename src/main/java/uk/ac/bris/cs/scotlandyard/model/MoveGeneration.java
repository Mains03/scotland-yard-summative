package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableSet;

import java.util.Objects;

public interface MoveGeneration {
    public ImmutableSet<Move> generateMoves();

    public final class SingleMoveGeneration implements MoveGeneration {
        private final ImmutableSet<Move.SingleMove> moves;

        public SingleMoveGeneration(final Player player) {
            Objects.requireNonNull(player);
            moves = generateMoves(player);
        }

        private ImmutableSet<Move.SingleMove> generateMoves(final Player player) {
            if (player.isMrX()) {
                return generateMrXMoves(player);
            } else {
                return generateDetectiveMoves(player);
            }
        }

        private ImmutableSet<Move.SingleMove> generateMrXMoves(final Player player) {
            if (!player.isMrX()) { throw new IllegalArgumentException(); }
            return null;
        }

        private ImmutableSet<Move.SingleMove> generateDetectiveMoves(final Player player) {
            if (!player.isDetective()) { throw new IllegalArgumentException(); }
            return null;
        }

        public ImmutableSet<Move.SingleMove> getSingleMoves() {
            return moves;
        }

        @Override
        public ImmutableSet<Move> generateMoves() {
            return ImmutableSet.copyOf(moves);
        }
    }

    public class DoubleMoveGeneration implements MoveGeneration {
        private final ImmutableSet<Move.DoubleMove> moves;

        public DoubleMoveGeneration(final Player player) {
            Objects.requireNonNull(player);
            if (!player.isMrX()) { throw new IllegalArgumentException(); }
            moves = generateMoves(player, new SingleMoveGeneration(player).getSingleMoves());
        }

        private ImmutableSet<Move.DoubleMove> generateMoves(final Player player, ImmutableSet<Move.SingleMove> singleMoves) {
            return null;
        }

        @Override
        public ImmutableSet<Move> generateMoves() {
            return ImmutableSet.copyOf(moves);
        }
    }
}
