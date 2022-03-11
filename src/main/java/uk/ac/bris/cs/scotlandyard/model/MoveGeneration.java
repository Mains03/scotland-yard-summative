package uk.ac.bris.cs.scotlandyard.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.graph.ImmutableValueGraph;

import java.util.Objects;
import java.util.Optional;

public abstract class MoveGeneration {
    public abstract ImmutableSet<Move> generateMoves();

    public static final class SingleMoveGeneration extends MoveGeneration {
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

    public static final class DoubleMoveGeneration extends MoveGeneration {
        private final ImmutableSet<Move.DoubleMove> moves;

        public DoubleMoveGeneration(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player
        ) {
            Objects.requireNonNull(graph);
            Objects.requireNonNull(player);
            if (!player.isMrX()) { throw new IllegalArgumentException(); }
            moves = generateMoves(graph, player, new SingleMoveGeneration(graph, player).getSingleMoves());
        }

        private ImmutableSet<Move.DoubleMove> generateMoves(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player,
                final ImmutableSet<Move.SingleMove> singleMoves
        ) {
            ImmutableSet.Builder<Move.DoubleMove> builder = new ImmutableSet.Builder<>();
            singleMoves.stream().forEach(move -> {
                builder.addAll(generateDoubleMoves(graph, player, move));
            });
            return builder.build();
        }

        private ImmutableSet<Move.DoubleMove> generateDoubleMoves(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player,
                final Move.SingleMove singleMove
        ) {
            ImmutableSet.Builder<Move.DoubleMove> builder = new ImmutableSet.Builder<>();
            graph.adjacentNodes(singleMove.destination).stream().forEach(secondDestination -> {
                builder.addAll(generateDoubleMovesToSecondDestination(graph, player, singleMove, secondDestination));
            });
            return builder.build();
        }

        private ImmutableSet<Move.DoubleMove> generateDoubleMovesToSecondDestination(
                final ImmutableValueGraph<Integer, ImmutableSet<ScotlandYard.Transport>> graph,
                final Player player,
                final Move.SingleMove singleMove,
                Integer secondDestination
        ) {
            ImmutableSet.Builder<Move.DoubleMove> builder = new ImmutableSet.Builder<>();
            graph.edgeValue(singleMove.destination, secondDestination).ifPresent(allTransport -> {
                allTransport.stream().forEach(transport -> {
                    generateDoubleMoveFromTransport(player, singleMove, secondDestination, transport).ifPresent(move -> {
                        builder.add(move);
                    });
                });
            });
            generateDoubleMoveUsingSecret(player, singleMove, secondDestination).ifPresent(move -> {
                builder.add(move);
            });
            return builder.build();
        }

        private Move.DoubleMove createDoubleMove(
                final Player player,
                final Move.SingleMove singleMove,
                Integer secondDestination,
                ScotlandYard.Ticket ticket
        ) {
            return new Move.DoubleMove(
                    player.piece(),
                    player.location(),
                    singleMove.ticket,
                    singleMove.destination,
                    ticket,
                    secondDestination
            );
        }

        private Optional<Move.DoubleMove> generateDoubleMoveFromTransport(
                final Player player,
                final Move.SingleMove singleMove,
                Integer secondDestination,
                final ScotlandYard.Transport transport
        ) {
            boolean playerCanUseTransport = false;
            if (singleMove.ticket == transport.requiredTicket()) {
                playerCanUseTransport = player.hasAtLeast(transport.requiredTicket(), 2);
            } else {
                playerCanUseTransport = player.has(transport.requiredTicket());
            }
            if (playerCanUseTransport) {
                return Optional.of(createDoubleMove(
                        player,
                        singleMove,
                        secondDestination,
                        transport.requiredTicket()
                ));
            } else {
                return Optional.empty();
            }
        }

        private Optional<Move.DoubleMove> generateDoubleMoveUsingSecret(
                final Player player,
                final Move.SingleMove singleMove,
                Integer secondDestination
        ) {
            boolean playerCanUseTransport = false;
            if (singleMove.ticket == ScotlandYard.Ticket.SECRET) {
                playerCanUseTransport = player.hasAtLeast(ScotlandYard.Ticket.SECRET, 2);
            } else {
                playerCanUseTransport = player.has(ScotlandYard.Ticket.SECRET);
            }
            if (playerCanUseTransport) {
                return Optional.of(createDoubleMove(
                        player,
                        singleMove,
                        secondDestination,
                        ScotlandYard.Ticket.SECRET
                ));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public ImmutableSet<Move> generateMoves() {
            return ImmutableSet.copyOf(moves);
        }
    }
}
