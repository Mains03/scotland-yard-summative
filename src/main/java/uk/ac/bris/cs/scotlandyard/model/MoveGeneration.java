package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;

import javax.annotation.concurrent.Immutable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TransferQueue;

public interface MoveGeneration {
    public ImmutableSet<Move> generateMoves();

    public final class SingleMoveGeneration implements MoveGeneration {
        private final ImmutableSet<Move.SingleMove> moves;

        public SingleMoveGeneration(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player
        ) {
            Objects.requireNonNull(graph);
            Objects.requireNonNull(player);
            moves = generateMoves(graph, player);
        }

        private ImmutableSet<Move.SingleMove> generateMoves(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player
        ) {
            if (player.isMrX()) {
                return generateMrXMoves(graph, player);
            } else {
                return generateDetectiveMoves(graph, player);
            }
        }

        private ImmutableSet<Move.SingleMove> generateMrXMoves(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player
        ) {
            ImmutableSet.Builder<Move.SingleMove> builder = new ImmutableSet.Builder<>();
            graph.adjacentNodes(player.location()).stream().forEach(destination -> {
                builder.addAll(generateMrXMovesToDestination(
                        graph,
                        player,
                        destination
                ));
            });

            return builder.build();
        }

        private ImmutableSet<Move.SingleMove> generateGeneralMovesToDestination(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player,
                Integer destination
        ) {
            ImmutableSet.Builder<Move.SingleMove> builder = new ImmutableSet.Builder<>();
            graph.edgeValue(player.location(), destination).ifPresent(allTransport -> allTransport.stream()
                    .forEach(transport -> {
                        generateGeneralMoveFromTransport(player, destination, transport).ifPresent(move -> {
                            builder.add(move);
                        });
                    })
            );
            return builder.build();
        }

        private Optional<Move.SingleMove> generateGeneralMoveFromTransport(
                final Player player,
                Integer destination,
                ScotlandYard.Transport transport
        ) {
            if (player.has(transport.requiredTicket())) {
                return Optional.of(new Move.SingleMove(
                        player.piece(),
                        player.location(),
                        transport.requiredTicket(),
                        destination
                ));
            } else {
                return Optional.empty();
            }
        }

        private ImmutableSet<Move.SingleMove> generateMrXMovesToDestination(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player,
                Integer destination
        ) {
            ImmutableSet.Builder<Move.SingleMove> builder = new ImmutableSet.Builder<>();
            builder.addAll(generateGeneralMovesToDestination(graph, player, destination));
            generateMrXSecretMove(player, destination).ifPresent(move -> {
                builder.add(move);
            });
            return builder.build();
        }

        private Optional<Move.SingleMove> generateMrXSecretMove(
                final Player player,
                Integer destination
        ) {
            if (player.has(ScotlandYard.Ticket.SECRET)) {
                return Optional.of(new Move.SingleMove(
                        player.piece(),
                        player.location(),
                        ScotlandYard.Ticket.SECRET,
                        destination
                ));
            } else {
                return Optional.empty();
            }
        }

        private ImmutableSet<Move.SingleMove> generateDetectiveMoves(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player
        ) {
            ImmutableSet.Builder<Move.SingleMove> builder = new ImmutableSet.Builder<>();
            graph.adjacentNodes(player.location()).stream().forEach(destination -> {
                builder.addAll(generateDetectiveMovesToDestination(graph, player, destination));
            });
            return builder.build();
        }

        private ImmutableSet<Move.SingleMove> generateDetectiveMovesToDestination(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player,
                Integer destination
        ) {
            ImmutableSet.Builder<Move.SingleMove> builder = new ImmutableSet.Builder<>();
            builder.addAll(generateGeneralMovesToDestination(graph, player, destination));
            return builder.build();
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

        public DoubleMoveGeneration(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player
        ) {
            Objects.requireNonNull(graph);
            Objects.requireNonNull(player);
            if (!player.isMrX()) { throw new IllegalArgumentException(); }
            moves = generateMoves(player, new SingleMoveGeneration(graph, player).getSingleMoves());
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
